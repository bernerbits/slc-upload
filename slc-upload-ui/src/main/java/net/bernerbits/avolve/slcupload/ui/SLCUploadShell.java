package net.bernerbits.avolve.slcupload.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.bernerbits.avolve.slcupload.FileTransfer;
import net.bernerbits.avolve.slcupload.dataimport.exception.FileExtensionNotRecognizedException;
import net.bernerbits.avolve.slcupload.dataimport.exception.SpreadsheetFileNotFoundException;
import net.bernerbits.avolve.slcupload.dataimport.model.SpreadsheetRow;
import net.bernerbits.avolve.slcupload.ui.controller.SLCUploadController;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.wb.swt.SWTResourceManager;

public class SLCUploadShell extends Shell {
	private final SLCUploadController slcUploadController;

	private int currentTasks = 0;

	private Text inputFileField;
	private TableViewer sheetPreviewTable;
	private Text outputLocationField;
	private TableViewer transferResultsTable;

	private Group fileTransferGroup;

	private Label lblValidationErrors;

	private ProgressBar fileTransferProgress;

	private Button btnStartTransfer;
	private Button checkAutoScrollResults;

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

		Group grpTransferDestination = new Group(this, SWT.NONE);
		grpTransferDestination.setText("Transfer Destination");
		grpTransferDestination.setLayout(new FormLayout());
		FormData fd_grpTransferDestination = new FormData();
		fd_grpTransferDestination.bottom = new FormAttachment(grpInputSource, 54, SWT.BOTTOM);
		fd_grpTransferDestination.top = new FormAttachment(grpInputSource, 6);
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
		fd_btnSBucket.right = new FormAttachment(100, -10);
		fd_btnSBucket.left = new FormAttachment(100, -82);
		btnSBucket.setLayoutData(fd_btnSBucket);
		btnSBucket.setText("S3 Bucket...");

		Label lblLocation = new Label(grpTransferDestination, SWT.NONE);
		FormData fd_lblLocation = new FormData();
		fd_lblLocation.bottom = new FormAttachment(100, -22);
		fd_lblLocation.top = new FormAttachment(0, 5);
		fd_lblLocation.left = new FormAttachment(0, 7);
		lblLocation.setLayoutData(fd_lblLocation);
		lblLocation.setText("Location:");

		outputLocationField = new Text(grpTransferDestination, SWT.BORDER);
		fd_lblLocation.right = new FormAttachment(outputLocationField, -9);
		FormData fd_text_1 = new FormData();
		fd_text_1.right = new FormAttachment(btnFolder, -6);
		fd_text_1.top = new FormAttachment(0, 5);
		fd_text_1.left = new FormAttachment(0, 65);
		outputLocationField.setLayoutData(fd_text_1);
		outputLocationField.setEditable(false);

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

		FormData fd_fileTransferGroup = new FormData();
		fd_fileTransferGroup.top = new FormAttachment(lblValidationErrors, 5);
		fd_fileTransferGroup.right = new FormAttachment(grpInputSource, 0, SWT.RIGHT);
		fd_fileTransferGroup.left = new FormAttachment(0, 10);
		fd_fileTransferGroup.bottom = new FormAttachment(100, -18);
		fileTransferGroup.setLayoutData(fd_fileTransferGroup);

		checkAutoScrollResults = new Button(fileTransferGroup, SWT.CHECK);
		checkAutoScrollResults.setText("Auto-scroll results");
		checkAutoScrollResults.setSelection(true);
		FormData fd_checkAutoScrollResults = new FormData();
		fd_checkAutoScrollResults.top = new FormAttachment(0, 6);
		fd_checkAutoScrollResults.right = new FormAttachment(100, -6);
		checkAutoScrollResults.setLayoutData(fd_checkAutoScrollResults);
		checkAutoScrollResults.pack();
		
		btnStartTransfer = new Button(fileTransferGroup, SWT.NONE);
		btnStartTransfer.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				btnStartTransfer.setEnabled(false);
				transferResultsTable.getTable().removeAll();
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
		fd_progressBar.top = new FormAttachment(btnStartTransfer, 5, SWT.TOP);
		fd_progressBar.left = new FormAttachment(0, 10);
		fd_progressBar.right = new FormAttachment(btnStartTransfer, -10);
		fileTransferProgress.setLayoutData(fd_progressBar);

		Table table2 = new Table(fileTransferGroup, SWT.BORDER | SWT.FULL_SELECTION);
		transferResultsTable = new TableViewer(table2);
		FormData fd_table_1 = new FormData();
		fd_table_1.top = new FormAttachment(checkAutoScrollResults, 6);
		fd_table_1.left = new FormAttachment(0, 6);
		fd_table_1.right = new FormAttachment(100, -6);
		fd_table_1.bottom = new FormAttachment(btnStartTransfer, -6);

		table2.setLayoutData(fd_table_1);
		table2.setHeaderVisible(true);
		table2.setLinesVisible(true);

		TableViewerColumn statusColumn = new TableViewerColumn(transferResultsTable, SWT.NONE);
		statusColumn.getColumn().setText("Status");
		statusColumn.getColumn().setResizable(true);
		statusColumn.getColumn().setMoveable(false);
		statusColumn.getColumn().pack();
		statusColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((FileTransfer) element).getStatus();
			}

			@Override
			public Color getForeground(Object element) {
				if (((FileTransfer) element).getStatus().toUpperCase().equals("OK")) {
					return SWTResourceManager.getColor(SWT.COLOR_DARK_GREEN);
				} else {
					return SWTResourceManager.getColor(SWT.COLOR_RED);
				}
			}
		});

		TableViewerColumn localFileColumn = new TableViewerColumn(transferResultsTable, SWT.NONE);
		localFileColumn.getColumn().setText("Local path");
		localFileColumn.getColumn().setResizable(true);
		localFileColumn.getColumn().setMoveable(false);
		localFileColumn.getColumn().pack();
		localFileColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((FileTransfer) element).getFile().getAbsolutePath();
			}
		});

		TableViewerColumn remotePathColumn = new TableViewerColumn(transferResultsTable, SWT.NONE);
		remotePathColumn.getColumn().setText("Remote path");
		remotePathColumn.getColumn().setResizable(true);
		remotePathColumn.getColumn().setMoveable(false);
		remotePathColumn.getColumn().pack();
		remotePathColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((FileTransfer) element).getDestination();
			}
		});

		checkValidForTransfer();

		createContents();
	}

	/**
	 * Create contents of the shell.
	 */
	protected void createContents() {
		setText("SLC Upload");
		setSize(1032, 634);

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

	private void outputFolderSelected(String folderPath) {
		outputLocationField.setText("Folder: " + folderPath);
		checkValidForTransfer();
	}

	private void outputBucketSelected(String folderPath) {
		outputLocationField.setText("S3: " + folderPath);
		checkValidForTransfer();
	}

	private void checkValidForTransfer() {
		boolean valid = slcUploadController.isValidForTransfer(this::setValidationMessage);
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

	private void setValidationMessage(String message) {
		lblValidationErrors.setText(message);
		lblValidationErrors.setVisible(true);
	}

	private void updateProgress(double progress, boolean complete) {
		btnStartTransfer.setEnabled(complete);
		fileTransferProgress.setEnabled(!complete);
		fileTransferProgress.setSelection((int) Math.round(progress * fileTransferProgress.getMaximum()));
		if (complete) {
			fileTransferProgress.setSelection(0);
		}
	}

	private long lastPackTime = 0;
	
	private void fileTransferUpdate(FileTransfer transfer) {
		transferResultsTable.add(transfer);
		if (checkAutoScrollResults.getSelection())
		{
			transferResultsTable.getTable().setSelection(transferResultsTable.getTable().getItems().length - 1);
			transferResultsTable.getTable().showSelection();
		}
		
		long currentTime = System.currentTimeMillis();
		if(currentTime - lastPackTime > 5000)
		{
			for (TableColumn column : transferResultsTable.getTable().getColumns()) {
				column.pack();
			}
			lastPackTime = currentTime;
		}
	}
}
