package net.bernerbits.avolve.slcupload.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import net.bernerbits.avolve.slcupload.ErrorFileTransfer;
import net.bernerbits.avolve.slcupload.FileTransfer;
import net.bernerbits.avolve.slcupload.dataexport.ColumnDefinition;
import net.bernerbits.avolve.slcupload.dataimport.exception.FileExtensionNotRecognizedException;
import net.bernerbits.avolve.slcupload.dataimport.exception.SpreadsheetFileNotFoundException;
import net.bernerbits.avolve.slcupload.dataimport.model.SpreadsheetRow;
import net.bernerbits.avolve.slcupload.ui.controller.SLCUploadController;
import net.bernerbits.avolve.slcupload.ui.presenter.FileTransferPresenter;
import net.bernerbits.avolve.slcupload.ui.util.ClosureColumnLabelProvider;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.StatusLineManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.SWTResourceManager;

import com.google.common.base.Stopwatch;

public class SLCUploadShell extends Shell {
	private static final boolean S3_ENABLED = true;

	private final SLCUploadController slcUploadController;

	private int currentTasks = 0;

	private Text inputFileField;
	private TableViewer sheetPreviewTable;
	private Text outputLocationField;
	private TableViewer transferResultsTable;
	private Menu resultsMenu;

	private Group fileTransferGroup;

	private Label lblValidationErrors;
	private Label lblTransferResults;

	private ProgressBar fileTransferProgress;

	private Button btnStartTransfer;
	private Button checkAutoScrollResults;
	private Button checkErrorResultsOnly;

	private Text inputLocationField;

	private Stopwatch transferStopwatch;

	private StatusLineManager status;

	public SLCUploadShell(Display display) {
		super(display, SWT.SHELL_TRIM);
		slcUploadController = new SLCUploadController(this);

		setLayout(new FormLayout());

		Group grpInputSource = new Group(this, SWT.NONE);
		FormData fd_grpInputSource = new FormData();
		fd_grpInputSource.top = new FormAttachment(0, 10);
		fd_grpInputSource.left = new FormAttachment(0, 10);
		fd_grpInputSource.right = new FormAttachment(100, -10);
		fd_grpInputSource.bottom = new FormAttachment(0, 196);
		grpInputSource.setLayoutData(fd_grpInputSource);
		grpInputSource.setLayout(new FormLayout());
		grpInputSource.setText("Input Source");

		Label fileInputLabel = new Label(grpInputSource, SWT.NONE);
		FormData fd_fileInputLabel = new FormData();
		fd_fileInputLabel.top = new FormAttachment(0, 6);
		fd_fileInputLabel.left = new FormAttachment(0, 10);
		fileInputLabel.setLayoutData(fd_fileInputLabel);
		fileInputLabel.setText("From File:");

		inputFileField = new Text(grpInputSource, SWT.BORDER | SWT.READ_ONLY);
		FormData fd_inputFileField = new FormData();
		fd_inputFileField.left = new FormAttachment(fileInputLabel, 6);
		fd_inputFileField.top = new FormAttachment(0, 3);
		inputFileField.setLayoutData(fd_inputFileField);

		Button inputFileSearchButton = new Button(grpInputSource, SWT.NONE);
		inputFileSearchButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				slcUploadController.spreadsheetFileSearchRequested(SLCUploadShell.this::inputFileSelected);
			}
		});
		fd_inputFileField.right = new FormAttachment(inputFileSearchButton, -6);
		FormData fd_inputFileSearchButton = new FormData();
		fd_inputFileSearchButton.left = new FormAttachment(100, -109);
		fd_inputFileSearchButton.top = new FormAttachment(0, 1);
		fd_inputFileSearchButton.right = new FormAttachment(100, -10);
		inputFileSearchButton.setLayoutData(fd_inputFileSearchButton);
		inputFileSearchButton.setText("Search...");

		Label previewLabel = new Label(grpInputSource, SWT.NONE);
		previewLabel.setText("Preview: ");
		FormData fd_previewLabel = new FormData();
		fd_previewLabel.left = new FormAttachment(fileInputLabel, 0, SWT.LEFT);
		fd_previewLabel.top = new FormAttachment(inputFileSearchButton, 6, SWT.BOTTOM);
		fd_previewLabel.right = new FormAttachment(100, -1);
		previewLabel.setLayoutData(fd_previewLabel);

		sheetPreviewTable = new TableViewer(grpInputSource, SWT.BORDER | SWT.V_SCROLL);
		Table table = sheetPreviewTable.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		FormData fd_sheetPreviewTable = new FormData();
		fd_sheetPreviewTable.bottom = new FormAttachment(0, 161);
		fd_sheetPreviewTable.top = new FormAttachment(previewLabel, 6);
		fd_sheetPreviewTable.left = new FormAttachment(fileInputLabel, 0, SWT.LEFT);
		fd_sheetPreviewTable.right = new FormAttachment(100, -10);
		table.setLayoutData(fd_sheetPreviewTable);

		Group grpTransferSource = new Group(this, SWT.NONE);
		grpTransferSource.setText("File Source");
		grpTransferSource.setLayout(new FormLayout());
		FormData fd_grpTransferSource = new FormData();
		fd_grpTransferSource.bottom = new FormAttachment(grpInputSource, 54, SWT.BOTTOM);
		fd_grpTransferSource.top = new FormAttachment(grpInputSource, 6);
		fd_grpTransferSource.right = new FormAttachment(grpInputSource, 0, SWT.RIGHT);
		fd_grpTransferSource.left = new FormAttachment(0, 10);
		grpTransferSource.setLayoutData(fd_grpTransferSource);

		Button btnSrcFolder = new Button(grpTransferSource, SWT.NONE);
		btnSrcFolder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				slcUploadController.sourceFolderSearchRequested(SLCUploadShell.this::inputFolderSelected);
			}
		});

		FormData fd_btnSrcFolder = new FormData();
		fd_btnSrcFolder.top = new FormAttachment(0, 4);
		fd_btnSrcFolder.right = new FormAttachment(100, -4);
		btnSrcFolder.setLayoutData(fd_btnSrcFolder);
		btnSrcFolder.setText("Folder...");

		Label lblSrcLocation = new Label(grpTransferSource, SWT.NONE);
		FormData fd_lblSrcLocation = new FormData();
		fd_lblSrcLocation.left = new FormAttachment(0, 7);
		lblSrcLocation.setLayoutData(fd_lblSrcLocation);
		lblSrcLocation.setText("Location:");

		inputLocationField = new Text(grpTransferSource, SWT.BORDER);
		FormData fd_text_2 = new FormData();
		fd_text_2.right = new FormAttachment(btnSrcFolder, -6);
		fd_text_2.top = new FormAttachment(0, 5);
		fd_text_2.left = new FormAttachment(lblSrcLocation, 6);
		inputLocationField.setLayoutData(fd_text_2);
		inputLocationField.setEditable(false);

		fd_lblSrcLocation.bottom = new FormAttachment(inputLocationField, 0, SWT.BOTTOM);
		fd_lblSrcLocation.top = new FormAttachment(inputLocationField, 0, SWT.TOP);

		Group grpTransferDestination = new Group(this, SWT.NONE);
		grpTransferDestination.setText("Transfer Destination");
		grpTransferDestination.setLayout(new FormLayout());
		FormData fd_grpTransferDestination = new FormData();
		fd_grpTransferDestination.bottom = new FormAttachment(grpTransferSource, 54, SWT.BOTTOM);
		fd_grpTransferDestination.top = new FormAttachment(grpTransferSource, 6);
		fd_grpTransferDestination.right = new FormAttachment(grpInputSource, 0, SWT.RIGHT);
		fd_grpTransferDestination.left = new FormAttachment(0, 10);
		grpTransferDestination.setLayoutData(fd_grpTransferDestination);

		Button btnFolder = new Button(grpTransferDestination, SWT.NONE);
		btnFolder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				slcUploadController.destinationFolderSearchRequested(SLCUploadShell.this::outputFolderSelected);
			}
		});
		FormData fd_btnFolder = new FormData();
		btnFolder.setLayoutData(fd_btnFolder);
		btnFolder.setText("Folder...");

		Button btnSBucket = new Button(grpTransferDestination, SWT.NONE);
		btnSBucket.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				slcUploadController.s3BucketSearchRequested(SLCUploadShell.this::outputBucketSelected);
			}
		});
		fd_btnFolder.top = new FormAttachment(btnSBucket, 0, SWT.TOP);
		fd_btnFolder.right = new FormAttachment(btnSBucket, -6);
		FormData fd_btnSBucket = new FormData();
		fd_btnSBucket.top = new FormAttachment(0, 3);
		fd_btnSBucket.right = new FormAttachment(100, -5);
		fd_btnSBucket.left = new FormAttachment(100, -82);
		btnSBucket.setLayoutData(fd_btnSBucket);
		btnSBucket.setText("S3 Bucket...");

		if (!S3_ENABLED) {
			fd_btnSBucket.left = new FormAttachment(100, 0);
			btnSBucket.setVisible(false);
		}
		Label lblLocation = new Label(grpTransferDestination, SWT.NONE);
		FormData fd_lblLocation = new FormData();
		fd_lblLocation.left = new FormAttachment(0, 7);
		lblLocation.setLayoutData(fd_lblLocation);
		lblLocation.setText("Location:");

		outputLocationField = new Text(grpTransferDestination, SWT.BORDER);
		FormData fd_text_1 = new FormData();
		fd_text_1.right = new FormAttachment(btnFolder, -6);
		fd_text_1.top = new FormAttachment(0, 5);
		fd_text_1.left = new FormAttachment(lblLocation, 6);
		outputLocationField.setLayoutData(fd_text_1);
		outputLocationField.setEditable(false);

		fd_lblLocation.bottom = new FormAttachment(outputLocationField, 0, SWT.BOTTOM);
		fd_lblLocation.top = new FormAttachment(outputLocationField, 0, SWT.TOP);

		Label lblLocation_1 = new Label(grpTransferDestination, SWT.NONE);
		FormData fd_lblLocation_1 = new FormData();
		fd_lblLocation_1.top = new FormAttachment(btnFolder, 5, SWT.TOP);
		fd_lblLocation_1.left = new FormAttachment(lblLocation, 0, SWT.LEFT);
		lblLocation_1.setLayoutData(fd_lblLocation_1);
		lblLocation_1.setText("Location:");

		fileTransferGroup = new Group(this, SWT.NONE);
		fileTransferGroup.setEnabled(false);
		fileTransferGroup.setText("File Transfer");
		fileTransferGroup.setLayout(new FormLayout());

		lblValidationErrors = new Label(this, SWT.NONE);
		lblValidationErrors.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
		FormData fd_lblValidationErrors = new FormData();
		fd_lblValidationErrors.top = new FormAttachment(grpTransferDestination, 6);
		fd_lblValidationErrors.left = new FormAttachment(0, 10);
		fd_lblValidationErrors.right = new FormAttachment(100, -10);
		lblValidationErrors.setLayoutData(fd_lblValidationErrors);
		lblValidationErrors.setText("");

		status = new StatusLineManager();
		Control statusBar = status.createControl(this);
		FormData fd_statusBar = new FormData();
		fd_statusBar.left = new FormAttachment(0, 0);
		fd_statusBar.right = new FormAttachment(100, 0);
		fd_statusBar.bottom = new FormAttachment(100, 0);
		statusBar.setLayoutData(fd_statusBar);

		FormData fd_fileTransferGroup = new FormData();
		fd_fileTransferGroup.top = new FormAttachment(lblValidationErrors, 5);
		fd_fileTransferGroup.right = new FormAttachment(grpInputSource, 0, SWT.RIGHT);
		fd_fileTransferGroup.left = new FormAttachment(0, 10);
		fd_fileTransferGroup.bottom = new FormAttachment(statusBar, -5);
		fileTransferGroup.setLayoutData(fd_fileTransferGroup);

		checkAutoScrollResults = new Button(fileTransferGroup, SWT.CHECK);
		checkAutoScrollResults.setText("Auto-scroll results");
		checkAutoScrollResults.setSelection(true);
		FormData fd_checkAutoScrollResults = new FormData();
		fd_checkAutoScrollResults.top = new FormAttachment(0, 6);
		fd_checkAutoScrollResults.right = new FormAttachment(100, -6);
		checkAutoScrollResults.setLayoutData(fd_checkAutoScrollResults);
		checkAutoScrollResults.pack();

		checkErrorResultsOnly = new Button(fileTransferGroup, SWT.CHECK);
		checkErrorResultsOnly.setText("Show only error results");
		checkErrorResultsOnly.setSelection(false);
		checkErrorResultsOnly.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				toggleErrorResults(checkErrorResultsOnly.getSelection());
			}
		});
		FormData fd_checkErrorResultsOnly = new FormData();
		fd_checkErrorResultsOnly.top = new FormAttachment(0, 6);
		fd_checkErrorResultsOnly.right = new FormAttachment(checkAutoScrollResults, -6);
		checkErrorResultsOnly.setLayoutData(fd_checkErrorResultsOnly);
		checkErrorResultsOnly.pack();

		lblTransferResults = new Label(fileTransferGroup, SWT.NONE);
		FormData fd_lblTransferResults = new FormData();
		fd_lblTransferResults.top = new FormAttachment(0, 6);
		fd_lblTransferResults.left = new FormAttachment(0, 5);
		fd_lblTransferResults.right = new FormAttachment(checkErrorResultsOnly, -5);
		lblTransferResults.setLayoutData(fd_lblTransferResults);

		btnStartTransfer = new Button(fileTransferGroup, SWT.NONE);
		btnStartTransfer.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				btnStartTransfer.setEnabled(false);
				duplicateCount = 0;
				transferResultsTable.getTable().removeAll();
				transferResultsTable.getTable().setMenu(null);
				transferResults.clear();
				transfersByPath.clear();
				errorTransferResults.clear();
				lblTransferResults.setText("");
				for (FileTransfer transfer : conversionResults) {
					fileTransferUpdate(transfer);
				}
				transferStopwatch = Stopwatch.createStarted();
				status.setMessage("Starting transfer ...");
				slcUploadController.beginTransfer(SLCUploadShell.this::updateProgress,
						SLCUploadShell.this::fileTransferUpdate);
			}
		});
		FormData fd_btnStartTransfer = new FormData();
		fd_btnStartTransfer.bottom = new FormAttachment(100, -10);
		fd_btnStartTransfer.right = new FormAttachment(100, -10);
		btnStartTransfer.setLayoutData(fd_btnStartTransfer);
		btnStartTransfer.setText("Start Transfer");

		fileTransferProgress = new ProgressBar(fileTransferGroup, SWT.SMOOTH);
		FormData fd_progressBar = new FormData();
		fd_progressBar.top = new FormAttachment(btnStartTransfer, 2, SWT.TOP);
		fd_progressBar.right = new FormAttachment(btnStartTransfer, -15);
		fd_progressBar.left = new FormAttachment(lblTransferResults, 0, SWT.LEFT);
		fileTransferProgress.setLayoutData(fd_progressBar);

		Table table2 = new Table(fileTransferGroup, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		transferResultsTable = new TableViewer(table2);
		FormData fd_table_1 = new FormData();
		fd_table_1.top = new FormAttachment(checkAutoScrollResults, 6);
		fd_table_1.left = new FormAttachment(0, 6);
		fd_table_1.right = new FormAttachment(100, -6);
		fd_table_1.bottom = new FormAttachment(btnStartTransfer, -6);

		table2.setLayoutData(fd_table_1);
		table2.setHeaderVisible(true);
		table2.setLinesVisible(true);

		// TODO only enable when transfer is finished/paused/stopped
		resultsMenu = new Menu(table2);
		MenuItem exportItem = new MenuItem(resultsMenu, SWT.NONE);
		exportItem.setText("Export Results...");
		exportItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				slcUploadController.saveToCSV(SLCUploadShell.this::saveResultsToCsv);
			}
		});
		
		TableViewerColumn statusColumn = new TableViewerColumn(transferResultsTable, SWT.NONE);
		statusColumn.getColumn().setText("Status");
		statusColumn.getColumn().setResizable(true);
		statusColumn.getColumn().setMoveable(false);
		statusColumn.getColumn().pack();
		statusColumn.setLabelProvider(new ClosureColumnLabelProvider<>(FileTransferPresenter::status,
				FileTransferPresenter::foregroundHint));

		TableViewerColumn duplicateColumn = new TableViewerColumn(transferResultsTable, SWT.NONE);
		duplicateColumn.getColumn().setText("Dup");
		duplicateColumn.getColumn().setResizable(true);
		duplicateColumn.getColumn().setMoveable(false);
		duplicateColumn.getColumn().pack();
		duplicateColumn.setLabelProvider(new ClosureColumnLabelProvider<>(FileTransferPresenter::duplicates));

		TableViewerColumn localFileColumn = new TableViewerColumn(transferResultsTable, SWT.NONE);
		localFileColumn.getColumn().setText("Local path");
		localFileColumn.getColumn().setResizable(true);
		localFileColumn.getColumn().setMoveable(false);
		localFileColumn.getColumn().pack();
		localFileColumn.setLabelProvider(new ClosureColumnLabelProvider<>(FileTransferPresenter::localPath));

		TableViewerColumn remotePathColumn = new TableViewerColumn(transferResultsTable, SWT.NONE);
		remotePathColumn.getColumn().setText("Remote path");
		remotePathColumn.getColumn().setResizable(true);
		remotePathColumn.getColumn().setMoveable(false);
		remotePathColumn.getColumn().pack();
		remotePathColumn.setLabelProvider(new ClosureColumnLabelProvider<>(FileTransferPresenter::remotePath));

		checkValidForTransfer();

		createContents();
	}

	/**
	 * Create contents of the shell.
	 */
	protected void createContents() {
		setText("PlansAnywhere File Transport Manager");
		setSize(1080, 700);

	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

	private void inputFileSelected(String inputFile) {
		inputFileField.setText(inputFile);
		clearSpreadsheetPreview();
		busy("Loading \"" + inputFile + "\"...", () -> slcUploadController.openSpreadsheet(inputFile),
				this::previewRows);
	}

	private <T> void busy(String waitMessage, Callable<T> task, Consumer<T> handler) {
		FutureTask<T> result = new FutureTask<T>(task);
		startTask(waitMessage, result);
		T value;
		try {
			value = result.get();
			getDisplay().asyncExec(() -> handler.accept(value));
		} catch (ExecutionException e) {
			if (e.getCause() instanceof OutOfMemoryError) {
				getDisplay().asyncExec(
						() -> ErrorDialog.openError(this, waitMessage, "Could not complete the operation requested.",
								new Status(IStatus.ERROR, "SLC Uploader", "Insufficient memory", e.getCause())));
			} else if (e.getCause() instanceof FileExtensionNotRecognizedException) {
				FileExtensionNotRecognizedException ex = (FileExtensionNotRecognizedException) e.getCause();
				String reason;
				if (ex.getExtension().isEmpty()) {
					reason = "File has no extension.";
				} else {
					reason = "Extension " + ex.getExtension() + " is not supported.";
				}
				getDisplay().asyncExec(
						() -> ErrorDialog.openError(this, waitMessage, "Could not complete the operation requested.",
								new Status(IStatus.ERROR, "SLC Uploader", reason, ex)));
			} else if (e.getCause() instanceof SpreadsheetFileNotFoundException) {
				SpreadsheetFileNotFoundException ex = (SpreadsheetFileNotFoundException) e.getCause();
				String reason = "File \"" + ex.getFileName() + "\" does not exist.";
				getDisplay().asyncExec(
						() -> ErrorDialog.openError(this, waitMessage, "Could not complete the operation requested.",
								new Status(IStatus.ERROR, "SLC Uploader", reason, ex)));
			} else {
				getDisplay().asyncExec(
						() -> ErrorDialog.openError(this, waitMessage, "Could not complete the operation requested.",
								new Status(IStatus.ERROR, "SLC Uploader", e.getMessage(), e.getCause())));
			}
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			endTask();
		}
	}

	private void startTask(String waitMessage, Runnable task) {
		currentTasks++;
		setEnabled(false);
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(this);
		dialog.setOpenOnRun(true);
		dialog.setCancelable(false);
		try {
			dialog.run(true, false, (monitor) -> {
				monitor.beginTask(waitMessage, IProgressMonitor.UNKNOWN);
				task.run();
				monitor.done();
			});
		} catch (OutOfMemoryError e) {
			getDisplay().asyncExec(
					() -> ErrorDialog.openError(this, waitMessage, "Could not complete the operation requested.",
							new Status(IStatus.ERROR, "SLC Uploader", "Insufficient memory", e)));
			System.gc();
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void endTask() {
		if (--currentTasks == 0) {
			setEnabled(true);
		}
	}

	private void clearSpreadsheetPreview() {
		sheetPreviewTable.getTable().clearAll();
		sheetPreviewTable.getTable().setItemCount(0);
		for (TableColumn column : sheetPreviewTable.getTable().getColumns()) {
			column.dispose();
		}
	}

	private void previewRows(Iterable<SpreadsheetRow> rows) {
		sheetPreviewTable.setContentProvider(ArrayContentProvider.getInstance());
		sheetPreviewTable.getTable().setEnabled(false);
		Iterator<SpreadsheetRow> it = rows.iterator();
		for (int i = 0; i < 11 && it.hasNext(); i++) {
			SpreadsheetRow row = it.next();
			if (i == 0) {
				int col = 0;
				for (String cellValue : row.getValues()) {
					final TableViewerColumn column = new TableViewerColumn(sheetPreviewTable, SWT.NONE);
					if (cellValue != null) {
						column.getColumn().setText(cellValue);
					}
					column.getColumn().setResizable(true);
					column.getColumn().setMoveable(false);
					column.getColumn().pack();
					final int colInd = col;
					column.setLabelProvider(new ColumnLabelProvider() {
						@Override
						public String getText(Object element) {
							String text = ((SpreadsheetRow) element).getValues()[colInd];
							return text;
						}
					});
					col++;
				}
			} else {
				sheetPreviewTable.add(row);
			}
		}
		for (TableColumn column : sheetPreviewTable.getTable().getColumns()) {
			column.pack();
		}
		sheetPreviewTable.getTable().setEnabled(true);
		checkValidForTransfer();
	}

	private void saveResultsToCsv(String csvFile) {
		List<FileTransferPresenter> results = checkErrorResultsOnly.getSelection() ? errorTransferResults
				: transferResults;
		
		busy("Writing \"" + csvFile + "\"...", () -> slcUploadController.writeCsv(csvFile, results, 
				new ColumnDefinition<>("Status", FileTransferPresenter::status),
				new ColumnDefinition<>("Dup", FileTransferPresenter::duplicates),
				new ColumnDefinition<>("Local Path", FileTransferPresenter::localPath),
				new ColumnDefinition<>("Remote Path", FileTransferPresenter::remotePath)),
				(o) -> { /* noop */ });
	}
	
	private void inputFolderSelected(String folderPath) {
		inputLocationField.setText("Folder: " + folderPath);
		checkValidForTransfer();
	}

	private void outputFolderSelected(String folderPath) {
		outputLocationField.setText("Folder: " + folderPath);
		checkValidForTransfer();
	}

	private void outputBucketSelected(String folderPath) {
		outputLocationField.setText("S3: " + folderPath);
		checkValidForTransfer();
	}

	private void checkValidForTransfer() {
		boolean valid = slcUploadController.isValidForTransfer(this::setValidationMessage, this::handleConversionError,
				this::conversionStarted);
		lblValidationErrors.setVisible(!valid);
		recursiveSetEnabled(fileTransferGroup, valid);
	}

	private void recursiveSetEnabled(Control ctrl, boolean enabled) {
		if (ctrl instanceof Composite) {
			Composite comp = (Composite) ctrl;
			for (Control c : comp.getChildren())
				recursiveSetEnabled(c, enabled);
		}
		ctrl.setEnabled(enabled);
	}

	private void conversionStarted() {
		conversionResults.clear();
	}

	private void handleConversionError(String message, String detail) {
		ErrorFileTransfer eft = new ErrorFileTransfer(message, detail);
		conversionResults.add(eft);
	}

	private void setValidationMessage(String message) {
		lblValidationErrors.setText(message);
		lblValidationErrors.setVisible(true);
	}

	private void updateProgress(double progress, boolean complete) {
		btnStartTransfer.setEnabled(complete);
		transferResultsTable.getTable().setMenu(complete ? resultsMenu : null);
		fileTransferProgress.setEnabled(!complete);
		fileTransferProgress.setSelection((int) Math.round(progress * fileTransferProgress.getMaximum()));

		if (complete) {
			transferStopwatch.stop();
			fileTransferProgress.setSelection(0);
			lblTransferResults.setText("Transfer complete! "
					+ (transferResults.size() - errorTransferResults.size()) + " File(s) Copied, "
					+ duplicateCount + " Duplicates, " + errorTransferResults.size() + " Errors.");
			status.setMessage("Transfer complete");
		} else {
			updateStatusLine(progress);
		}
	}

	private void updateStatusLine(double progress) {
		String timeRemainingMsg;
		if (progress > 0) {
			double elapsedNanos = transferStopwatch.elapsed(TimeUnit.NANOSECONDS);
			double totalEstNanos = elapsedNanos / progress;
			double estNanosRemaining = totalEstNanos - elapsedNanos;

			int secondsRemaining = (int) TimeUnit.SECONDS.convert((long) estNanosRemaining, TimeUnit.NANOSECONDS);

			int minutesRemaining = secondsRemaining / 60;
			secondsRemaining %= 60;

			int hoursRemaining = minutesRemaining / 60;
			minutesRemaining %= 60;

			int daysRemaining = hoursRemaining / 24;
			hoursRemaining %= 24;

			int yearsRemaining = daysRemaining / 365;
			daysRemaining %= 365;

			if (yearsRemaining > 0) {
				timeRemainingMsg = "Over " + yearsRemaining + " years" + (yearsRemaining > 1 ? "s" : "");
			} else if (daysRemaining > 0) {
				timeRemainingMsg = "Over " + daysRemaining + " day" + (daysRemaining > 1 ? "s" : "");
			} else if (hoursRemaining > 0) {
				timeRemainingMsg = String.format("About %d hour" + (hoursRemaining > 1 ? "s" : "") + " %d minute"
						+ (minutesRemaining > 1 ? "s" : ""), hoursRemaining, minutesRemaining);
			} else if (minutesRemaining > 0) {
				timeRemainingMsg = String.format("About %d minute" + (minutesRemaining > 1 ? "s" : ""),
						minutesRemaining);
			} else if (secondsRemaining > 0) {
				timeRemainingMsg = "About " + secondsRemaining + " second" + (secondsRemaining > 1 ? "s" : "");
			} else {
				timeRemainingMsg = "Almost done";
			}
		} else {
			timeRemainingMsg = "Calculating...";
		}
		status.setMessage("Time Remaining: " + timeRemainingMsg + " (Transferring file "
				+ slcUploadController.getTransferCount() + " of " + slcUploadController.getTotalCount() + ")");
	}

	private long lastPackTime = 0;

	private List<FileTransferPresenter> transferResults = new ArrayList<>();
	private List<FileTransferPresenter> errorTransferResults = new ArrayList<>();

	private int duplicateCount = 0;
	private Map<String, FileTransferPresenter> transfersByPath = new HashMap<>();

	private List<FileTransfer> conversionResults = new ArrayList<>();

	private void toggleErrorResults(boolean errorResultsOnly) {
		transferResultsTable.getTable().removeAll();
		if (errorResultsOnly) {
			transferResultsTable.add(errorTransferResults.toArray(new FileTransferPresenter[0]));
		} else {
			transferResultsTable.add(transferResults.toArray(new FileTransferPresenter[0]));
		}
		packTransferResultColumns();
	}

	private void packTransferResultColumns() {
		for (TableColumn column : transferResultsTable.getTable().getColumns()) {
			column.pack();
		}
	}

	private void fileTransferUpdate(FileTransfer transfer) {
		if (transfer.isDuplicate()) {
			FileTransferPresenter originalTransfer = transfersByPath.get(transfer.getPathAsString());
			originalTransfer.addDuplicate();
			transferResultsTable.refresh(originalTransfer);
			duplicateCount++;
		} else {
			FileTransferPresenter presenter = new FileTransferPresenter(transfer);
			transferResults.add(presenter);
			boolean errorResult = !(transfer.getStatus().equals("\u2713"));
			if (errorResult || !checkErrorResultsOnly.getSelection()) {
				if (errorResult) {
					errorTransferResults.add(presenter);
				}
				transferResultsTable.add(presenter);
				if (checkAutoScrollResults.getSelection()) {
					transferResultsTable.getTable().setSelection(transferResultsTable.getTable().getItems().length - 1);
					transferResultsTable.getTable().showSelection();
				}

				long currentTime = System.currentTimeMillis();
				if (currentTime - lastPackTime > 5000) {
					packTransferResultColumns();
					lastPackTime = currentTime;
				}
			}

			transfersByPath.put(transfer.getPathAsString(), presenter);
		}
	}
}
