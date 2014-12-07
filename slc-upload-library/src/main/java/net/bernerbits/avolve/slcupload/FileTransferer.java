package net.bernerbits.avolve.slcupload;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import net.bernerbits.avolve.slcupload.callback.FileTransferCallback;
import net.bernerbits.avolve.slcupload.model.FileTransferObject;
import net.bernerbits.avolve.slcupload.model.RemoteFolder;
import net.bernerbits.avolve.slcupload.model.S3Folder;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public class FileTransferer {

	public void beginLocalTransfer(List<FileTransferObject> transferObjects, String folderSource,
			String folderDestination, FileTransferCallback callback) {
		Multiset<Path> pathCounts = ConcurrentHashMultiset.create();
		transferObjects.parallelStream().map(localFileTransfer(folderSource, folderDestination))
				.forEachOrdered(transfer(pathCounts, callback));
	}

	public void beginRemoteTransfer(List<FileTransferObject> transferObjects, String folderSource,
			RemoteFolder s3Destination, FileTransferCallback callback) {
		S3Folder s3Folder = (S3Folder) s3Destination;
		Multiset<Path> pathCounts = HashMultiset.create();

		transferObjects.parallelStream().map(remoteFileTransfer(folderSource, s3Folder))
				.forEachOrdered(transfer(pathCounts, callback));
	}

	private Function<FileTransferObject, FileTransfer> localFileTransfer(String folderSource, String folderDestination) {
		return (tobj) -> LocalFileTransfer.create(folderSource, folderDestination, tobj);
	}

	private Function<FileTransferObject, FileTransfer> remoteFileTransfer(String folderSource, S3Folder s3Folder) {
		return (tobj) -> S3FileTransfer.create(folderSource, s3Folder.getCredentials(), s3Folder.getBucketName(),
				s3Folder.getPrefix(), tobj);
	}

	private Consumer<FileTransfer> transfer(Multiset<Path> pathCounts, FileTransferCallback callback) {
		return (tr) -> doTransfer(tr, pathCounts, callback);
	}

	private void doTransfer(FileTransfer tr, Multiset<Path> pathCounts, FileTransferCallback callback) {
		if (tr instanceof RealFileTransfer) {
			RealFileTransfer rtr = (RealFileTransfer)tr;
			Path p = rtr.getPath();
			try {
				if (!pathCounts.contains(p)) {
					try {
						rtr.transfer();
					} finally {
						callback.onFileTransfer(rtr);
					}
				} else {
					callback.onFileTransfer(new DuplicateFileTransfer(rtr, pathCounts.count(p)));
				}
			} finally {
				pathCounts.add(p);
			}
		} else {
			callback.onFileTransfer(tr);
		}
	}
}
