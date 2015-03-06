package net.bernerbits.avolve.slcupload;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import net.bernerbits.avolve.slcupload.callback.FileTransferCallback;
import net.bernerbits.avolve.slcupload.handler.ExistingFileHandler;
import net.bernerbits.avolve.slcupload.model.FileTransferObject;
import net.bernerbits.avolve.slcupload.model.RemoteFolder;
import net.bernerbits.avolve.slcupload.model.S3Folder;
import net.bernerbits.avolve.slcupload.state.TaskHandler;

import org.apache.log4j.Logger;

import com.google.common.base.Supplier;
import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;

public class FileTransferer implements TaskHandler<FileTransferObject> {
	private static Logger logger = Logger.getLogger(FileTransferer.class);

	private Function<FileTransferObject, FileTransfer> transferMapper;
	private Multiset<Path> pathCounts = ConcurrentHashMultiset.create();
	private Set<Path> finishedPaths = new HashSet<>();
	private FileTransferCallback callback;
	
	public FileTransferer(String folderSource, String folderDestination, FileTransferCallback callback,
			ExistingFileHandler handler) {
		logger.debug("Starting local transfer from " + folderSource + " to " + folderDestination);

		this.callback = callback;

		this.transferMapper = localFileTransfer(folderSource, folderDestination, handler);
	}

	public FileTransferer(String folderSource, RemoteFolder s3Destination, FileTransferCallback callback,
			ExistingFileHandler handler) {
		logger.debug("Starting S3 transfer from " + folderSource + " to " + s3Destination);

		this.callback = callback;

		S3Folder s3Folder = (S3Folder) s3Destination;
		this.transferMapper = remoteFileTransfer(folderSource, s3Folder, handler);

	}

	private static Function<FileTransferObject, FileTransfer> localFileTransfer(String folderSource,
			String folderDestination, ExistingFileHandler handler) {
		return (tobj) -> LocalFileTransfer.create(folderSource, folderDestination, tobj, handler);
	}

	private static Function<FileTransferObject, FileTransfer> remoteFileTransfer(String folderSource,
			S3Folder s3Folder, ExistingFileHandler handler) {
		return (tobj) -> S3FileTransfer.create(folderSource, s3Folder.getCredentials(), s3Folder.getBucketName(),
				s3Folder.getPrefix(), tobj, handler);
	}

	@Override
	public void handle(FileTransferObject task) {
		FileTransfer tr = transferMapper.apply(task);
		if (tr instanceof RealFileTransfer) {
			RealFileTransfer rtr = (RealFileTransfer) tr;
			Path p = rtr.getPath();
			
			int duplicates = pathCounts.add(p,1);
			if (duplicates == 0) {
				try {
					rtr.transfer();
				} finally {
					callback.onFileTransfer(rtr);
					doAndNotify(() -> finishedPaths.add(p));
				}
			} else {
				waitUntil(() -> finishedPaths.contains(p));
				callback.onFileTransfer(new DuplicateFileTransfer(rtr, duplicates));
			}
		} else {
			callback.onFileTransfer(tr);
		}
	}

	// Notify/wait helper methods
	
	private Lock pathsLock = new ReentrantLock();
	private Condition pathsCondition = pathsLock.newCondition();

	private void doAndNotify(Runnable r) {
		pathsLock.lock();
		try {
			r.run();
			pathsCondition.signalAll();
		} finally {
			pathsLock.unlock();
		}
	}
	
	private void waitUntil(Supplier<Boolean> condition) {
		pathsLock.lock();
		try {
			while (!condition.get()) {
				pathsCondition.awaitUninterruptibly();
			}
		} finally {
			pathsLock.unlock();
		}
	}
	
}
