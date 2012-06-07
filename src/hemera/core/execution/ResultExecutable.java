package hemera.core.execution;

import java.util.concurrent.TimeUnit;

import hemera.core.execution.interfaces.task.IResultTask;
import hemera.core.execution.interfaces.task.handle.IResultTaskHandle;

/**
 * <code>ResultExecutable</code> defines implementation
 * of an executable unit that directly corresponds to
 * a result task unit. It implements the interface of
 * <code>IResultTaskHandle</code> and extends the
 * implementation of <code>EventExecutable</code> to
 * provide the result task type specific handling.
 * <p>
 * @param <V> The result executable result return type.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
class ResultExecutable<V> extends EventExecutable implements IResultTaskHandle<V> {
	/**
	 * The <code>IResultTask</code> to be executed.
	 */
	private final IResultTask<V> task;
	/**
	 * The <code>V</code> execution result.
	 * <p>
	 * This reference does not need to be volatile,
	 * since it is always written before the volatile
	 * <code>finished</code> flag write and only read
	 * after reading the <code>finished</code> flag.
	 * The volatile <code>finished</code> flag ensures
	 * the happens-before relationship.
	 */
	private V result;

	/**
	 * Constructor of <code>ResultExecutable</code>.
	 * @param task The <code>IResultTask</code> to be executed.
	 */
	public ResultExecutable(final IResultTask<V> task) {
		super();
		this.task = task;
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
			this.result = this.task.execute();
		// Guarantees to set finished flag and wake up
		// waiting threads.
		} finally {
			this.doFinish();
		}
	}
	
	@Override
	public V executeAndGet() throws Exception {
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
		return this.getAndWait();
	}
	
	@Override
	public V getAndWait() throws InterruptedException {
		return this.getAndWait(-1, null);
	}
	
	@Override
	public V getAndWait(final long value, final TimeUnit unit) throws InterruptedException {
		if(this.await(value, unit)) return this.result;
		else return null;
	}
}
