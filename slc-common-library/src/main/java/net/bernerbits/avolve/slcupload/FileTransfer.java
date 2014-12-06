package net.bernerbits.avolve.slcupload;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import net.bernerbits.avolve.slcupload.model.FileTransferObject;

public abstract class FileTransfer {

	private final AtomicInteger duplicateCount = new AtomicInteger(0);
	protected String status = "";

	public FileTransfer() {
	}

	public boolean isDuplicate() {
		return false;
	}

	public int getDuplicateCount() {
		return duplicateCount.get();
	}

	public final void addDuplicate() {
		duplicateCount.incrementAndGet();
	}
	
	public final String getStatus()
	{
		return status;
	}

	public abstract String getPathAsString();

	public abstract FileTransferObject getTransferObject();

	public abstract String getDestination();

}
