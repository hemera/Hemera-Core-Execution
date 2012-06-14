package hemera.core.execution;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import hemera.core.execution.interfaces.IExceptionHandler;
import hemera.core.execution.interfaces.IExecutionService;
import hemera.core.execution.interfaces.task.IEventTask;
import hemera.core.execution.interfaces.task.IResultTask;
import hemera.core.execution.interfaces.task.handle.IEventTaskHandle;
import hemera.core.execution.interfaces.task.handle.IResultTaskHandle;
import hemera.core.utility.logging.FileLogger;

/**
 * <code>ExecutionService</code> defines abstraction
 * of the centralized threading management unit that
 * is responsible for task execution for the entire
 * system. It provides the commonly shared logic among
 * all types of execution services.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public abstract class ExecutionService implements IExecutionService {
	/**
	 * The <code>FileLogger</code> instance.
	 */
	protected final FileLogger logger;
	/**
	 * The <code>IExceptionHandler</code> instance used
	 * to gracefully allow executors handle exceptions.
	 */
	protected final IExceptionHandler handler;
	/**
	 * The <code>AtomicBoolean</code> activated flag.
	 * <p>
	 * Since concurrent modification invocations may
	 * occur, atomic compare and set operation must
	 * be provided.
	 */
	private final AtomicBoolean activated;
	/**
	 * The <code>AtomicBoolean</code> shutdown flag.
	 * <p>
	 * Since concurrent modification invocations may
	 * occur, atomic compare and set operation must
	 * be provided.
	 */
	private final AtomicBoolean shutdown;

	/**
	 * Constructor of <code>ExecutionService</code>.
	 * @param handler The <code>IExceptionHandler</code>
	 * instance.
	 */
	public ExecutionService(final IExceptionHandler handler) {
		if (handler == null) throw new IllegalArgumentException("Exception handler cannot be null.");
		this.logger = FileLogger.getLogger(this.getClass());
		this.handler = handler;
		this.activated = new AtomicBoolean(false);
		this.shutdown = new AtomicBoolean(false);
		// Add exception handler as system shutdown hook.
		Runtime.getRuntime().addShutdownHook(new Thread(handler));
	}
	
	@Override
	public void activate() {
		// Allow first invocation to pass.
		if (!this.activated.compareAndSet(false, true)) return;
		this.doActivate();
	}
	
	/**
	 * Perform the service type specific activation.
	 */
	protected abstract void doActivate();

	@Override
	public void shutdown() {
		if (!this.shutdown.compareAndSet(false, true)) return;
		this.doShutdown();
	}
	
	/**
	 * Perform service type specific graceful shutdown.
	 */
	protected abstract void doShutdown();
	
	@Override
	public void shutdownAndWait() throws InterruptedException {
		if (!this.shutdown.compareAndSet(false, true)) return;
		this.doShutdownAndWait();
	}
	
	/**
	 * Perform service type specific graceful shutdown
	 * and wait.
	 * @throws InterruptedException If waiting process
	 * is interrupted.
	 */
	protected abstract void doShutdownAndWait() throws InterruptedException;

	@Override
	public void forceShutdown() {
		if (!this.shutdown.compareAndSet(false, true)) return;
		this.doForceShutdown();
	}
	
	/**
	 * Perform service type specific force shutdown.
	 */
	protected abstract void doForceShutdown();

	@Override
	public void forceShutdown(final long time, final TimeUnit unit) throws InterruptedException {
		if (!this.shutdown.compareAndSet(false, true)) return;
		this.doForceShutdown(time, unit);
	}
	
	/**
	 * Perform service type specific graceful shutdown
	 * and wait then force shutdown.
	 * @param time The <code>Long</code> time value.
	 * @param unit The <code>TimeUnit</code> of the value.
	 * @throws InterruptedException If waiting process is
	 * interrupted.
	 */
	protected abstract void doForceShutdown(final long time, final TimeUnit unit) throws InterruptedException;

	@Override
	public IEventTaskHandle submit(final IEventTask task) {
		this.exceptionCheck(task);
		return this.doSubmit(task);
	}
	
	/**
	 * Perform the service type specific event task
	 * assignment.
	 * @param task The <code>IEventTask</code> to be
	 * submitted.
	 * @return The <code>IEventTaskHandle</code> instance.
	 */
	protected abstract IEventTaskHandle doSubmit(final IEventTask task);

	@Override
	public <V> IResultTaskHandle<V> submit(final IResultTask<V> task) {
		this.exceptionCheck(task);
		return this.doSubmit(task);
	}
	
	/**
	 * Perform the service type specific result task
	 * assignment.
	 * @param <V> The result task result return type.
	 * @param task The <code>IResultTask</code> to be
	 * submitted.
	 * @return The <code>IResultTaskHandle</code> instance.
	 */
	protected abstract <V> IResultTaskHandle<V> doSubmit(final IResultTask<V> task);
	
	/**
	 * Check all the exception causing status.
	 * @param <T> The task type.
	 * @param task The <code>T</code> task to be submitted.
	 */
	private <T> void exceptionCheck(final T task) {
		if (!this.activated.get()) throw new IllegalStateException("Service has not yet been activated.");
		else if (this.shutdown.get()) throw new IllegalStateException("Service has already been shutdown.");
		else if (task == null) throw new IllegalArgumentException("Task is null.");
	}
}
