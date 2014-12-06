package net.bernerbits.avolve.slcupload;

import java.nio.file.Path;
import java.nio.file.Paths;

import net.bernerbits.avolve.slcupload.exception.FileTransferException;
import net.bernerbits.avolve.slcupload.model.FileTransferObject;

public class ErrorFileTransfer extends FileTransfer {

	private String file;

	public ErrorFileTransfer(String errorMessage, String file) {
		this.file = file;
		status = errorMessage;
	}

	@Override
	public String getPathAsString() {
		return file;
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

}
