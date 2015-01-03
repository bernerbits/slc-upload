package net.bernerbits.avolve.slcupload.ui.model;

import java.util.List;

import net.bernerbits.avolve.slcupload.model.FileTransferObject;
import net.bernerbits.avolve.slcupload.model.RemoteFolder;

import org.apache.log4j.Logger;

public class S3FileTransferOperation extends FileTransferOperation {

	private static Logger logger = Logger.getLogger(S3FileTransferOperation.class);

	private final RemoteFolder s3Destination;

	public S3FileTransferOperation(String folderSource, List<FileTransferObject> transferObjects,
			RemoteFolder s3Destination) {
		super(folderSource, transferObjects);
		logger.debug("S3 Transfer Operation: " + folderSource);
		this.s3Destination = s3Destination;
	}

	public RemoteFolder getS3Destination() {
		return s3Destination;
	}
}
