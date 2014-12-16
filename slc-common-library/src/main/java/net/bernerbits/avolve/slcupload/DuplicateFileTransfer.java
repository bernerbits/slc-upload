package net.bernerbits.avolve.slcupload;

import net.bernerbits.avolve.slcupload.model.FileTransferObject;

public class DuplicateFileTransfer extends FileTransfer {

	private final FileTransfer original;
	private final int duplicateCount;

	public DuplicateFileTransfer(FileTransfer original, int duplicateCount) {
		this.original = original;
		this.duplicateCount = duplicateCount;
	}

	@Override
	public boolean isDuplicate() {
		return true;
	}
	
	public int getDuplicateCount() {
		return duplicateCount;
	}

	public String getPathAsString() {
		return original.getPathAsString();
	}

	public FileTransferObject getTransferObject() {
		return original.getTransferObject();
	}

	public String getDestination() {
		return original.getDestination();
	}

}
