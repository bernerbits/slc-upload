package net.bernerbits.avolve.slcupload.ui.presenter;

import net.bernerbits.avolve.slcupload.FileTransfer;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
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

	@SuppressWarnings("null")
	public @NonNull Color foregroundHint() {
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
		duplicateCount++;
	}
}
