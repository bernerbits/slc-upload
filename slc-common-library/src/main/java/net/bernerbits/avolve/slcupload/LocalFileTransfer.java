package net.bernerbits.avolve.slcupload;

import java.io.File;
import java.io.IOException;

import net.bernerbits.avolve.slcupload.exception.FileTransferException;
import net.bernerbits.avolve.slcupload.model.FileTransferObject;

import com.google.common.io.Files;

public class LocalFileTransfer extends FileTransfer {

	private final String localPath;

	public LocalFileTransfer(String folderSource, String localPath, FileTransferObject transferObject) {
		super(folderSource, transferObject);
		this.localPath = localPath;
	}

	@Override
	public String getDestination() throws FileTransferException {
		return (localPath + '/' + getRemotePath()).replace('/', File.separatorChar);
	}

	@Override
	public void transfer() {
		try {
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
		} catch (FileTransferException e) {
			status = e.getMessage();
		}
	}

}
