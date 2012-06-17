package hemera.core.execution.executable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import hemera.core.execution.interfaces.task.IEventTask;
import hemera.core.execution.interfaces.task.handle.IEventTaskHandle;

/**
 * <code>EventExecutable</code> defines a composite
 * container unit of an event task, as well as being
 * the task handle for the contained task.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public class EventExecutable implements IEventTaskHandle {
	/**
	 * The <code>IEventTask</code> to be executed.
	 */
	private final IEventTask task;
	/**
	 * The execution <code>Lock</code>.
	 * <p>
	 * The locking sequence is first lock execution
	 * lock, then lock completion lock.
	 */
	private final Lock executionLock;
	/**
	 * The completion <code>Lock</code>.
	 * <p>
	 * The locking sequence is first lock execution
	 * lock, then lock completion lock.
	 */
	private final Lock completionLock;
	/**
	 * The completion waiting <code>Condition</code>.
	 */
	private final Condition completionCondition;
	/**
	 * The <code>boolean</code> indicating if the
	 * task execution has been canceled.
	 * <p>
	 * This value is guarded by the execution lock.
	 */
	private boolean canceled;
	/**
	 * The <code>boolean</code> indicating if the
	 * task execution has been completed.
	 * <p>
	 * This value is guarded by both the execution lock
	 * and the completion lock.
	 */
	private boolean completed;

	/**
	 * Constructor of <code>EventExecutable</code>.
	 */
	protected EventExecutable() {
		this.task = null;
		this.executionLock = new ReentrantLock();
		this.completionLock = new ReentrantLock();
		this.completionCondition = this.completionLock.newCondition();
	}

	/**
	 * Constructor of <code>EventExecuable</code>.
	 * @param task The <code>IEventTask</code> to be
	 * executed.
	 */
	public EventExecutable(final IEventTask task) {
		this.task = task;
		this.executionLock = new ReentrantLock();
		this.completionLock = new ReentrantLock();
		this.completionCondition = this.completionLock.newCondition();
	}

	/**
	 * Execute the contained task.
	 * @throws Exception If any processing failed.
	 */
	public final void execute() throws Exception {
		// Acquire execution lock.
		this.executionLock.lock();
		try {
			// Check cancelled status while holding the execution lock,
			// since cancellation can only set this status after acquiring
			// this lock.
			if (this.canceled) return;
			// Execute task.
			this.executeTask();
			// Signal completion.
			this.completionLock.lock();
			this.completed = true;
			try {
				this.completionCondition.signalAll();
			} finally {
				this.completionLock.unlock();
			}
		} finally {
			this.executionLock.unlock();
		}
	}
	
	/**
	 * Execute the contained task.
	 * @throws Exception If task execution failed.
	 */
	protected void executeTask() throws Exception {
		this.task.execute();
	}

	@Override
	public boolean await() throws InterruptedException {
		return this.await(-1, null);
	}

	@Override
	public boolean await(final long value, final TimeUnit unit) throws InterruptedException {
		// Wait to acquire the execution lock. This will only go through
		// if the execution has not yet started or has completed.
		this.executionLock.lock();
		try {
			// This will catch the case where the execution has completed.
			if (this.completed) return true;
			this.completionLock.lock();
			try {
				// Must release execution lock before go into waiting to
				// allow executor to acquire the lock and execute task.
				// Here we still hold the completion lock, so the executor
				// cannot signal completion before we go into waiting, so
				// that we won't miss the signal.
				this.executionLock.unlock();
				// Perform wait.
				if (value < 0 || unit == null) {
					this.completionCondition.await();
				} else {
					this.completionCondition.await(value, unit);
				}
			} finally {
				this.completionLock.unlock();
			}
			// When reaches here, re-acquire the execution lock, so either
			// the task has been completed or it has not yet been started,
			// or it has been cancelled.
			this.executionLock.lock();
			if (this.completed) return true;
			else return false;
		} finally {
			this.executionLock.unlock();
		}
	}

	@Override
	public boolean cancel() {
		// Try to acquire execution lock to set the cancelled status.
		// The acquiring will only succeed if the execution has not
		// yet began or has completed.
		final boolean succeeded = this.executionLock.tryLock();
		// If cannot lock, then execution has began.
		if (!succeeded) return false;
		else {
			try {
				// Acquire completion lock to check for completion status.
				this.completionLock.lock();
				try {
					if (this.completed) return false;
					// Otherwise, we can cancel.
					this.canceled = true;
					// Signal completion waiting.
					this.completionCondition.signalAll();
					return true;
				} finally {
					this.completionLock.unlock();
				}
			} finally {
				this.executionLock.unlock();
			}
		}
	}
}
