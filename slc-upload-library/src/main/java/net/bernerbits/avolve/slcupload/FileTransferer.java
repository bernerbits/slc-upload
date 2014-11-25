package net.bernerbits.avolve.slcupload;

import java.util.List;

import net.bernerbits.avolve.slcupload.callback.FileTransferCallback;
import net.bernerbits.avolve.slcupload.model.FileTransferObject;
import net.bernerbits.avolve.slcupload.model.RemoteFolder;
import net.bernerbits.avolve.slcupload.model.S3Folder;

public class FileTransferer {

	public void beginLocalTransfer(List<FileTransferObject> transferObjects, String folderSource,
			String folderDestination, FileTransferCallback callback) {
		transferObjects.parallelStream().map((tobj) -> new LocalFileTransfer(folderSource, folderDestination, tobj))
				.forEach((tr) -> {
					try {
						tr.transfer();
					} finally {
						callback.onFileTransfer(tr);
					}
				});
	}

	public void beginRemoteTransfer(List<FileTransferObject> transferObjects, String folderSource,
			RemoteFolder s3Destination, FileTransferCallback callback) {
		S3Folder s3Folder = (S3Folder) s3Destination;

		transferObjects
				.parallelStream()
				.map((tobj) -> new S3FileTransfer(folderSource, s3Folder.getCredentials(), s3Folder.getBucketName(),
						s3Folder.getPrefix(), tobj)).forEach((tr) -> {
					try {
						tr.transfer();
					} finally {
						callback.onFileTransfer(tr);
					}
				});
	}

}
