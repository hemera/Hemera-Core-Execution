package hemera.core.execution.executable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
 * @param <R> The result executable result return type.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public class ResultExecutable<R> extends EventExecutable implements IResultTaskHandle<R> {
	/**
	 * The <code>IResultTask</code> to be executed.
	 */
	private final IResultTask<R> task;
	/**
	 * The <code>AtomicReference</code> of the task
	 * result.
	 */
	private final AtomicReference<R> result;

	/**
	 * Constructor of <code>ResultExecutable</code>.
	 * @param task The <code>IResultTask</code> to be executed.
	 */
	public ResultExecutable(final IResultTask<R> task) {
		super();
		this.task = task;
		this.result = new AtomicReference<R>(null);
	}

	@Override
	protected void doExecute() throws Exception {
		try {
			final R result = this.task.execute();
			this.result.compareAndSet(null, result);
		} finally {
			// Guarantees to wake up waiting threads.
			this.signalAll();
		}
	}
	
	@Override
	public R getAndWait() throws InterruptedException {
		return this.getAndWait(-1, null);
	}
	
	@Override
	public R getAndWait(final long value, final TimeUnit unit) throws InterruptedException {
		if(this.await(value, unit)) return this.result.get();
		else return null;
	}
}
