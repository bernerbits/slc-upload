package net.bernerbits.avolve.slcupload;

import java.nio.file.Path;

import net.bernerbits.avolve.slcupload.exception.FileTransferException;
import net.bernerbits.avolve.slcupload.model.FileTransferObject;

import org.eclipse.jdt.annotation.Nullable;

public abstract class RealFileTransfer extends FileTransfer {

	private final FileTransferObject transferObject;
	private final Path path;
	private final String remotePath;
	private @Nullable String destination;

	public RealFileTransfer(String folderSource, FileTransferObject transferObject) throws FileTransferException {
		this.transferObject = transferObject;
		this.path = FileTransferUtil.getLatestFile(folderSource, transferObject);
		this.remotePath = FileTransferUtil.getRemotePath(folderSource, transferObject);
	}

	public Path getPath() {
		return path;
	}

	@Override
	public String getPathAsString() {
		return path.toString();
	}

	protected String getRemotePath() {
		return remotePath;
	}

	@Override
	public FileTransferObject getTransferObject() {
		return transferObject;
	}

	@Override
	public final synchronized String getDestination() {
		if (destination != null) {
			return destination;
		}
		destination = calculateDestination();
		return destination;
	}

	protected abstract String calculateDestination();

	public abstract void transfer();

}
