package net.bernerbits.avolve.slcupload.ui.model;

import java.util.List;

import net.bernerbits.avolve.slcupload.model.FileTransferObject;
import net.bernerbits.avolve.slcupload.model.RemoteFolder;

public class S3FileTransferOperation extends FileTransferOperation {

	private final RemoteFolder s3Destination;

	public S3FileTransferOperation(String folderSource, List<FileTransferObject> transferObjects,
			RemoteFolder s3Destination) {
		super(folderSource, transferObjects);
		this.s3Destination = s3Destination;
	}

	public RemoteFolder getS3Destination() {
		return s3Destination;
	}
}
