package net.bernerbits.avolve.slcupload.ui.model;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.bernerbits.avolve.slcupload.model.FileTransferObject;

public abstract class FileTransferOperation {

	private final String folderSource;
	private final List<FileTransferObject> transferObjects;
	private final AtomicInteger counter = new AtomicInteger(0);
	
	public FileTransferOperation(String folderSource, List<FileTransferObject> transferObjects) {
		this.folderSource = folderSource;
		this.transferObjects = transferObjects;
	}

	public String getFolderSource() {
		return folderSource;
	}

	public List<FileTransferObject> getTransferObjects() {
		return transferObjects;
	}

	public int currentCount() {
		return counter.get();
	}

	public void resetCount() {
		counter.set(0);
	}

	public float updateCount() {
		return counter.incrementAndGet();
	}

	public boolean isComplete() {
		return counter.get() == transferObjects.size();
	}

}
