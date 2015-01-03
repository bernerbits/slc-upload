package net.bernerbits.avolve.slcupload.ui.model;

import java.util.List;

import net.bernerbits.avolve.slcupload.model.FileTransferObject;

import org.apache.log4j.Logger;

public class FileSystemFileTransferOperation extends FileTransferOperation {

	private Logger logger = Logger.getLogger(FileSystemFileTransferOperation.class);

	private final String folderDestination;

	public FileSystemFileTransferOperation(String folderSource, List<FileTransferObject> transferObjects,
			String folderDestination) {
		super(folderSource, transferObjects);
		logger.debug("File System Transfer Operation: " + folderSource);
		this.folderDestination = folderDestination;
	}

	public String getFolderDestination() {
		return folderDestination;
	}

}
