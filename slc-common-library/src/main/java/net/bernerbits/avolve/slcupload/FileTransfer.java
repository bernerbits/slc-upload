package net.bernerbits.avolve.slcupload;

import java.io.File;

import net.bernerbits.avolve.slcupload.exception.FileTransferException;
import net.bernerbits.avolve.slcupload.model.FileTransferObject;

public abstract class FileTransfer {
	
	private final FileTransferObject transferObject;
	private final String folderSource;
	protected String status = "";

	public FileTransfer(String folderSource, FileTransferObject transferObject) {
		this.folderSource = folderSource;
		this.transferObject = transferObject;
	}

	public File getFile() throws FileTransferException {
		return FileTransferUtil.getLatestFile(folderSource, transferObject);
	}

	protected String getRemotePath() throws FileTransferException {
		return FileTransferUtil.getRemotePath(folderSource, transferObject);
	}
	
	public final String getStatus()
	{
		return status;
	}

	public FileTransferObject getTransferObject() {
		return transferObject;
	}

	public abstract String getDestination() throws FileTransferException;

	public abstract void transfer();
	
}
