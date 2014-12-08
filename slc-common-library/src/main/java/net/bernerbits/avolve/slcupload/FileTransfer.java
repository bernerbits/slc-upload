package net.bernerbits.avolve.slcupload;

import net.bernerbits.avolve.slcupload.model.FileTransferObject;

public abstract class FileTransfer {

	protected String status = "";

	public FileTransfer() {
	}

	public boolean isDuplicate() {
		return false;
	}

	public final String getStatus()
	{
		return status;
	}

	public abstract String getPathAsString();

	public abstract FileTransferObject getTransferObject();

	public abstract String getDestination();

}
