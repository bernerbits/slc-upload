package net.bernerbits.avolve.slcupload.ui;

import java.io.IOException;
import java.nio.file.Path;

import net.bernerbits.avolve.slcupload.model.ExistingFileOptions;
import net.bernerbits.avolve.slcupload.ui.util.FileIcons;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wb.swt.SWTResourceManager;

public class ExistingFileDialog extends Dialog {

	protected ExistingFileOptions result;
	protected Shell shell;

	private Image fileIcon = FileIcons.getDefaultFileImage();
	private Label lblFileIcon;
	private Label lblExistingFileName;
	private Button radSkip;
	private Button radWrite;
	private Button cbRemember;
	private Button btnOK;

	/**
	 * Create the dialog.
	 * 
	 * @param parent
	 * @param style
	 */
	public ExistingFileDialog(Shell parent, int style) {
		super(parent, (style & ~SWT.CLOSE) | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL);
		setText("File Already Exists");
	}

	/**
	 * Open the dialog.
	 * 
	 * @return the result
	 */
	public ExistingFileOptions open(Path existingFile) {
		createContents();

		try {
			lblFileIcon.setImage(FileIcons.getFileImage(existingFile.toFile()));
		} catch (IOException e) {
			lblFileIcon.setImage(fileIcon);
		}
		lblExistingFileName.setText(existingFile.toAbsolutePath().normalize().toString());

		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}

	public void closeInUIThread() {
		shell.getDisplay().asyncExec(() -> {
			if (!shell.isDisposed()) {
				shell.dispose();
			}
		});
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		shell = new Shell(getParent(), getStyle());
		shell.setImage(SWTResourceManager.getImage(ExistingFileDialog.class, "/ftm-export.png"));
		shell.setSize(350, 300);
		shell.setText("File Already Exists");
		shell.setLayout(new FormLayout());

		lblFileIcon = new Label(shell, SWT.NONE);
		FormData fd_lblNewLabel = new FormData();
		fd_lblNewLabel.top = new FormAttachment(0, 10);
		fd_lblNewLabel.left = new FormAttachment(0, 10);
		lblFileIcon.setLayoutData(fd_lblNewLabel);
		lblFileIcon.setText("");
		lblFileIcon.setImage(fileIcon);

		Label lblStaticText1 = new Label(shell, SWT.NONE);
		FormData fd_lblStaticText1 = new FormData();
		fd_lblStaticText1.top = new FormAttachment(0, 10);
		fd_lblStaticText1.right = new FormAttachment(100, -10);
		fd_lblStaticText1.left = new FormAttachment(lblFileIcon, 10);
		lblStaticText1.setLayoutData(fd_lblStaticText1);
		lblStaticText1.setText("The file");

		lblExistingFileName = new Label(shell, SWT.WRAP);
		FormData fd_lblFileName = new FormData();
		fd_lblFileName.top = new FormAttachment(lblStaticText1, 10);
		fd_lblFileName.right = new FormAttachment(100, -10);
		fd_lblFileName.left = new FormAttachment(lblFileIcon, 10);
		lblExistingFileName.setLayoutData(fd_lblFileName);
		lblExistingFileName.setFont(SWTResourceManager.getBoldFont(lblExistingFileName.getFont()));
		lblExistingFileName.setText("");

		Label lblStaticText2 = new Label(shell, SWT.NONE);
		FormData fd_lblStaticText2 = new FormData();
		fd_lblStaticText2.top = new FormAttachment(lblExistingFileName, 10);
		fd_lblStaticText2.right = new FormAttachment(100, -10);
		fd_lblStaticText2.left = new FormAttachment(lblFileIcon, 10);
		lblStaticText2.setLayoutData(fd_lblStaticText2);
		lblStaticText2.setText("already exists in the destination.");

		Label lblStaticText3 = new Label(shell, SWT.NONE);
		FormData fd_lblStaticText3 = new FormData();
		fd_lblStaticText3.top = new FormAttachment(lblStaticText2, 20);
		fd_lblStaticText3.right = new FormAttachment(100, -10);
		fd_lblStaticText3.left = new FormAttachment(0, 10);
		lblStaticText3.setLayoutData(fd_lblStaticText3);
		lblStaticText3.setText("What would you like to do?");

		radSkip = new Button(shell, SWT.RADIO);
		FormData fd_radSkip = new FormData();
		fd_radSkip.top = new FormAttachment(lblStaticText3, 20);
		fd_radSkip.right = new FormAttachment(100, -10);
		fd_radSkip.left = new FormAttachment(0, 10);
		radSkip.setLayoutData(fd_radSkip);
		radSkip.setText("&Skip this file");

		radWrite = new Button(shell, SWT.RADIO);
		FormData fd_radWrite = new FormData();
		fd_radWrite.top = new FormAttachment(radSkip, 2);
		fd_radWrite.right = new FormAttachment(100, -10);
		fd_radWrite.left = new FormAttachment(0, 10);
		radWrite.setLayoutData(fd_radWrite);
		radWrite.setText("O&verwrite this file");

		cbRemember = new Button(shell, SWT.CHECK);
		FormData fd_cbRemember = new FormData();
		fd_cbRemember.top = new FormAttachment(radWrite, 5);
		fd_cbRemember.right = new FormAttachment(100, -10);
		fd_cbRemember.left = new FormAttachment(0, 10);
		cbRemember.setLayoutData(fd_cbRemember);
		cbRemember.setText("Do this for &all files");

		btnOK = new Button(shell, SWT.PUSH);
		FormData fd_btnOK = new FormData();
		fd_btnOK.right = new FormAttachment(100, -10);
		fd_btnOK.bottom = new FormAttachment(100, -10);
		btnOK.setLayoutData(fd_btnOK);
		btnOK.setText("&Ok");
		btnOK.setEnabled(false);

		radSkip.addListener(SWT.Selection, (e) -> btnOK.setEnabled(true));
		radWrite.addListener(SWT.Selection, (e) -> btnOK.setEnabled(true));

		btnOK.addListener(SWT.Selection, (e) -> {
			result = new ExistingFileOptions(radSkip.getSelection(), cbRemember.getSelection());
			shell.dispose();
		});
	}
}
