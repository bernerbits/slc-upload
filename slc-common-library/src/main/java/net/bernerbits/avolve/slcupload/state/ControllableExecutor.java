package net.bernerbits.avolve.slcupload.state;

import static net.bernerbits.avolve.slcupload.state.ExecutionState.ABORTED;
import static net.bernerbits.avolve.slcupload.state.ExecutionState.ACTIVE;
import static net.bernerbits.avolve.slcupload.state.ExecutionState.FINISHED;
import static net.bernerbits.avolve.slcupload.state.ExecutionState.INACTIVE;
import static net.bernerbits.avolve.slcupload.state.ExecutionState.PAUSED;
import static net.bernerbits.avolve.slcupload.state.ExecutionState.PAUSING;
import static net.bernerbits.avolve.slcupload.state.ExecutionState.STOPPED;
import static net.bernerbits.avolve.slcupload.state.ExecutionState.STOPPING;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.apache.log4j.Logger;

public class ControllableExecutor<TD extends TaskDescriptor> {

	private static Logger logger = Logger.getLogger(ControllableExecutor.class);

	private final Lock stateLock = new ReentrantLock();
	private final Condition stateCondition = stateLock.newCondition();

	private final ExecutionStateChangeCallback stateChangeListener;
	private final TaskHandler<TD> taskHandler;

	@SuppressWarnings("serial")
	private final BlockingQueue<Runnable> execQueue = new LinkedBlockingQueue<Runnable>() {

		@Override
		public Runnable poll(long timeout, TimeUnit unit) throws InterruptedException {
			long nanos = System.nanoTime() + unit.toNanos(timeout);
			awaitState(ACTIVE, timeout, unit);
			long remainingNanos = nanos - System.nanoTime();
			if (remainingNanos > 0) {
				return super.poll(remainingNanos, TimeUnit.NANOSECONDS);
			} else {
				return null;
			}
		};

		@Override
		public Runnable take() throws InterruptedException {
			awaitStateInterruptibly(ACTIVE);
			return super.take();
		};

	};

	private final ExecutorService executor = new ThreadPoolExecutor(
			16, 16, 
			5L, TimeUnit.SECONDS, 
			execQueue) {{
		allowCoreThreadTimeOut(true);
	}};

	private int runningTasks;
	private ExecutionState state = INACTIVE;

	public ControllableExecutor(TaskHandler<TD> taskHandler, ExecutionStateChangeCallback stateChangeListener) {
		this.taskHandler = taskHandler;
		this.stateChangeListener = stateChangeListener;
	}

	private void lockState(Runnable r) {
		stateLock.lock();
		try {
			r.run();
		} finally {
			stateLock.unlock();
		}
	}

	private void lockState(InterruptibleRunnable r, long timeout, TimeUnit timeoutUnit) throws InterruptedException {
		if (stateLock.tryLock(timeout, timeoutUnit)) {
			try {
				r.run();
			} finally {
				stateLock.unlock();
			}
		}
	}

	private void lockStateInterruptibly(InterruptibleRunnable r) throws InterruptedException {
		stateLock.lockInterruptibly();
		try {
			r.run();
		} finally {
			stateLock.unlock();
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

	private void awaitState(ExecutionState expectedState, long timeout, TimeUnit timeoutUnit)
			throws InterruptedException {
		long nanos = timeoutUnit.toNanos(timeout);
		long nanoTimeout = System.nanoTime() + nanos;
		lockState(() -> {
			logger.debug("Waiting for state: " + expectedState);
			long remainingNanos = nanoTimeout - System.nanoTime();
			while (state != expectedState && remainingNanos > 0) {
				stateCondition.await(remainingNanos, TimeUnit.NANOSECONDS);
				remainingNanos = nanoTimeout - System.nanoTime();
			}
			logger.debug("State " + expectedState + " reached.");
		}, nanos, TimeUnit.NANOSECONDS);
	}

	private void awaitStateInterruptibly(ExecutionState expectedState) throws InterruptedException {
		lockStateInterruptibly(() -> {
			logger.debug("Waiting for state: " + expectedState);
			while (state != expectedState) {
				stateCondition.await();
			}
			logger.debug("State " + expectedState + " reached.");
		});
	}
	
	public void awaitState(ExecutionState expectedState) {
		lockState(() -> {
			logger.debug("Waiting for state: " + expectedState);
			while (state != expectedState) {
				stateCondition.awaitUninterruptibly();
			}
			logger.debug("State " + expectedState + " reached.");
		});
	}

	private void changeState(ExecutionState newState) {
		lockState(() -> {
			logger.debug("File execution state change: " + state + " -> " + newState);
			state = newState;
			stateCondition.signalAll();
			stateChangeListener.stateChange(newState);
		});
	}

	public void pause() {
		lockState(() -> {
			logger.debug("Pausing execution");
			if (state == ACTIVE) {
				logger.debug("Execution is active - pausing");
				if (runningTasks == 0) {
					changeState(PAUSED);
					logger.debug("Execution is paused.");
				} else {
					changeState(PAUSING);
					logger.debug("Pausing - awaiting " + runningTasks + " more tasks.");
				}
			} else {
				logger.warn("Couldn't pause - illegal state " + state + " found");
				throw new IllegalStateException("Tried to pause a execution while in " + state + " state");
			}
		});
	}

	public void resume() {
		lockState(() -> {
			logger.debug("Resuming execution");
			if (state == PAUSED) {
				logger.debug("Execution is paused - resuming");
				changeState(ACTIVE);
				logger.debug("Execution is active.");
				if (execQueue.isEmpty()) {
					executionFinished();
				} else {
					// Send a no-op task to jump-start the executor
					executor.execute(() -> {
					});
				}
			} else {
				logger.warn("Couldn't resume - illegal state " + state + " found");
				throw new IllegalStateException("Tried to resume a execution while in " + state + " state");
			}
		});
	}

	public void stop() {
		lockState(() -> {
			logger.debug("Stopping execution");
			if (state == PAUSED) {
				logger.debug("Execution is paused - resume first");
				resume();
			}
			if (state == ACTIVE) {
				logger.debug("Execution is active - stopping (clears the execution queue)");
				execQueue.clear();
				changeState(STOPPING);
				if(runningTasks == 0)
				{ 
					executionFinished();
				}
			} else {
				logger.warn("Couldn't stop - illegal state " + state + " found");
				throw new IllegalStateException("Tried to stop a execution while in " + state + " state");
			}
		});
	}

	public void start(Collection<TD> taskDefs) {
		lockState(() -> {
			if (stateIsOneOf(INACTIVE, FINISHED, STOPPED, ABORTED)) {
				changeState(ACTIVE);
				try {
					execQueue.clear();
					taskDefs.stream().forEach((td) -> executor.execute(() -> execute(td)));
				} catch (Throwable t) {
					logger.warn("An error caused the execution to abort early - setting to ABORTED", t);
					execQueue.clear();
					changeState(ABORTED);
					throw t;
				}
			} else {
				throw new IllegalStateException("Tried to start a execution while in " + state + " state");
			}
		});
	}

	private void executionFinished() {
		if (state == STOPPING) {
			logger.debug("Execution finished while stopping - setting to STOPPED.");
			changeState(STOPPED);
		} else if (state == ACTIVE) {
			logger.debug("Execution finished while active - setting to FINISHED.");
			changeState(FINISHED);
		} else {
			logger.warn("Execution finished in illegal state " + state + " setting to ABORTED.");
			changeState(ABORTED);
			throw new IllegalArgumentException("Completed file execution while in " + state + " state");
		}
	}

	private void execute(TD taskDesc) {
		lockState(() -> runningTasks++);
		try {
			taskHandler.handle(taskDesc);
		} finally {
			lockState(() -> {
				if (--runningTasks == 0) {
					if (state == PAUSING) {
						changeState(PAUSED);
					} else if (execQueue.isEmpty()) {
						executionFinished();
					}
				}
			});
		}
	}

	public boolean stateIsOneOf(ExecutionState... states) {
		return lockState(() -> {
			logger.trace("Checking execution state: state is " + state);
			for (ExecutionState stateToTest : states) {
				if (stateToTest == state) {
					return true;
				}
			}
			return false;
		});
	}

}
