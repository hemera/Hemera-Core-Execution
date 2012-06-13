package hemera.core.execution;

import java.util.concurrent.atomic.AtomicBoolean;

import hemera.core.execution.interfaces.IExceptionHandler;
import hemera.core.execution.interfaces.IExecutor;
import hemera.core.execution.interfaces.task.IEventTask;
import hemera.core.execution.interfaces.task.IResultTask;
import hemera.core.execution.interfaces.task.handle.IEventTaskHandle;
import hemera.core.execution.interfaces.task.handle.IResultTaskHandle;

/**
 * <code>Executor</code> defines the abstraction of an
 * unit that is responsible for executing the assigned
 * tasks within its own internal thread. This abstraction
 * provides all the necessary implementation guarantees
 * defined by the <code>IExecutor</code> interface.
 * <p>
 * This abstraction defines the thread running cycle
 * of an executor. Subclasses only need to implement a
 * single running cycle logic without worrying about
 * the activation, termination or exception handling
 * issues.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
abstract class Executor implements IExecutor {
	/**
	 * The executing <code>Thread</code> of this
	 * executor.
	 */
	private final Thread thread;
	/**
	 * The <code>IExceptionHandler</code> used for
	 * task execution graceful exception handling.
	 */
	final IExceptionHandler handler;
	/**
	 * The <code>AtomicBoolean</code> executor thread
	 * started flag.
	 * <p>
	 * Since multiple threads can potentially invoke
	 * <code>start</code> method concurrently, atomic
	 * check and set operations are required to ensure
	 * that only a single invocation passes.
	 */
	private final AtomicBoolean started;
	/**
	 * The <code>Boolean</code> executor termination
	 * flag.
	 * <p>
	 * Since termination invocation can potentially be
	 * in a different thread than the executing thread,
	 * memory visibility of this flag needs to be
	 * guaranteed.
	 */
	volatile boolean terminated;
	/**
	 * The <code>Boolean</code> executor thread terminated
	 * flag.
	 * <p>
	 * All implementation executors should guarantee
	 * to write this flag to <code>true</code> upon
	 * exiting thread <code>run</code> method. This
	 * flag is read by the <code>ExecutionService</code>
	 * <code>shutdownAndWait</code> method.
	 * <p>
	 * Since write-thread, which is the executor thread
	 * is different from the read-thread thread, memory
	 * visibility of this flag needs to be guaranteed.
	 */
	private volatile boolean threadTerminated;
	
	/**
	 * Constructor of <code>Executor</code>.
	 * @param name The <code>String</code> name of this
	 * executor thread.
	 * @param handler The <code>IExceptionHandler</code>
	 * used for task execution graceful exception handling.
	 */
	Executor(final String name, final IExceptionHandler handler) {
		this.thread = new Thread(this);
		this.thread.setName(name);
		this.handler = handler;
		this.started = new AtomicBoolean(false);
		this.terminated = false;
		this.threadTerminated = false;
	}
	
	@Override
	public final void run() {
		try {
			while (!this.terminated) {
				try {
					this.doRun();
				// Catch any runtime exceptions with exception handler.
				} catch (final Exception e) {
					this.handler.handle(e);
				}
			}
		} finally {
			this.threadTerminated = true;
		}
	}
	
	/**
	 * Perform the actual executor running logic in a
	 * single execution cycle.
	 * @throws Exception If any execution failed. This
	 * exception does not cause thread termination. It
	 * is gracefully handled by the exeception handler.
	 */
	abstract void doRun() throws Exception;

	@Override
	public void start() {
		if(!this.started.compareAndSet(false, true)) return;
		this.thread.start();
	}

	@Override
	public void terminate() throws Exception {
		this.terminated = true;
	}
	
	@Override
	public void forceTerminate() throws Exception {
		this.terminate();
		this.thread.interrupt();
	}

	@Override
	public final IEventTaskHandle assign(final IEventTask task) {
		// Check for termination early to avoid object construction.
		if (this.terminated) return null;
		// Perform assignment.
		final EventExecutable executable = new EventExecutable(task);
		this.doAssign(executable);
		return executable;
	}

	@Override
	public final <V> IResultTaskHandle<V> assign(final IResultTask<V> task) {
		// Check for termination early to avoid object construction.
		if (this.terminated) return null;
		// Perform assignment.
		final ResultExecutable<V> executable = new ResultExecutable<V>(task);
		this.doAssign(executable);
		return executable;
	}
	
	/**
	 * Perform the assignment of given executable.
	 * @param <E> The <code>Executable</code> type.
	 * @param executable The <code>E</code> to be
	 * assigned.
	 */
	abstract <E extends Executable> void doAssign(final E executable);

	@Override
	public boolean hasStarted() {
		return this.started.get();
	}

	@Override
	public boolean hasTerminated() {
		return this.threadTerminated;
	}
}
