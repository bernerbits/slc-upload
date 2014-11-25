package net.bernerbits.avolve.slcupload;

import java.io.File;

import net.bernerbits.avolve.slcupload.exception.FileTransferException;
import net.bernerbits.avolve.slcupload.model.FileTransferObject;


public class ErrorFileTransfer extends FileTransfer {

	private String errorMessage;
	private String file;

	public ErrorFileTransfer(String errorMessage, String file) {
		super(null, null);
		this.errorMessage = errorMessage;
		this.file = file;
		status = errorMessage;
	}

	public File getFile() throws FileTransferException {
		throw new FileTransferException(errorMessage);
	}

	@Override
	public FileTransferObject getTransferObject() {
		return new FileTransferObject(null, file, file);
	}
	
	@Override
	public String getDestination() {
		return "";
	}

	public String getLocalPath() {
		return file;
	}

	@Override
	public void transfer() {
	}
}
