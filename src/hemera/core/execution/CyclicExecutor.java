package hemera.core.execution;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import hemera.core.execution.cycle.NotifyingBarrier;
import hemera.core.execution.interfaces.ICyclicExecutor;
import hemera.core.execution.interfaces.IExecutionService;
import hemera.core.execution.interfaces.cycle.INotifyingBarrier;
import hemera.core.execution.interfaces.exception.IExceptionHandler;
import hemera.core.execution.interfaces.task.ICyclicTask;
import hemera.core.execution.interfaces.task.handle.ICyclicTaskHandle;

/**
 * <code>CyclicExecutor</code> defines implementation
 * of an executor unit that is only responsible for
 * the execution of cyclic tasks while making a best
 * attempt to maintain the desired execution rate.
 * <p>
 * <code>CyclicExecutor</code> is an internal structure
 * that should never be used outside the scope of the
 * execution service due to its dependency on various
 * internal implementations.
 * <p>
 * <code>CyclicExecutor</code> should be created in an
 * on-demand fashion when there is a new cyclic task
 * to assign. Once started, cyclic executor will
 * directly attempt to execute the assigned cyclic task
 * and block for the remaining time of the cycle. If
 * there is no task or the assigned task has been
 * canceled, the executor will try to recycle itself.
 * Small amount of eager instantiations are encouraged.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
class CyclicExecutor extends Executor implements ICyclicExecutor {
	/**
	 * The <code>IExecutionService</code> instance used
	 * to execute the observer notification event tasks.
	 */
	private final IExecutionService service;
	/**
	 * The <code>IExecutorBin</code> used to recycle
	 * this executor.
	 */
	private final IExecutorBin recycleBin;
	/**
	 * The <code>AtomicReference</code> of the next
	 * <code>ExecutablePair</code> to be executed.
	 * <p>
	 * Since concurrent assignments may occur, atomic
	 * check and set operation is needed to guarantee
	 * only a single assignment succeeds.
	 */
	private final AtomicReference<ExecutablePair> pair;

	/**
	 * Constructor of <code>CyclicExecutor</code>.
	 * @param name The <code>String</code> name of this
	 * executor thread.
	 * @param handler The <code>IExceptionHandler</code>
	 * used for task execution graceful exception handling.
	 * @param service The <code>IExecutionService</code>
	 * instance used to execute the observer notification
	 * event tasks.
	 * @param recycleBin The <code>IExecutorBin</code>
	 * used to recycle this executor.
	 */
	public CyclicExecutor(final String name, final IExceptionHandler handler, final IExecutionService service, final IExecutorBin recycleBin) {
		super(name, handler);
		this.service = service;
		this.recycleBin = recycleBin;
		this.pair = new AtomicReference<ExecutablePair>(null);
	}

	@Override
	public void run() {
		try {
			// Need to check terminated flag in case of an
			// external termination such as system shutdown.
			while(!this.terminated) {
				try {
					final ExecutablePair next = this.pair.get();
					// Check null.
					if(next == null) {
						if(!this.recycleBin.recycle(this)) break;
						// Check cancellation.
					} else if(next.executable.isCanceled()) {
						this.pair.set(null);
						if(!this.recycleBin.recycle(this)) break;
						// Execute and reach for barrier. Also
						// gracefully handle any exceptions.
					} else if(!this.terminated) {
						next.executable.execute();
						next.barrier.reach();
					}
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
	public ICyclicTaskHandle assign(final ICyclicTask task) {
		// Check for termination.
		if(this.terminated) return null;
		// Check and set.
		final ExecutablePair pair = new ExecutablePair(task);
		final boolean succeeded = this.pair.compareAndSet(null, pair);
		// If succeeded, set time values.
		if(succeeded) {
			task.setTimeHandle(pair.barrier.getTimeHandle());
			// Tolerate initial time limit set.
			final long limit = task.getCycleLimit(TimeUnit.NANOSECONDS);
			pair.barrier.setTimeLimit((limit>0 ? limit : 0), TimeUnit.NANOSECONDS);
		}
		// Return conditionally.
		return succeeded ? pair.executable : null;
	}

	@Override
	public void terminate() throws Exception {
		super.terminate();
		final ExecutablePair next = this.pair.get();
		if(next == null) return;
		next.barrier.breakBarrier();
		next.executable.cancel();
	}

	/**
	 * <code>ExecutablePair</code> defines the internal
	 * data structure used to maintain a pair instance
	 * of <code>CyclicExecutable</code> and its matching
	 * <code>INotifyingBarrier</code>.
	 *
	 * @author Yi Wang (Neakor)
	 * @version Creation date: 12-08-2009 10:07 EST
	 * @version Modified date: 12-08-2009 10:10 EST
	 */
	private class ExecutablePair {
		/**
		 * The <code>CyclicExecutable</code> instance.
		 */
		private final CyclicExecutable executable;
		/**
		 * The <code>INotifyingBarrier</code> instance.
		 */
		private final INotifyingBarrier barrier;

		/**
		 * Constructor of <code>ExecutablePair</code>.
		 * @param task The <code>ICyclicTask</code> unit.
		 */
		private ExecutablePair(final ICyclicTask task) {
			this.barrier = new NotifyingBarrier(1, service, handler);
			this.executable = new CyclicExecutable(task, this.barrier);
		}
	}
}
