package net.bernerbits.avolve.slcupload.ui.presenter;

import net.bernerbits.avolve.slcupload.FileTransfer;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.wb.swt.SWTResourceManager;

public class FileTransferPresenter {
	private final FileTransfer fileTransfer;
	private int duplicateCount = 0;

	public FileTransferPresenter(FileTransfer fileTransfer) {
		this.fileTransfer = fileTransfer;
	}

	public @NonNull String status() {
		return fileTransfer.getStatus();
	}

	public @NonNull String duplicates() {
		return duplicateCount == 0 ? "" : "" + duplicateCount;
	}

	public @NonNull String localPath() {
		return fileTransfer.getPathAsString();
	}

	public @NonNull String remotePath() {
		return fileTransfer.getDestination();
	}

	public @NonNull RGB foregroundHint() {
		if (isError()) {
			return new RGB(255,0,0);
		} else if (isSkipped()) {
			return new RGB(128,128,0);
		} else {
			return new RGB(0,128,0);
		}
	}

	public boolean isSkipped() {
		return fileTransfer.getStatus().toUpperCase().startsWith("SKIPPED");
	}

	public boolean isError() {
		return !isSkipped() && !fileTransfer.getStatus().toUpperCase().equals("\u2713");
	}

	public void addDuplicate() {
		duplicateCount++;
	}
}
