package net.bernerbits.avolve.slcupload;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import net.bernerbits.avolve.slcupload.exception.FileTransferException;
import net.bernerbits.avolve.slcupload.exception.FileTransferMissingFileException;
import net.bernerbits.avolve.slcupload.handler.ExistingFileHandler;
import net.bernerbits.avolve.slcupload.model.ExistingFileOptions;
import net.bernerbits.avolve.slcupload.model.FileTransferObject;

import org.apache.commons.io.FileExistsException;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

public class LocalFileTransfer extends RealFileTransfer {

	private final String localPath;
	private ExistingFileHandler existingFileHandler;

	public LocalFileTransfer(String folderSource, String localPath, FileTransferObject transferObject, ExistingFileHandler handler) throws FileTransferException {
		super(folderSource, transferObject);
		this.localPath = localPath;
		this.existingFileHandler = handler;
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
			CopyOption[] cpOptions = new CopyOption[0];
			if (Files.exists(dest)) {
				ExistingFileOptions options = existingFileHandler.getOptions(dest);
				if (!options.isSkip()) {
					cpOptions = new CopyOption[]{StandardCopyOption.REPLACE_EXISTING};
				}
			}
			Files.createDirectories(dest.getParent());
			Files.copy(getPath(), dest, cpOptions);
			
			if(!verifyCopy(getPath(), dest))
			{
				status = "File was not copied correctly. Please try again.";
			}
			else
			{
				status = "\u2713";
			}
		} catch (FileExistsException | FileAlreadyExistsException e) {
			status = "Skipped - File Exists";
		} catch (FileSystemException e) {
			status = "File could not be copied: " + e.getReason();
		} catch (IOException e) {
			status = "File could not be copied: " + e.getMessage();
		}
	}

	private static boolean verifyCopy(Path source, Path dest) throws IOException {
		HashCode sourceMd5 = com.google.common.io.Files.hash(source.toFile(), Hashing.md5());
		HashCode destMd5 = com.google.common.io.Files.hash(dest.toFile(), Hashing.md5());
		return sourceMd5.equals(destMd5);
	}

	public static FileTransfer create(String folderSource, String localPath, FileTransferObject transferObject, ExistingFileHandler handler) {
		try {
			return new LocalFileTransfer(folderSource, localPath, transferObject, handler);
		} catch (FileTransferMissingFileException e) {
			return new ErrorFileTransfer(e.getMessage(), e.getPath());
		} catch (FileTransferException e) {
			return new ErrorFileTransfer(e.getMessage(), transferObject.getFileName());
		}
	}

}
