package net.bernerbits.avolve.slcupload;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.FileExistsException;

import net.bernerbits.avolve.slcupload.exception.FileTransferException;
import net.bernerbits.avolve.slcupload.exception.FileTransferMissingFileException;
import net.bernerbits.avolve.slcupload.model.FileTransferObject;

public class LocalFileTransfer extends RealFileTransfer {

	private final String localPath;

	public LocalFileTransfer(String folderSource, String localPath, FileTransferObject transferObject) throws FileTransferException {
		super(folderSource, transferObject);
		this.localPath = localPath;
	}

	@Override
	public String calculateDestination() {
		return (localPath + '/' + getRemotePath()).replace('/', File.separatorChar);
	}

	@Override
	public void transfer() {
		if (!Files.exists(getPath())) {
			status = "File does not exist";
			return;
		}
		try {
			Path dest = Paths.get(getDestination());
			Files.createDirectories(dest.getParent());
			Files.copy(getPath(), dest);
			status = "OK";
		} catch (FileExistsException | FileAlreadyExistsException e) {
			status = "Skipped - File Exists";
		} catch (FileSystemException e) {
			status = "File could not be copied: " + e.getReason();
		} catch (IOException e) {
			status = "File could not be copied: " + e.getMessage();
		}
	}

	public static FileTransfer create(String folderSource, String localPath, FileTransferObject transferObject) {
		try {
			return new LocalFileTransfer(folderSource, localPath, transferObject);
		} catch (FileTransferMissingFileException e) {
			return new ErrorFileTransfer(e.getMessage(), e.getPath());
		} catch (FileTransferException e) {
			return new ErrorFileTransfer(e.getMessage(), transferObject.getFileName());
		}
	}

}
