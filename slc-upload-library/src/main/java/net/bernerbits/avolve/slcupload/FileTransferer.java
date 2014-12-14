package net.bernerbits.avolve.slcupload;

import java.nio.file.Path;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.bernerbits.avolve.slcupload.callback.FileTransferCallback;
import net.bernerbits.avolve.slcupload.model.FileTransferObject;
import net.bernerbits.avolve.slcupload.model.RemoteFolder;
import net.bernerbits.avolve.slcupload.model.S3Folder;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public class FileTransferer {
	private static enum TransferState {
		INACTIVE, ACTIVE, PAUSING, PAUSED, FINISHED, STOPPING, STOPPED
	};

	// Allow (essentially) unlimited parallel transfers
	private static final int PERMIT_COUNT = Integer.MAX_VALUE;
	private final Semaphore transferPermits = new Semaphore(PERMIT_COUNT, true);

	private final Lock stateLock = new ReentrantLock();
	private final Condition stateCondition = stateLock.newCondition();

	// Any thread can change the state
	private TransferState state = TransferState.INACTIVE;

	private void lockState(Runnable r)
	{
		stateLock.lock();
		try {
			r.run();
		} finally {
			stateLock.unlock();
		}
	}

	private void unlockState(Runnable r)
	{
		stateLock.unlock();
		try {
			r.run();
		} finally {
			stateLock.lock();
		}
	}

	private <T> T lockState(Supplier<T> s)
	{
		stateLock.lock();
		try {
			return s.get();
		} finally {
			stateLock.unlock();
		}
	}

	private void awaitState(TransferState expectedState)
	{
		lockState(() -> {
			while (state != expectedState)
			{
				stateCondition.awaitUninterruptibly();
			}
		});
	}
	
	private void changeState(TransferState newState)
	{
		lockState(() -> {
			state = newState;
			stateCondition.signalAll();
		});
	}
	
	public void pauseTransfer() {
		lockState(() -> {
			if (state == TransferState.ACTIVE) {
				changeState(TransferState.PAUSING);
				// Grab all the permits we can, and then get the rest one by one.
				int outstandingPermits = PERMIT_COUNT;
				outstandingPermits -= transferPermits.drainPermits();
				while (outstandingPermits > 0) {
					transferPermits.acquireUninterruptibly();
					outstandingPermits--;
				}
				changeState(TransferState.PAUSED);
			} else {
				throw new IllegalStateException("Tried to pause a transfer while in " + state + " state");
			}
		});
	}

	public void resumeTransfer() {
		lockState(() -> {
			if (state == TransferState.PAUSED) {
				transferPermits.release(PERMIT_COUNT);
				changeState(TransferState.ACTIVE);
			} else {
				throw new IllegalStateException("Tried to resume a transfer while in " + state + " state");
			}
		});
	}

	public void stopTransfer() {
		lockState(() -> {
			if (state == TransferState.PAUSED) {
				resumeTransfer();
			}
			if (state == TransferState.ACTIVE) {
				state = TransferState.STOPPING;
				awaitState(TransferState.STOPPED);
			} else {
				throw new IllegalStateException("Tried to stop a transfer while in " + state + " state");
			}
		});
	}

	private void withState(Stream<FileTransfer> ftStream, Consumer<FileTransfer> ftConsumer) {
		lockState(() -> {
			if (state == TransferState.INACTIVE || state == TransferState.FINISHED || state == TransferState.STOPPED) {
				changeState(TransferState.ACTIVE);
				try {
					unlockState(() -> 
						interruptibleStream(ftStream).forEach(ftConsumer)
					);
				} finally {
					// In case we paused at the last possible moment,
					// wait here for the client to resume the transfer.
					if (state == TransferState.STOPPING) {
						changeState(TransferState.STOPPED);
					} else if (state == TransferState.ACTIVE) {
						changeState(TransferState.FINISHED);
					} else {
						throw new IllegalArgumentException("Completed file transfer while in " + state + " state");
					}
				}
			} else {
				throw new IllegalStateException("Tried to start a transfer while in " + state + " state");
			}
		});
	}

	private <T> Stream<T> interruptibleStream(Stream<T> originalStream)
	{
		return StreamSupport.stream(interruptibleSpliterator(originalStream.spliterator()), originalStream.isParallel());
	}

	private <T> Spliterator<T> interruptibleSpliterator(Spliterator<T> spliterator)
	{
		return new Spliterator<T>() {
			@Override
			public int characteristics() {
				return spliterator.characteristics();
			}
			@Override
			public long estimateSize() {
				if(lockState(() -> state == TransferState.STOPPING)) {
					return 0;
				} else {
					return spliterator.characteristics();
				}
			}
			@Override
			public boolean tryAdvance(Consumer<? super T> action) {
				if(lockState(() -> state == TransferState.STOPPING)) {
					return false;
				} else {
					return spliterator.tryAdvance(action);
				}
			}
			@Override
			public Spliterator<T> trySplit() {
				if(lockState(() -> state == TransferState.STOPPING)) {
					return null;
				} else {
					Spliterator<T> split = spliterator.trySplit();
					return split == null ? null : interruptibleSpliterator(split);
				}
			}
		};
	}

	
	public void performLocalTransfer(List<FileTransferObject> transferObjects, String folderSource,
			String folderDestination, FileTransferCallback callback) {
		Multiset<Path> pathCounts = ConcurrentHashMultiset.create();
		withState(transferObjects.stream().map(localFileTransfer(folderSource, folderDestination)),
				transfer(pathCounts, callback));
	}

	public void performRemoteTransfer(List<FileTransferObject> transferObjects, String folderSource,
			RemoteFolder s3Destination, FileTransferCallback callback) {
		S3Folder s3Folder = (S3Folder) s3Destination;
		Multiset<Path> pathCounts = HashMultiset.create();
		withState(transferObjects.parallelStream().map(remoteFileTransfer(folderSource, s3Folder)),
				transfer(pathCounts, callback));
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
		transferPermits.acquireUninterruptibly();
		try {
			if (tr instanceof RealFileTransfer) {
				RealFileTransfer rtr = (RealFileTransfer) tr;
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
		} finally {
			transferPermits.release();
		}
	}

}
