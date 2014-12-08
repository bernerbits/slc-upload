package net.bernerbits.avolve.slcupload.ui.presenter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.wb.swt.SWTResourceManager;

import net.bernerbits.avolve.slcupload.FileTransfer;

public class FileTransferPresenter {
	private final FileTransfer fileTransfer;
	private int duplicateCount = 0;
	
	public FileTransferPresenter(FileTransfer fileTransfer) {
		this.fileTransfer = fileTransfer;
	}

	public String status() {
		return fileTransfer.getStatus();
	}
	
	public String duplicates() {
		return duplicateCount == 0 ? "" : Integer.toString(duplicateCount);
	}
	
	public String localPath() {
		return fileTransfer.getPathAsString();
	}
	
	public String remotePath() {
		return fileTransfer.getDestination();
	}
	
	public Color foregroundHint() {
		if (!isError()) {
			return SWTResourceManager.getColor(SWT.COLOR_DARK_GREEN);
		} else {
			return SWTResourceManager.getColor(SWT.COLOR_RED);
		}
	}

	public boolean isError() {
		return !fileTransfer.getStatus().toUpperCase().equals("\u2713");
	}

	public void addDuplicate() {
		duplicateCount ++;
	}
}
