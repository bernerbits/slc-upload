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
import net.bernerbits.avolve.slcupload.callback.FileTransferStateChangeCallback;
import net.bernerbits.avolve.slcupload.handler.ExistingFileHandler;
import net.bernerbits.avolve.slcupload.model.FileTransferObject;
import net.bernerbits.avolve.slcupload.model.RemoteFolder;
import net.bernerbits.avolve.slcupload.model.S3Folder;

import org.apache.log4j.Logger;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;

public class FileTransferer {
	private static Logger logger = Logger.getLogger(FileTransferer.class);

	public static enum TransferState {
		INACTIVE, ACTIVE, PAUSING, PAUSED, FINISHED, STOPPING, STOPPED, ABORTED
	};

	// Allow (essentially) unlimited parallel transfers
	private static final int PERMIT_COUNT = Integer.MAX_VALUE;
	private final Semaphore transferPermits = new Semaphore(PERMIT_COUNT, true);

	private final Lock stateLock = new ReentrantLock();
	@SuppressWarnings("null")
	private final Condition stateCondition = stateLock.newCondition();

	// Any thread can change the state
	private TransferState state = TransferState.INACTIVE;
	private FileTransferStateChangeCallback stateChangeListener;

	private void lockState(Runnable r) {
		stateLock.lock();
		try {
			r.run();
		} finally {
			stateLock.unlock();
		}
	}

	private void unlockState(Runnable r) {
		stateLock.unlock();
		try {
			r.run();
		} finally {
			stateLock.lock();
		}
	}

	private <T> T lockState(Supplier<T> s) {
		stateLock.lock();
		try {
			return s.get();
		} finally {
			stateLock.unlock();
		}
	}

	private void awaitState(TransferState expectedState) {
		lockState(() -> {
			logger.debug("Waiting for state: " + expectedState);
			while (state != expectedState) {
				stateCondition.awaitUninterruptibly();
			}
			logger.debug("State " + expectedState + " reached.");
		});
	}

	private void changeState(TransferState newState) {
		lockState(() -> {
			logger.debug("File transfer state change: " + state + " -> " + newState);
			state = newState;
			stateCondition.signalAll();
			stateChangeListener.stateChange(newState);
		});
	}

	public void pauseTransfer() {
		lockState(() -> {
			logger.debug("Pausing transfer");
			if (state == TransferState.ACTIVE) {
				logger.debug("Transfer is active - pausing (drains all semaphore permits)");
				changeState(TransferState.PAUSING);
				// Grab all the permits we can, and then get the rest one by
				// one.
				int outstandingPermits = PERMIT_COUNT;
				outstandingPermits -= transferPermits.drainPermits();
				logger.debug("Drained permits - " + outstandingPermits + " remaining");
				while (outstandingPermits > 0) {
					transferPermits.acquireUninterruptibly();
					outstandingPermits--;
					logger.trace(outstandingPermits + " permits remaining");
				}
				changeState(TransferState.PAUSED);
				logger.debug("Transfer is paused.");
			} else {
				logger.warn("Couldn't pause - illegal state " + state + " found");
				throw new IllegalStateException("Tried to pause a transfer while in " + state + " state");
			}
		});
	}

	public void resumeTransfer() {
		lockState(() -> {
			logger.debug("Resuming transfer");
			if (state == TransferState.PAUSED) {
				logger.debug("Transfer is paused - resuming (releases semaphore permits)");
				transferPermits.release(PERMIT_COUNT);
				changeState(TransferState.ACTIVE);
				logger.debug("Transfer is active.");
			} else {
				logger.warn("Couldn't resume - illegal state " + state + " found");
				throw new IllegalStateException("Tried to resume a transfer while in " + state + " state");
			}
		});
	}

	public void stopTransfer() {
		lockState(() -> {
			logger.debug("Stopping transfer");
			if (state == TransferState.PAUSED) {
				logger.debug("Transfer is paused - resume first");
				resumeTransfer();
			}
			if (state == TransferState.ACTIVE) {
				logger.debug("Transfer is active - stopping (signals spliterator to act empty)");
				state = TransferState.STOPPING;
				awaitState(TransferState.STOPPED);
				logger.debug("Transfer is stopped.");
			} else {
				logger.warn("Couldn't stop - illegal state " + state + " found");
				throw new IllegalStateException("Tried to stop a transfer while in " + state + " state");
			}
		});
	}

	private void withState(Stream<FileTransfer> ftStream, Consumer<FileTransfer> ftConsumer) {
		lockState(() -> {
			if (state == TransferState.INACTIVE || state == TransferState.FINISHED || state == TransferState.STOPPED
					|| state == TransferState.ABORTED) {
				changeState(TransferState.ACTIVE);
				try {
					unlockState(() -> interruptibleStream(ftStream).forEach(ftConsumer));
					if (state == TransferState.STOPPING) {
						logger.debug("Transfer finished while stopping - setting to STOPPED.");
						changeState(TransferState.STOPPED);
					} else if (state == TransferState.ACTIVE) {
						logger.debug("Transfer finished while active - setting to FINISHED.");
						changeState(TransferState.FINISHED);
					} else {
						logger.warn("Transfer finished in illegal state " + state + " setting to ABORTED.");
						changeState(TransferState.ABORTED);
						throw new IllegalArgumentException("Completed file transfer while in " + state + " state");
					}
				} catch (Throwable t) {
					logger.warn("An error caused the transfer to abort early - setting to ABORTED", t);
					changeState(TransferState.ABORTED);
					throw t;
				}
			} else {
				throw new IllegalStateException("Tried to start a transfer while in " + state + " state");
			}
		});
	}

	private <T> Stream<T> interruptibleStream(Stream<T> originalStream) {
		return StreamSupport
				.stream(interruptibleSpliterator(originalStream.spliterator()), originalStream.isParallel());
	}

	private <T> Spliterator<T> interruptibleSpliterator(Spliterator<T> spliterator) {
		return new Spliterator<T>() {
			@Override
			public int characteristics() {
				return spliterator.characteristics();
			}

			@Override
			public long estimateSize() {
				if (lockState(() -> state == TransferState.STOPPING)) {
					return 0;
				} else {
					return spliterator.characteristics();
				}
			}

			@Override
			public boolean tryAdvance(@Nullable Consumer<? super T> action) {
				if (lockState(() -> state == TransferState.STOPPING)) {
					return false;
				} else {
					return spliterator.tryAdvance(action);
				}
			}

			@Override
			public @Nullable Spliterator<T> trySplit() {
				if (lockState(() -> state == TransferState.STOPPING)) {
					return null;
				} else {
					Spliterator<T> split = spliterator.trySplit();
					return split == null ? null : interruptibleSpliterator(split);
				}
			}
		};
	}

	public void performLocalTransfer(List<FileTransferObject> transferObjects, String folderSource,
			String folderDestination, FileTransferCallback callback, ExistingFileHandler handler,
			FileTransferStateChangeCallback stateChangeListener) {
		logger.debug("Starting local transfer from " + folderSource + " to " + folderDestination);
		this.stateChangeListener = stateChangeListener;
		withState(transferObjects.stream().map(localFileTransfer(folderSource, folderDestination, handler)),
				transfer(ConcurrentHashMultiset.create(), callback));
	}

	public void performRemoteTransfer(List<FileTransferObject> transferObjects, String folderSource,
			RemoteFolder s3Destination, FileTransferCallback callback, ExistingFileHandler handler,
			FileTransferStateChangeCallback stateChangeListener) {
		logger.debug("Starting S3 transfer from " + folderSource + " to " + s3Destination);
		this.stateChangeListener = stateChangeListener;
		S3Folder s3Folder = (S3Folder) s3Destination;
		withState(transferObjects.parallelStream().map(remoteFileTransfer(folderSource, s3Folder, handler)),
				transfer(ConcurrentHashMultiset.create(), callback));
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

	public boolean stateIsOneOf(TransferState[] states) {
		return lockState(() -> {
			logger.trace("Checking transfer state: state is " + state);
			for (TransferState stateToTest : states) {
				if (stateToTest == state) {
					return true;
				}
			}
			return false;
		});
	}

}
