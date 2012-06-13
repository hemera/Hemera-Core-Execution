package hemera.core.execution;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import hemera.core.execution.interfaces.task.IEventTask;
import hemera.core.execution.interfaces.task.handle.IEventTaskHandle;

/**
 * <code>EventExecutable</code> defines implementation
 * of an executable unit that directly corresponds to
 * an event task unit. It implements the interface of
 * <code>IEventTaskHandle</code> to provide the event
 * task type specific handling.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
class EventExecutable extends Executable implements IEventTaskHandle {
	/**
	 * The <code>IEventTask</code> to be executed.
	 */
	private final IEventTask task;
	/**
	 * The execution waiting <code>Lock</code>.
	 */
	private final Lock lock;
	/**
	 * The execution waiting<code>Condition</code>.
	 */
	private final Condition condition;

	/**
	 * Constructor of <code>EventExecutable</code>.
	 */
	EventExecutable() {
		this.task = null;
		this.lock = new ReentrantLock();
		this.condition = this.lock.newCondition();
	}

	/**
	 * Constructor of <code>EventExecuable</code>.
	 * @param task The <code>IEventTask</code> to be
	 * executed.
	 */
	EventExecutable(final IEventTask task) {
		this.task = task;
		this.lock = new ReentrantLock();
		this.condition = this.lock.newCondition();
	}

	@Override
	void doExecute() throws Exception {
		try {
			this.task.execute();
		} finally {
			// Guarantees to wake up waiting threads.
			this.signalAll();
		}
	}

	/**
	 * Signal all waiting threads to wake up.
	 */
	void signalAll() {
		this.lock.lock();
		try {
			this.condition.signalAll();
		} finally {
			this.lock.unlock();
		}
	}

	@Override
	public boolean await() throws InterruptedException {
		return this.await(-1, null);
	}

	@Override
	public boolean await(final long value, final TimeUnit unit) throws InterruptedException {
		// Check finished/canceled to early return.
		if(this.hasFinished()) return true;
		else if(this.isCanceled()) return false;
		// Wait.
		boolean signaled = false;
		this.lock.lock();
		try {
			// Check flags in case signal was sent before this lock is obtained.
			if(this.hasFinished()) return true;
			else if(this.isCanceled()) return false;
			// Unconditional wait.
			if(value < 0 || unit == null) {
				this.condition.await();
				signaled = true;
			}
			// Timed wait.
			else {
				signaled = this.condition.await(value, unit);
			}
		} finally {
			this.lock.unlock();
		}
		// If signaled, condition return result.
		if(signaled) {
			if(this.isCanceled()) return false;
			else return true;
			// Otherwise, return false.
		} else return false;
	}

	@Override
	public boolean cancel() {
		final boolean canceled = super.cancel();
		// Wake up waiting threads.
		if (canceled) this.signalAll();
		return canceled;
	}
}
