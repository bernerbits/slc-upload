package net.bernerbits.avolve.slcupload.ui.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;

import net.bernerbits.avolve.slcupload.FileTransferer;
import net.bernerbits.avolve.slcupload.S3Connection;
import net.bernerbits.avolve.slcupload.S3Connector;
import net.bernerbits.avolve.slcupload.dataexport.CSVExporter;
import net.bernerbits.avolve.slcupload.dataexport.ColumnDefinition;
import net.bernerbits.avolve.slcupload.dataexport.FileExportException;
import net.bernerbits.avolve.slcupload.dataimport.SpreadsheetImporter;
import net.bernerbits.avolve.slcupload.dataimport.exception.SpreadsheetImportException;
import net.bernerbits.avolve.slcupload.dataimport.handler.ErrorHandler;
import net.bernerbits.avolve.slcupload.dataimport.model.SpreadsheetRow;
import net.bernerbits.avolve.slcupload.model.FileTransferObject;
import net.bernerbits.avolve.slcupload.model.RemoteFolder;
import net.bernerbits.avolve.slcupload.ui.S3Dialog;
import net.bernerbits.avolve.slcupload.ui.handler.BucketHandler;
import net.bernerbits.avolve.slcupload.ui.handler.ConnectionCheckHandler;
import net.bernerbits.avolve.slcupload.ui.handler.FileSelectedHandler;
import net.bernerbits.avolve.slcupload.ui.handler.FileTransferUpdateHandler;
import net.bernerbits.avolve.slcupload.ui.handler.ProgresssHandler;
import net.bernerbits.avolve.slcupload.ui.handler.StartConversionHandler;
import net.bernerbits.avolve.slcupload.ui.handler.ValidationHandler;
import net.bernerbits.avolve.slcupload.ui.presenter.FileTransferPresenter;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

public class SLCUploadController {

	private final Shell shell;

	private final SpreadsheetImporter importer;

	private final S3Connector s3Connector;

	private final FileTransferer transferer;

	private String folderSource;

	private RemoteFolder s3Destination;
	private String folderDestination;

	private Iterable<SpreadsheetRow> rows;

	private List<FileTransferObject> convertedRows;

	public SLCUploadController(Shell shell) {
		this.shell = shell;
		this.importer = new SpreadsheetImporter();
		this.s3Connector = new S3Connector();
		this.transferer = new FileTransferer();
	}

	public void spreadsheetFileSearchRequested(FileSelectedHandler handler) {
		FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
		fileDialog.setFilterNames(new String[] { "Spreadsheets" });
		fileDialog.setFilterExtensions(new String[] { "*.xlsx;*.xsl;*.csv" });
		String filterPath = "/";
		String platform = SWT.getPlatform();
		if (platform.equals("win32") || platform.equals("wpf")) {
			filterPath = "c:\\";
		}
		fileDialog.setFilterPath(filterPath);
		String fileSelected = fileDialog.open();
		if (fileSelected != null) {
			handler.fileSelected(fileSelected);
		}
	}

	public Iterable<SpreadsheetRow> openSpreadsheet(String inputFile) throws SpreadsheetImportException {
		rows = importer.importSpreadsheet(inputFile);
		return rows;
	}

	public void destinationFolderSearchRequested(FileSelectedHandler handler) {
		DirectoryDialog fileDialog = new DirectoryDialog(shell, SWT.OPEN);
		String filterPath = "/";
		String platform = SWT.getPlatform();
		if (platform.equals("win32") || platform.equals("wpf")) {
			filterPath = "c:\\";
		}
		filterPath = Preferences.userNodeForPackage(getClass()).get("lastDestFolder", filterPath);
		fileDialog.setFilterPath(filterPath);
		String fileSelected = fileDialog.open();
		if (fileSelected != null) {
			s3Destination = null;
			folderDestination = fileSelected;
			handler.fileSelected(fileSelected);
			Preferences.userNodeForPackage(getClass()).put("lastDestFolder", fileSelected);
		}
	}

	public void sourceFolderSearchRequested(FileSelectedHandler handler) {
		DirectoryDialog fileDialog = new DirectoryDialog(shell, SWT.OPEN);
		String filterPath = "/";
		String platform = SWT.getPlatform();
		if (platform.equals("win32") || platform.equals("wpf")) {
			filterPath = "c:\\";
		}
		filterPath = Preferences.userNodeForPackage(getClass()).get("lastSourceFolder", filterPath);
		fileDialog.setFilterPath(filterPath);
		String fileSelected = fileDialog.open();
		if (fileSelected != null) {
			folderSource = fileSelected;
			handler.fileSelected(fileSelected);
			Preferences.userNodeForPackage(getClass()).put("lastSourceFolder", fileSelected);
		}
	}

	public void s3BucketSearchRequested(FileSelectedHandler handler) {
		S3Dialog s3Dialog = new S3Dialog(this, shell, SWT.OPEN);
		RemoteFolder locSelected = s3Dialog.open();
		if (locSelected != null) {
			s3Destination = locSelected;
			folderDestination = null;
			handler.fileSelected(locSelected.getPath());
		}
	}

	public void listBuckets(String awsKey, String awsSecret, ConnectionCheckHandler connHandler, BucketHandler handler) {
		S3Connection connection = s3Connector.connect(awsKey, awsSecret);
		if (connection != null) {
			handler.bucketsLoaded(connection.listRemoteFolders());
		}
		connHandler.connectionCheckCompleted(connection != null);
	}

	public boolean isValidForTransfer(ValidationHandler handler, ErrorHandler errorHandler,
			StartConversionHandler conversionHandler) {
		if (rows == null) {
			handler.validationFailed("Please select an input file.");
			return false;
		} else if (convertedRows == null && !convertRows(errorHandler, conversionHandler)) {
			handler.validationFailed("The input file is not recognized. The following columns must be present: projectid, sourcepath, filename.");
			return false;
		} else if (folderSource == null) {
			handler.validationFailed("Please select a source folder.");
			return false;
		} else if (s3Destination == null && folderDestination == null) {
			handler.validationFailed("Please select a destination.");
			return false;
		} else if (folderDestination != null && folderSource.equalsIgnoreCase(folderDestination)) {
			handler.validationFailed("Source and destination cannot be the same.");
			return false;
		}
		return true;
	}

	private boolean convertRows(ErrorHandler errorHandler, StartConversionHandler handler) {
		try {
			convertedRows = importer.convertRows(rows, errorHandler);
			return true;
		} catch (SpreadsheetImportException e) {
			return false;
		}
	}

	private AtomicInteger count = new AtomicInteger(0);

	public void beginTransfer(ProgresssHandler progress, FileTransferUpdateHandler updateHandler) {
		new Thread(() -> {
			count.set(0);
			if (folderDestination != null) {
				transferer.performLocalTransfer(convertedRows, folderSource, folderDestination, (update) -> shell
						.getDisplay().syncExec(() -> {
							float ratio = ((float) count.incrementAndGet()) / convertedRows.size();
							updateHandler.notifyFileTransfer(update);
							progress.updateProgress(ratio, count.get() == convertedRows.size());
						}));
			} else {
				transferer.performRemoteTransfer(convertedRows, folderSource, s3Destination, (update) -> shell
						.getDisplay().syncExec(() -> {
							float ratio = ((float) count.incrementAndGet()) / convertedRows.size();
							updateHandler.notifyFileTransfer(update);
							progress.updateProgress(ratio, count.get() == convertedRows.size());
						}));
			}
		}).start();
	}

	public void resumeTransfer() {
		transferer.resumeTransfer();
	}

	public void pauseTransfer() {
		transferer.pauseTransfer();		
	}
	
	public void stopTransfer() {
		transferer.stopTransfer();		
	}
	
	public final <T> void saveToCSV(FileSelectedHandler handler) {
		FileDialog fileDialog = new FileDialog(shell, SWT.SAVE);
		fileDialog.setFilterNames(new String[] { "Comma-Delimited Values (*.csv)" });
		fileDialog.setFilterExtensions(new String[] { "*.csv" });
		String filterPath = "/";
		String platform = SWT.getPlatform();
		if (platform.equals("win32") || platform.equals("wpf")) {
			filterPath = "c:\\";
		}
		fileDialog.setFilterPath(filterPath);
		String fileSelected = fileDialog.open();

		if (fileSelected != null) {
			Path pathSelected = Paths.get(fileSelected);
			if (!Files.exists(pathSelected) || confirmOverwrite(pathSelected)) {
				handler.fileSelected(fileSelected);
			}
		}
	}

	public boolean confirmOverwrite(Path pathSelected) {
		return MessageDialog.openQuestion(shell, "Overwrite file?", pathSelected + " already exists. Overwrite it?");
	}

	public int getTransferCount() {
		return count.get();
	}

	public int getTotalCount() {
		return convertedRows.size();
	}

	@SafeVarargs
	public final <T> Object writeCsv(String csvFile, List<T> results, ColumnDefinition<T>... columnDefinitions) throws FileExportException {
		new CSVExporter<T>().export(Paths.get(csvFile), results, columnDefinitions);
		return null;
	}

}
