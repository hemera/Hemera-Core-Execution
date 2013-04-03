package hemera.core.execution.executable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import hemera.core.execution.interfaces.IExceptionHandler;
import hemera.core.execution.interfaces.task.ICyclicTask;
import hemera.core.execution.interfaces.task.handle.ICyclicTaskHandle;

/**
 * <code>CyclicExecutable</code> defines a composite
 * container unit of a cyclic task, as well as being
 * the task handle for the contained task.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.2
 */
public class CyclicExecutable extends EventExecutable implements ICyclicTaskHandle {
	/**
	 * The <code>ICyclicTask</code> instance.
	 */
	private final ICyclicTask task;
	/**
	 * The <code>IExceptionHandler</code> used by the
	 * execution service.
	 */
	private final IExceptionHandler handler;
	/**
	 * The execution cycle waiting <code>Lock</code>.
	 */
	private final Lock waitLock;
	/**
	 * The execution cycle waiting <code>Condition</code>.
	 */
	private final Condition waitCondition;
	/**
	 * The <code>boolean</code> terminated flag.
	 * <p>
	 * The memory visibility of this flag needs to be
	 * guaranteed since it can be read and written in
	 * different threads.
	 */
	private volatile boolean terminated;
	
	/**
	 * Constructor of <code>CyclicExecutable</code>.
	 * @param task The <code>ICyclicTask</code> to be
	 * executed.
	 * @param handler The <code>IExceptionHandler</code>
	 * used by the execution service.
	 */
	public CyclicExecutable(final ICyclicTask task, final IExceptionHandler handler) {
		super();
		this.task = task;
		this.handler = handler;
		this.waitLock = new ReentrantLock();
		this.waitCondition = this.waitLock.newCondition();
		this.terminated = false;
	}
	
	@Override
	protected void executeTask() throws Exception {
		final long maxDuration = this.task.getCycleLimit(TimeUnit.NANOSECONDS);
		// Execute until it should terminate.
		int count = 0;
		while (!this.terminated) {
			// Record execution duration.
			final long start = System.nanoTime();
			// Execute.
			try {
				final boolean shouldContinue = this.task.execute();
				if (!shouldContinue) this.terminate();
			} catch (Exception e) {
				this.handler.handle(e);
			}
			// Increment cycle count.
			count++;
			// Check if we should terminate.
			final boolean shouldTerminate = this.shouldTerminate(count);
			if (shouldTerminate) break;
			// Otherwise try to wait for the remaining of the cycle.
			else {
				final long end = System.nanoTime();
				final long remaining = maxDuration - (end-start);
				if (remaining > 0) {
					this.waitLock.lock();
					try {
						// Double check terminated status before entering wait while holding lock.
						if (!this.terminated) {
							this.waitCondition.awaitNanos(remaining);
						}
					} catch (final InterruptedException e) {
						this.handler.handle(e);
					} finally {
						this.waitLock.unlock();
					}
				}
			}
		}
		// Clean up.
		this.task.cleanup();
	}
	
	/**
	 * Check if the task cyclic execution should be
	 * terminated.
	 * @param count The <code>int</code> current
	 * number of cycles that have been completed.
	 * @return <code>true</code> if the execution
	 * should be terminated. <code>false</code> if
	 * the execution should move onto the next
	 * cycle.
	 */
	private boolean shouldTerminate(final int count) {
		final int cycleCount = this.task.getCycleCount();
		if (cycleCount <= 0) return this.terminated;
		else return (this.terminated || (count >= cycleCount));
	}
	
	@Override
	public void terminate() {
		this.terminated = true;
		try {
			this.task.signalTerminate();
		} catch (final Exception e) {
			this.handler.handle(e);
		}
		// Signal waiting.
		this.waitLock.lock();
		try {
			this.waitCondition.signalAll();
		} finally {
			this.waitLock.unlock();
		}
	}

	/**
	 * Retrieve the cyclic task this executable contains.
	 * @return The <code>ICyclicTask</code> instance.
	 */
	public ICyclicTask getTask() {
		return this.task;
	}
}
