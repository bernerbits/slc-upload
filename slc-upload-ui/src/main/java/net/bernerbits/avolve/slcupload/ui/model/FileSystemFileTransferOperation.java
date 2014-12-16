package net.bernerbits.avolve.slcupload.ui.model;

import java.util.List;

import net.bernerbits.avolve.slcupload.model.FileTransferObject;

public class FileSystemFileTransferOperation extends FileTransferOperation {

	private final String folderDestination;

	public FileSystemFileTransferOperation(String folderSource, List<FileTransferObject> transferObjects,
			String folderDestination) {
		super(folderSource, transferObjects);
		this.folderDestination = folderDestination;
	}

	public String getFolderDestination() {
		return folderDestination;
	}
	
}
