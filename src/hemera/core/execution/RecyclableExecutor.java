package hemera.core.execution;

import java.util.concurrent.atomic.AtomicReference;

import hemera.core.execution.interfaces.IRecyclableExecutor;
import hemera.core.execution.interfaces.exception.IExceptionHandler;
import hemera.core.execution.interfaces.task.IEventTask;
import hemera.core.execution.interfaces.task.IResultTask;
import hemera.core.execution.interfaces.task.handle.IEventTaskHandle;
import hemera.core.execution.interfaces.task.handle.IResultTaskHandle;

/**
 * <code>RecyclableExecutor</code> defines implementation
 * of a recyclable executor that should only be used
 * for background event or result task execution. It
 * is constructed with <code>IExecutorBin</code> to
 * allow recycling.
 * <p>
 * <code>RecyclableExecutor</code> is an internal class
 * that should never be used outside the scope of the
 * execution service due to its dependencies on various
 * internal structures.
 * <p>
 * <code>RecyclableExecutor</code> should be created in
 * an on-demand fashion when there is a new background
 * event or result task to assign. Once the executor is
 * started, it will directly attempt to execute its
 * internal task reference and recycle itself once
 * completed. A small amount of eager instantiations are
 * encouraged.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
class RecyclableExecutor extends Executor implements IRecyclableExecutor {
	/**
	 * The <code>IExecutorBin</code> used to recycle
	 * this executor.
	 */
	private final IExecutorBin recycleBin;
	/**
	 * The <code>AtomicReference</code> of the next
	 * background <code>EventExecutable</code> to be
	 * executed.
	 * <p>
	 * Since concurrent assignments may occur, atomic
	 * check and set operation is needed to guarantee
	 * only a single assignment succeeds.
	 */
	private final AtomicReference<EventExecutable> executable;

	/**
	 * Constructor of <code>RecyclableExecutor</code>.
	 * @param name The <code>String</code> name of this
	 * executor thread.
	 * @param handler The <code>IExceptionHandler</code>
	 * used for task execution graceful exception handling.
	 * @param recycleBin The <code>IExecutorBin</code>
	 * used to recycle this executor.
	 */
	public RecyclableExecutor(final String name, final IExceptionHandler handler, final IExecutorBin recycleBin) {
		super(name, handler);
		this.recycleBin = recycleBin;
		this.executable = new AtomicReference<EventExecutable>(null);
	}

	@Override
	public void run() {
		try {
			// Need to check terminated flag in case of an
			// external termination such as system shutdown.
			while(!this.terminated) {
				try {
					// Get and set reference to null.
					final EventExecutable next = this.executable.getAndSet(null);
					// Execute and gracefully handle exception.
					if(next != null) {
						next.execute();
					}
					// Try recycle.
					if(!this.recycleBin.recycle(this)) break;
				// Catch any runtime exceptions with exception handler.
				} catch (final Exception e) {
					this.handler.handle(e);
				}
			}
		} finally {
			this.threadTerminated = true;
		}
	}

	@Override
	public IEventTaskHandle assign(final IEventTask task) {
		// Check for termination early to avoid object
		// construction.
		if(this.terminated) return null;
		// Perform assignment.
		final EventExecutable executable = new EventExecutable(task);
		return this.doAssign(executable);
	}

	@Override
	public <V> IResultTaskHandle<V> assign(final IResultTask<V> task) {
		// Check for termination early to avoid object
		// construction.
		if(this.terminated) return null;
		// Perform assignment.
		final ResultExecutable<V> executable = new ResultExecutable<V>(task);
		return this.doAssign(executable);
	}

	/**
	 * Perform the atomic assignment operation and return
	 * executable conditionally.
	 * @param <E> The <code>EventExecutable</code> type.
	 * @param executable The <code>E</code> to be assigned.
	 * @return The given <code>E</code> executable if
	 * assignment succeeded. <code>null</code> otherwise.
	 */
	private <E extends EventExecutable> E doAssign(final E executable) {
		// Check and set then conditional return.
		final boolean succeeded = this.executable.compareAndSet(null, executable);
		return succeeded ? executable : null;
	}
}
