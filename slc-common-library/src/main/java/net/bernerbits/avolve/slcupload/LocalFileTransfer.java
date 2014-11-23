package net.bernerbits.avolve.slcupload;

import java.io.File;
import java.io.IOException;

import net.bernerbits.avolve.slcupload.model.FileTransferObject;

import com.google.common.io.Files;

public class LocalFileTransfer extends FileTransfer {

	private final String localPath;

	private String status = "";

	public LocalFileTransfer(String localPath, FileTransferObject transferObject) {
		super(transferObject);
		this.localPath = localPath;
	}

	@Override
	public String getDestination() {
		return (localPath + '/' + getRemotePath()).replace('/', File.separatorChar);
	}

	@Override
	public String getStatus() {
		return status;
	}

	@Override
	public void transfer() {
		if (!getFile().exists()) {
			status = "File does not exist";
			return;
		}
		try {
			File dest = new File(getDestination());
			dest.getParentFile().mkdirs();
			Files.copy(getFile(), dest);
			status = "OK";
		} catch (IOException e) {
			status = "File could not be copied: " + e.getMessage();
		}
	}

}
