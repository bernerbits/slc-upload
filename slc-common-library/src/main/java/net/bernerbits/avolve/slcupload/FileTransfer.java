package net.bernerbits.avolve.slcupload;

import java.io.File;

import net.bernerbits.avolve.slcupload.model.FileTransferObject;

public abstract class FileTransfer {
	private final FileTransferObject transferObject;

	public FileTransfer(FileTransferObject transferObject) {
		this.transferObject = transferObject;
	}

	public File getFile() {
		return FileTransferUtil.getLatestFile(transferObject);
	}

	protected String getRemotePath() {
		return FileTransferUtil.getRemotePath(transferObject);
	}

	public abstract String getDestination();

	public abstract String getStatus();

	public abstract void transfer();
}
