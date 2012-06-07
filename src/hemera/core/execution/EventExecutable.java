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
	 * The result <code>Lock</code>.
	 */
	private final Lock lock;
	/**
	 * The result <code>Condition</code>.
	 */
	private final Condition condition;
	/**
	 * The <code>Boolean</code> finished flag.
	 * <p>
	 * This volatile field is the guard to ensure the
	 * happens-before relationship for the result data
	 * reference.
	 * <p>
	 * Since the logic execution thread, which sets this
	 * flag may be a different one from the external
	 * status checking thread, memory visibility needs to
	 * be guaranteed.
	 */
	private volatile boolean finished;
	
	/**
	 * Constructor of <code>EventExecutable</code>.
	 */
	protected EventExecutable() {
		this.task = null;
		this.lock = new ReentrantLock();
		this.condition = this.lock.newCondition();
	}
	
	/**
	 * Constructor of <code>EventExecuable</code>.
	 * @param task The <code>IEventTask</code> to be
	 * executed.
	 */
	public EventExecutable(final IEventTask task) {
		this.task = task;
		this.lock = new ReentrantLock();
		this.condition = this.lock.newCondition();
	}

	@Override
	protected void doExecute() throws Exception {
		// If has been finished, return.
		// Do not check has started here since parent
		// class guarantees only the owner thread can
		// invoke this method. Also the has started
		// flag is marked before this invocation. If
		// checking has started, the task will never
		// be executed.
		if (this.hasFinished()) return;
		try {
			this.task.execute();
		// Guarantees to set finished flag and wake up
		// waiting threads.
		} finally {
			this.doFinish();
		}
	}
	
	/**
	 * Set the finished status and wake up all waiting
	 * threads.
	 */
	protected final void doFinish() {
		this.finished = true;
		this.signalAll();
	}
	
	/**
	 * Signal all waiting threads to wake up.
	 */
	private void signalAll() {
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
		if(this.finished) return true;
		else if(this.canceled) return false;
		// Wait.
		boolean signaled = false;
		this.lock.lock();
		try {
			// Check flags in case signal was sent
			// before this lock is obtained.
			if(this.finished) return true;
			else if(this.canceled) return false;
			// Unconditional wait.
			if(value < 0 || unit == null) {
				this.condition.await();
				signaled = true;
			// Timed wait.
			} else {
				signaled = this.condition.await(value, unit);
			}
		} finally {
			this.lock.unlock();
		}
		// If signaled, condition return result.
		if(signaled) {
			if(this.canceled) return false;
			else return true;
		// Otherwise, return false.
		} else return false;
	}

	@Override
	public boolean executeAwait() throws Exception {
		// Try to execute if does not have an owner.
		// This guard is needed to prevent the case:
		// 1. ThreadA gets assigned with this executable.
		// 2. ThreadA executes this executable and sets
		//    itself as the owner thread.
		// 3. ThreadA invokes executeAwait.
		// 4. Since ThreadA was set as the owner during
		//    the previous normal execution, it will
		//    pass the owner thread test in execute method
		//    and executes the logic for the second time.
		if (!this.hasStarted()) {
			this.execute();
		}
		// Return result.
		return this.await();
	}

	@Override
	public boolean cancel() {
		if(this.hasStarted()) return false;
		else {
			this.canceled = true;
			// Wake up waiting threads.
			this.signalAll();
			return true;
		}
	}

	@Override
	public boolean hasStarted() {
		return (this.ownerThread.get() != null);
	}
	
	@Override
	public boolean hasFinished() {
		return this.finished;
	}
}
