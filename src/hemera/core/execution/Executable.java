package hemera.core.execution;

import java.util.concurrent.atomic.AtomicReference;

/**
 * <code>Executable</code> defines the abstraction of
 * an executable unit that provides the commonly shared
 * functionality by all types of executables represent
 * all types of tasks.
 * <p>
 * <code>Executable</code> provides all the necessary
 * thread safety guarantees related to cancellation
 * and execution started flag. Make sure the internal
 * synchronization policy is fully understood before
 * overriding the implementations. No locking is
 * performed on cancellation and started flag, hardware
 * atomic operations are used.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
abstract class Executable implements IExecutable {
	/**
	 * The <code>AtomicReference</code> of the owner
	 * execution thread.
	 * <p>
	 * Since multiple executor threads may invoke logic
	 * execution at the same time, atomic check and set
	 * operation is required to guarantee that only a
	 * single invocation passes through.
	 * <p>
	 * Alternatively, an <code>AtomicBoolean</code> can
	 * be used to mark started flap. However, that will
	 * not support checking for the same executor thread
	 * for cyclic or retried executions. Making the task
	 * mark the boolean back to false at the end of each
	 * execution cycle is possible, though atomic write-
	 * operation is more expensive than read-operation,
	 * thus reference reading based check is used.
	 */
	protected final AtomicReference<Thread> ownerThread;
	/**
	 * The <code>Boolean</code> canceled flag.
	 * <p>
	 * Since the cancellation thread, which sets this flag
	 * may be a different thread from the concurrent result
	 * retrieval thread or the logic execution thread, the
	 * memory visibility needs to be guaranteed, in order
	 * for retrieval operations to early exit.
	 */
	protected volatile boolean canceled;

	/**
	 * Constructor of <code>Executable</code>.
	 */
	protected Executable() {
		this.ownerThread = new AtomicReference<Thread>(null);
		this.canceled = false;
	}

	@Override
	public final void execute() throws Exception {
		// If canceled, return.
		if (this.isCanceled()) return;
		// Check started thread.
		else {
			final Thread thread = Thread.currentThread();
			// If this is not the owner thread, try to
			// set as the owner thread.
			if (this.ownerThread.get() != thread) {
				// Only allow setting on the first try.
				final boolean succeeded = this.ownerThread.compareAndSet(null, thread);
				// Directly return if some other thread is
				// already set as the owner thread.
				if (!succeeded) return;
			}
			// Execute if this is the owner thread.
			// No else-block here, since cyclic execution
			// may be needed.
			this.doExecute();
		}
	}
	
	/**
	 * Execute the task logic.
	 * <p>
	 * It is guaranteed that this method is only invoked
	 * by a single owner thread. However, the same owner
	 * thread may invoke this method multiple times.
	 * <p>
	 * This method may provide the necessary duplicate
	 * invocation check such as checking if the task
	 * been finished in case of an event or result task
	 * to avoid multiple times of logic execution.
	 * @throws Exception if any processing failed.
	 */
	protected abstract void doExecute() throws Exception;

	@Override
	public boolean isCanceled() {
		return this.canceled;
	}
}
