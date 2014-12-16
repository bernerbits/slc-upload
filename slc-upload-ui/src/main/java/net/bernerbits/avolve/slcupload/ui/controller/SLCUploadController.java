package net.bernerbits.avolve.slcupload.ui.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
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
import net.bernerbits.avolve.slcupload.model.ExistingFileOptions;
import net.bernerbits.avolve.slcupload.model.RemoteFolder;
import net.bernerbits.avolve.slcupload.ui.ExistingFileDialog;
import net.bernerbits.avolve.slcupload.ui.S3Dialog;
import net.bernerbits.avolve.slcupload.ui.handler.BucketHandler;
import net.bernerbits.avolve.slcupload.ui.handler.ConnectionCheckHandler;
import net.bernerbits.avolve.slcupload.ui.handler.FileSelectedHandler;
import net.bernerbits.avolve.slcupload.ui.handler.FileTransferUpdateHandler;
import net.bernerbits.avolve.slcupload.ui.handler.ProgresssHandler;
import net.bernerbits.avolve.slcupload.ui.handler.StartConversionHandler;
import net.bernerbits.avolve.slcupload.ui.handler.ValidationHandler;
import net.bernerbits.avolve.slcupload.ui.model.FileSystemFileTransferOperation;
import net.bernerbits.avolve.slcupload.ui.model.FileTransferOperation;
import net.bernerbits.avolve.slcupload.ui.model.FileTransferOperationBuilder;
import net.bernerbits.avolve.slcupload.ui.model.S3FileTransferOperation;
import net.bernerbits.avolve.slcupload.util.function.NullSafe;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import com.google.common.base.Strings;

public class SLCUploadController {

	private final Shell shell;

	private final SpreadsheetImporter importer;

	private final S3Connector s3Connector;

	private final FileTransferer transferer;

	private final FileTransferOperationBuilder transferBuilder;

	private @Nullable FileTransferOperation fileTransfer;

	public SLCUploadController(Shell shell) {
		this.shell = shell;
		this.importer = new SpreadsheetImporter();
		this.s3Connector = new S3Connector();
		this.transferer = new FileTransferer();
		this.transferBuilder = new FileTransferOperationBuilder();
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
		Iterable<SpreadsheetRow> rows = importer.importSpreadsheet(inputFile);
		transferBuilder.setRows(rows);
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
			transferBuilder.setFolderDestination(fileSelected);
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
			transferBuilder.setFolderSource(fileSelected);
			handler.fileSelected(fileSelected);
			Preferences.userNodeForPackage(getClass()).put("lastSourceFolder", fileSelected);
		}
	}

	public void s3BucketSearchRequested(FileSelectedHandler handler) {
		S3Dialog s3Dialog = new S3Dialog(this, shell, SWT.OPEN);
		RemoteFolder locSelected = s3Dialog.open();
		if (locSelected != null) {
			transferBuilder.setS3Destination(locSelected);
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
		fileTransfer = null;
		if (transferBuilder.getRows() == null) {
			handler.validationFailed("Please select an input file.");
			return false;
		} else if (transferBuilder.getConvertedRows() == null && !convertRows(errorHandler, conversionHandler)) {
			handler.validationFailed("The input file is not recognized. The following columns must be present: projectid, sourcepath, filename.");
			return false;
		} else if (transferBuilder.getFolderSource() == null) {
			handler.validationFailed("Please select a source folder.");
			return false;
		} else if (transferBuilder.getS3Destination() == null && transferBuilder.getFolderDestination() == null) {
			handler.validationFailed("Please select a destination.");
			return false;
		} else if (Strings.nullToEmpty(transferBuilder.getFolderDestination()).equalsIgnoreCase(
				transferBuilder.getFolderSource())) {
			handler.validationFailed("Source and destination cannot be the same.");
			return false;
		}
		fileTransfer = transferBuilder.build();
		return true;
	}

	private boolean convertRows(ErrorHandler errorHandler, StartConversionHandler handler) {
		Iterable<SpreadsheetRow> rows = transferBuilder.getRows();
		if (rows != null) {
			try {
				transferBuilder.setConvertedRows(importer.convertRows(rows, errorHandler));
				return true;
			} catch (SpreadsheetImportException e) {
				return false;
			}
		} else {
			throw new IllegalStateException("Could not convert null rows");
		}
	}

	public void beginTransfer(ProgresssHandler progress, FileTransferUpdateHandler updateHandler) {
		if (fileTransfer != null) {
			synchronized(this) {
				existingFileOptions = null;
			}
			FileTransferOperation fileTransfer = this.fileTransfer;

			new Thread(() -> {
				fileTransfer.resetCount();
				if (fileTransfer instanceof FileSystemFileTransferOperation) {
					transferer.performLocalTransfer(
							fileTransfer.getTransferObjects(),
							fileTransfer.getFolderSource(),
							((FileSystemFileTransferOperation) fileTransfer).getFolderDestination(),
							(update) -> shell.getDisplay().syncExec(
									() -> {
										float ratio = ((float) fileTransfer.updateCount())
												/ fileTransfer.getTransferObjects().size();
										updateHandler.notifyFileTransfer(update);
										progress.updateProgress(ratio, fileTransfer.isComplete());
									}),
							SLCUploadController.this::getExistingFileOptions);
				} else if (fileTransfer instanceof S3FileTransferOperation) {
					transferer.performRemoteTransfer(
							fileTransfer.getTransferObjects(),
							fileTransfer.getFolderSource(),
							((S3FileTransferOperation) fileTransfer).getS3Destination(),
							(update) -> shell.getDisplay().syncExec(
									() -> {
										float ratio = ((float) fileTransfer.updateCount())
												/ fileTransfer.getTransferObjects().size();
										updateHandler.notifyFileTransfer(update);
										progress.updateProgress(ratio, fileTransfer.isComplete());
									}),
									SLCUploadController.this::getExistingFileOptions);
				}
			}).start();
		} else {
			throw new IllegalStateException("Cannot begin transfer: convertedRows or folderSource is null");
		}
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

	private ExistingFileOptions existingFileOptions = null;
	
	public synchronized ExistingFileOptions getExistingFileOptions(Path existingFile) {
		ExistingFileOptions existingFileOptions = this.existingFileOptions;
		if (existingFileOptions == null)
		{
			AtomicReference<ExistingFileOptions> val = new AtomicReference<>();
			shell.getDisplay().syncExec(() -> val.set(new ExistingFileDialog(shell, SWT.OPEN).open(existingFile)));
			existingFileOptions = val.get();
			if (existingFileOptions.isRemember())
			{
				this.existingFileOptions = existingFileOptions;
			}
		}
		return existingFileOptions;
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
			Path pathSelected = NullSafe.getPath(fileSelected);
			if (!Files.exists(pathSelected) || confirmOverwrite(pathSelected)) {
				handler.fileSelected(fileSelected);
			}
		}
	}

	public boolean confirmOverwrite(Path pathSelected) {
		return MessageDialog.openQuestion(shell, "Overwrite file?", pathSelected + " already exists. Overwrite it?");
	}

	public int getTransferCount() {
		if (fileTransfer != null) {
			return fileTransfer.currentCount();
		} else {
			throw new IllegalStateException("File transfer is not yet valid");
		}
	}

	public int getTotalCount() {
		if (fileTransfer != null) {
			return fileTransfer.getTransferObjects().size();
		} else {
			throw new IllegalStateException("File transfer is not yet valid");
		}
	}

	@SafeVarargs
	public final static <T> void writeCsv(String csvFile, List<@NonNull T> results,
			ColumnDefinition<T>... columnDefinitions) throws FileExportException {
		new CSVExporter<T>().export(NullSafe.getPath(csvFile), results, columnDefinitions);
	}

}
