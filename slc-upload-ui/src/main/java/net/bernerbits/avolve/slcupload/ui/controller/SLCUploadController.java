package net.bernerbits.avolve.slcupload.ui.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;

import net.bernerbits.avolve.slcupload.FileTransferer;
import net.bernerbits.avolve.slcupload.FileTransferer.TransferState;
import net.bernerbits.avolve.slcupload.S3Connection;
import net.bernerbits.avolve.slcupload.S3Connector;
import net.bernerbits.avolve.slcupload.callback.FileTransferStateChangeCallback;
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
import net.bernerbits.avolve.slcupload.ui.handler.UserInputHandler;
import net.bernerbits.avolve.slcupload.ui.handler.ValidationHandler;
import net.bernerbits.avolve.slcupload.ui.model.FileSystemFileTransferOperation;
import net.bernerbits.avolve.slcupload.ui.model.FileTransferOperation;
import net.bernerbits.avolve.slcupload.ui.model.FileTransferOperationBuilder;
import net.bernerbits.avolve.slcupload.ui.model.S3FileTransferOperation;
import net.bernerbits.avolve.slcupload.util.GlobalConfigs;
import net.bernerbits.avolve.slcupload.util.function.NullSafe;

import org.apache.log4j.Logger;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import com.google.common.base.Strings;

public class SLCUploadController {

	private static Logger logger = Logger.getLogger(SLCUploadController.class);

	private final Shell shell;

	private final SpreadsheetImporter importer;

	private final S3Connector s3Connector;

	private final FileTransferer transferer;

	private final FileTransferOperationBuilder transferBuilder;

	private @Nullable FileTransferOperation fileTransfer;
	private @Nullable UserInputHandler userInputHandler;

	public SLCUploadController(Shell shell) {
		this.shell = shell;
		this.importer = new SpreadsheetImporter();
		this.s3Connector = new S3Connector();
		this.transferer = new FileTransferer();
		this.transferBuilder = new FileTransferOperationBuilder();
	}

	public void spreadsheetFileSearchRequested(FileSelectedHandler handler) {
		logger.debug("Opening spreadsheet file dialog");

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
		logger.debug("Spreadsheet file selected: " + fileSelected);
		if (fileSelected != null) {
			handler.fileSelected(fileSelected);
		}
	}

	public Iterable<SpreadsheetRow> openSpreadsheet(String inputFile) throws SpreadsheetImportException {
		logger.debug("Importing spreadsheet: " + inputFile);
		Iterable<SpreadsheetRow> rows = importer.importSpreadsheet(inputFile);
		transferBuilder.setRows(rows);
		return rows;
	}

	public void destinationFolderSearchRequested(FileSelectedHandler handler) {
		logger.debug("Opening destination folder dialog");
		DirectoryDialog fileDialog = new DirectoryDialog(shell, SWT.OPEN);
		String filterPath = "/";
		String platform = SWT.getPlatform();
		if (platform.equals("win32") || platform.equals("wpf")) {
			filterPath = "c:\\";
		}
		filterPath = Preferences.userNodeForPackage(getClass()).get("lastDestFolder", filterPath);
		logger.debug("Destination search path = " + filterPath);
		fileDialog.setFilterPath(filterPath);
		String fileSelected = fileDialog.open();
		logger.debug("Destination folder selected: " + fileSelected);
		if (fileSelected != null) {
			transferBuilder.setFolderDestination(fileSelected);
			handler.fileSelected(fileSelected);
			Preferences.userNodeForPackage(getClass()).put("lastDestFolder", fileSelected);
		}
	}

	public void sourceFolderSearchRequested(FileSelectedHandler handler) {
		logger.debug("Opening source folder dialog");
		DirectoryDialog fileDialog = new DirectoryDialog(shell, SWT.OPEN);
		String filterPath = "/";
		String platform = SWT.getPlatform();
		if (platform.equals("win32") || platform.equals("wpf")) {
			filterPath = "c:\\";
		}
		filterPath = Preferences.userNodeForPackage(getClass()).get("lastSourceFolder", filterPath);
		logger.debug("Source search path = " + filterPath);
		fileDialog.setFilterPath(filterPath);
		String fileSelected = fileDialog.open();
		logger.debug("Source folder selected: " + fileSelected);
		if (fileSelected != null) {
			transferBuilder.setFolderSource(fileSelected);
			handler.fileSelected(fileSelected);
			Preferences.userNodeForPackage(getClass()).put("lastSourceFolder", fileSelected);
		}
	}

	public void s3BucketSearchRequested(FileSelectedHandler handler) {
		logger.debug("Opening S3 bucket dialog");
		S3Dialog s3Dialog = new S3Dialog(this, shell, SWT.OPEN);
		RemoteFolder locSelected = s3Dialog.open();
		logger.debug("S3 bucket selected: " + locSelected);
		if (locSelected != null) {
			transferBuilder.setS3Destination(locSelected);
			handler.fileSelected(locSelected.getPath());
		}
	}

	public void listBuckets(String awsKey, String awsSecret, ConnectionCheckHandler connHandler, BucketHandler handler) {
		logger.debug("Starting S3 connection check");
		S3Connection connection = s3Connector.connect(awsKey, awsSecret);
		if (connection != null) {
			logger.debug("Connection check success. Getting remote buckets.");
			handler.bucketsLoaded(connection.listRemoteFolders());
			connHandler.connectionCheckCompleted(true);
		} else {
			logger.debug("Connection check failed.");
			connHandler.connectionCheckCompleted(false);
		}
	}

	public boolean isValidForTransfer(ValidationHandler handler, ErrorHandler errorHandler,
			StartConversionHandler conversionHandler) {
		logger.debug("Validating transfer operation");
		fileTransfer = null;
		if (transferBuilder.getRows() == null) {
			logger.debug("Validation failed - input file not set");
			handler.validationFailed("Please select an input file.");
			return false;
		} else if (transferBuilder.getConvertedRows() == null && !convertRows(errorHandler, conversionHandler)) {
			logger.debug("Validation failed - input file headings invalid");
			handler.validationFailed("The input file is not recognized. The following columns must be present: projectid, sourcepath, filename.");
			return false;
		} else if (transferBuilder.getConvertedRows().isEmpty()) {
			logger.debug("Validation failed - input file is empty or invalid");
			handler.validationFailed("The input file is empty or contains no valid file records.");
			return false;
		} else if (transferBuilder.getFolderSource() == null) {
			logger.debug("Validation failed - source folder not set");
			handler.validationFailed("Please select a source folder.");
			return false;
		} else if (transferBuilder.getS3Destination() == null && transferBuilder.getFolderDestination() == null) {
			logger.debug("Validation failed - destination not set");
			handler.validationFailed("Please select a destination.");
			return false;
		} else if (Strings.nullToEmpty(transferBuilder.getFolderDestination()).equalsIgnoreCase(
				transferBuilder.getFolderSource())) {
			logger.debug("Validation failed - source and destination same");
			handler.validationFailed("Source and destination cannot be the same.");
			return false;
		}
		logger.debug("Validation succeeded - building file transfer operation");
		fileTransfer = transferBuilder.build();
		return true;
	}

	private boolean convertRows(ErrorHandler errorHandler, StartConversionHandler handler) {
		logger.debug("Converting rows to file transfers");
		Iterable<SpreadsheetRow> rows = transferBuilder.getRows();
		if (rows != null) {
			try {
				transferBuilder.setConvertedRows(importer.convertRows(rows, errorHandler));
				return true;
			} catch (SpreadsheetImportException e) {
				return false;
			}
		} else {
			logger.warn("Attempted to convert rows when not set");
			throw new IllegalStateException("Could not convert null rows");
		}
	}

	public void beginTransfer(ProgresssHandler progress, FileTransferUpdateHandler updateHandler,
			UserInputHandler userInputHandler, FileTransferStateChangeCallback stateChangeListener) {
		if (fileTransfer != null) {
			logger.debug("Starting file transfer");
			synchronized (this) {
				existingFileOptions = null;
			}
			FileTransferOperation fileTransfer = this.fileTransfer;
			this.userInputHandler = userInputHandler;

			GlobalConfigs.threadFactory.newThread(
					() -> {
						fileTransfer.resetCount();
						if (fileTransfer instanceof FileSystemFileTransferOperation) {
							logger.debug("Starting local file transfer");
							transferer.performLocalTransfer(
									fileTransfer.getTransferObjects(),
									fileTransfer.getFolderSource(),
									((FileSystemFileTransferOperation) fileTransfer).getFolderDestination(),
									(update) -> shell.getDisplay().syncExec(
											() -> {
												float ratio = (fileTransfer.updateCount())
														/ fileTransfer.getTransferObjects().size();
												updateHandler.notifyFileTransfer(update);
												progress.updateProgress(ratio, fileTransfer.isComplete());
											}), SLCUploadController.this::getExistingFileOptions, stateChangeListener);
						} else if (fileTransfer instanceof S3FileTransferOperation) {
							logger.debug("Starting remote file transfer");
							transferer.performRemoteTransfer(
									fileTransfer.getTransferObjects(),
									fileTransfer.getFolderSource(),
									((S3FileTransferOperation) fileTransfer).getS3Destination(),
									(update) -> shell.getDisplay().syncExec(
											() -> {
												float ratio = (fileTransfer.updateCount())
														/ fileTransfer.getTransferObjects().size();
												updateHandler.notifyFileTransfer(update);
												progress.updateProgress(ratio, fileTransfer.isComplete());
											}), SLCUploadController.this::getExistingFileOptions, stateChangeListener);
						}
					}).start();
		} else {
			logger.warn("File transfer build did not complete - no file transfer started");
			throw new IllegalStateException("Cannot begin transfer: convertedRows or folderSource is null");
		}
	}

	public void resumeTransfer() {
		logger.debug("Resuming transfer");
		transferer.resumeTransfer();
	}

	public void pauseTransfer() {
		logger.debug("Pausing transfer");
		transferer.pauseTransfer();
	}

	public void stopTransfer() {
		logger.debug("Stopping transfer");
		transferer.stopTransfer();
	}

	private ExistingFileOptions existingFileOptions = null;

	public synchronized ExistingFileOptions getExistingFileOptions(Path existingFile) {
		logger.debug("Showing existing file options dialog");
		ExistingFileOptions existingFileOptions = this.existingFileOptions;
		if (existingFileOptions == null) {
			AtomicReference<ExistingFileOptions> val = new AtomicReference<>();
			AtomicReference<ExistingFileDialog> dialog = new AtomicReference<>();
			try {
				userInputHandler.sync(() -> {
					dialog.set(new ExistingFileDialog(shell, SWT.OPEN));
					val.set(dialog.get().open(existingFile));
				});
			} catch (Throwable e) {
				logger.warn("Error waiting for existing file options dialog result");
				if (dialog.get() != null) {
					dialog.get().closeInUIThread();
				}
				throw e;
			}
			existingFileOptions = val.get();
			logger.debug("Option chosen: Skip=" + existingFileOptions.isSkip() + " Remember="
					+ existingFileOptions.isRemember());
			if (existingFileOptions.isRemember()) {
				logger.debug("Remembering choice for later.");
				this.existingFileOptions = existingFileOptions;
			}
		}
		return existingFileOptions;
	}

	public final void saveToCSV(FileSelectedHandler handler) {
		logger.debug("Save to CSV dialog requested");
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
		logger.debug("CSV file selected: " + fileSelected);

		if (fileSelected != null) {
			Path pathSelected = NullSafe.getPath(fileSelected);
			if (!Files.exists(pathSelected) || confirmOverwrite(pathSelected)) {
				handler.fileSelected(fileSelected);
			}
		}
	}

	public boolean confirmOverwrite(Path pathSelected) {
		logger.debug(pathSelected + " exists. Confirming overwrite.");
		boolean result = MessageDialog.openQuestion(shell, "Overwrite file?", pathSelected
				+ " already exists. Overwrite it?");
		logger.debug("User " + (result ? "confirmed" : "declined") + " file overwrite.");
		return result;
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
		logger.debug("Writing results to file: " + csvFile);
		new CSVExporter<T>().export(NullSafe.getPath(csvFile), results, columnDefinitions);
	}

	public boolean transferStateIsOneOf(TransferState... states) {
		return transferer.stateIsOneOf(states);
	}

}
