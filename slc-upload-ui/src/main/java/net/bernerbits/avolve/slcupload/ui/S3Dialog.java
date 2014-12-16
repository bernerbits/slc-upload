package net.bernerbits.avolve.slcupload.ui;

import java.io.IOException;
import java.util.List;
import java.util.prefs.Preferences;

import net.bernerbits.avolve.slcupload.model.RemoteFolder;
import net.bernerbits.avolve.slcupload.ui.controller.SLCUploadController;
import net.bernerbits.avolve.slcupload.ui.util.FileIcons;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.wb.swt.SWTResourceManager;

import com.google.common.base.Strings;

public class S3Dialog extends Dialog {
	private static Image folderIcon;
	private static Image bucketIcon;
	static {
	    try {
	    	folderIcon = FileIcons.getFolderImage();
			bucketIcon = FileIcons.getBucketImage(folderIcon.getBounds().height);
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}	
	
	protected @Nullable RemoteFolder result;
	
	protected Shell shell;
	private Text awsKeyText;
	private Text awsSecretText;

	private SLCUploadController uploadController;
	private TreeViewer s3BucketTree;
	private ProgressBar progress;
	private CLabel s3StatusLabel;
	private Button okButton;

	/**
	 * Create the dialog.
	 * 
	 * @param parent
	 * @param style
	 */
	public S3Dialog(SLCUploadController uploadController, Shell parent, int style) {
		super(parent, style | SWT.CLOSE | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL);
		setText("Select S3 Bucket");
		this.uploadController = uploadController;
	}

	/**
	 * Open the dialog.
	 * 
	 * @return the result
	 */
	public @Nullable RemoteFolder open() {
		createContents();
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

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		shell = new Shell(getParent(), getStyle());
		shell.setSize(576, 395);
		shell.setText("Select S3 Bucket");
		shell.setLayout(new FormLayout());
		shell.setBackgroundMode(SWT.INHERIT_FORCE);
		shell.setImage(SWTResourceManager.getImage(S3Dialog.class, "/ftm-export.png"));

		Label lblAwsKey = new Label(shell, SWT.NONE);
		FormData fd_lblAwsKey = new FormData();
		fd_lblAwsKey.top = new FormAttachment(0, 10);
		fd_lblAwsKey.left = new FormAttachment(0, 10);
		lblAwsKey.setLayoutData(fd_lblAwsKey);
		lblAwsKey.setText("AWS Key:");

		Label lblAwsSecret = new Label(shell, SWT.NONE);
		FormData fd_lblAwsSecret = new FormData();
		fd_lblAwsSecret.left = new FormAttachment(lblAwsKey, 0, SWT.LEFT);
		lblAwsSecret.setLayoutData(fd_lblAwsSecret);
		lblAwsSecret.setText("AWS Secret:");

		progress = new ProgressBar(shell, SWT.HORIZONTAL | SWT.INDETERMINATE);
		progress.setState(SWT.NORMAL);

		awsKeyText = new Text(shell, SWT.BORDER);
		awsKeyText.setText(Preferences.userNodeForPackage(SLCUploadUI.class).get("aws_key", ""));
		awsKeyText.addModifyListener((e) -> Preferences.userNodeForPackage(SLCUploadUI.class).put("aws_key",
				awsKeyText.getText() == null ? "" : awsKeyText.getText()));
		awsKeyText.addModifyListener((e) -> checkS3Connection());
		fd_lblAwsSecret.top = new FormAttachment(awsKeyText, 9);
		FormData fd_awsKeyText = new FormData();
		fd_awsKeyText.left = new FormAttachment(lblAwsKey, 6);
		awsKeyText.setLayoutData(fd_awsKeyText);

		awsSecretText = new Text(shell, SWT.BORDER | SWT.PASSWORD);
		awsSecretText.setText(Preferences.userNodeForPackage(SLCUploadUI.class).get("aws_secret", ""));
		awsSecretText.addModifyListener((e) -> Preferences.userNodeForPackage(SLCUploadUI.class).put("aws_secret",
				awsSecretText.getText() == null ? "" : awsSecretText.getText()));
		awsKeyText.addModifyListener((e) -> checkS3Connection());
		fd_awsKeyText.right = new FormAttachment(awsSecretText, 0, SWT.RIGHT);
		fd_awsKeyText.bottom = new FormAttachment(awsSecretText, -6);
		FormData fd_awsSecretText = new FormData();
		fd_awsSecretText.top = new FormAttachment(0, 34);
		fd_awsSecretText.right = new FormAttachment(100, -10);
		fd_awsSecretText.left = new FormAttachment(lblAwsSecret, 6);
		awsSecretText.setLayoutData(fd_awsSecretText);

		Button cancelButton = new Button(shell, SWT.NONE);
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(@Nullable SelectionEvent e) {
				finish(null);
			}
		});
		FormData fd_cancelButton = new FormData();
		fd_cancelButton.bottom = new FormAttachment(100, -10);
		fd_cancelButton.right = new FormAttachment(awsKeyText, 0, SWT.RIGHT);
		cancelButton.setLayoutData(fd_cancelButton);
		cancelButton.setText("Cancel");

		okButton = new Button(shell, SWT.NONE);
		FormData fd_okButton = new FormData();
		fd_okButton.top = new FormAttachment(cancelButton, 0, SWT.TOP);
		fd_okButton.right = new FormAttachment(cancelButton, -6);
		okButton.setLayoutData(fd_okButton);
		okButton.setText("OK");
		okButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(@Nullable SelectionEvent e) {
				RemoteFolder selected = s3BucketTree.getSelection().isEmpty() ? null
						: (RemoteFolder) ((IStructuredSelection) s3BucketTree.getSelection()).getFirstElement();
				finish(selected);
			}
		});
		okButton.setEnabled(false);

		FormData fd_progress = new FormData();
		fd_progress.right = new FormAttachment(awsKeyText, 0, SWT.RIGHT);
		fd_progress.left = new FormAttachment(0, 10);
		fd_progress.top = new FormAttachment(awsSecretText, 6);
		progress.setLayoutData(fd_progress);

		Tree tree = new Tree(shell, SWT.BORDER);
		s3BucketTree = new TreeViewer(tree);
		FormData fd_s3BucketTree = new FormData();
		fd_s3BucketTree.bottom = new FormAttachment(cancelButton, -6);
		fd_s3BucketTree.top = new FormAttachment(progress, 6);
		fd_s3BucketTree.right = new FormAttachment(awsKeyText, 0, SWT.RIGHT);
		fd_s3BucketTree.left = new FormAttachment(0, 10);
		tree.setLayoutData(fd_s3BucketTree);
		s3BucketTree.addDoubleClickListener((e) -> finish((RemoteFolder) ((IStructuredSelection) e.getSelection())
				.getFirstElement()));
		s3BucketTree.addSelectionChangedListener((e) -> okButton.setEnabled(!e.getSelection().isEmpty()));
		tree.setVisible(false);
		
		s3StatusLabel = new CLabel(shell, SWT.BORDER | SWT.WRAP);
		s3StatusLabel.setAlignment(SWT.CENTER);
		FormData fd_lblConnectingToS = new FormData();
		fd_lblConnectingToS.top = new FormAttachment(tree, 0, SWT.TOP);
		fd_lblConnectingToS.left = new FormAttachment(tree, 0, SWT.LEFT);
		fd_lblConnectingToS.bottom = new FormAttachment(tree, 0, SWT.BOTTOM);
		fd_lblConnectingToS.right = new FormAttachment(tree, 0, SWT.RIGHT);
		s3StatusLabel.setLayoutData(fd_lblConnectingToS);
		s3StatusLabel.setText("Connecting to S3...");
		s3StatusLabel.setVisible(true);
		
		checkS3Connection();
	}

	private boolean checkInProgress = false;
	private boolean checkNeeded = false;

	private void checkS3Connection() {
		if (!checkInProgress) {
			progress.setVisible(true);
			s3StatusLabel.setText("Connecting to S3...");
			s3StatusLabel.setVisible(true);
			s3BucketTree.getTree().setVisible(false);
			progress.setState(SWT.NORMAL);

			checkInProgress = true;
			String awsKey = awsKeyText.getText();
			String awsSecret = awsSecretText.getText();
			
			if (Strings.isNullOrEmpty(awsKey) || Strings.isNullOrEmpty(awsSecret))
			{
				s3StatusLabel.setText("Please enter your S3 credentials.");
				checkInProgress = false;
				progress.setVisible(false);
			}
			else
			{
				new Thread(() -> uploadController.listBuckets(awsKey, awsSecret,
						this::connectionCheckFinished, this::populateBucketTree)).start();
				;
			}
		} else {
			checkNeeded = true;
		}
	}

	private void connectionCheckFinished(boolean success) {
		shell.getDisplay().syncExec(() -> {
			checkInProgress = false;
			if (success) {
				progress.setVisible(false);
				s3StatusLabel.setVisible(false);
				s3BucketTree.getTree().setVisible(true);
			} else {
				progress.setState(SWT.PAUSED);
				s3StatusLabel.setText("Unable to connect to S3. Please check your S3 credentials.");
			}
			if (checkNeeded) {
				checkNeeded = false;
				if (!success) {
					checkS3Connection();
				}
			}
		});
	}

	private void populateBucketTree(List<RemoteFolder> buckets) {
		shell.getDisplay().syncExec(() -> {
			for(TreeItem item : s3BucketTree.getTree().getItems()) {
				item.dispose();
			}
			for (RemoteFolder bucket : buckets) {
				TreeItem item = new TreeItem(s3BucketTree.getTree(), SWT.NONE);
				item.setText(bucket.getName());
				item.setData(bucket);
				item.setImage(bucketIcon);
				populateTreeItem(item, bucket.getChildren());
			}
		});
	}

	private void populateTreeItem(TreeItem parent, List<RemoteFolder> buckets) {
		for (RemoteFolder bucket : buckets) {
			TreeItem item = new TreeItem(parent, SWT.NONE);
			item.setText(bucket.getName());
			item.setData(bucket);
			item.setImage(folderIcon);
			populateTreeItem(item, bucket.getChildren());
		}
	}

	private void finish(@Nullable RemoteFolder result) {
		this.result = result;
		shell.dispose();
	}

}
