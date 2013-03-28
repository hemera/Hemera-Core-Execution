package hemera.core.execution;

import hemera.core.execution.interfaces.IServiceListener;
import hemera.core.utility.logging.FileLogger;

/**
 * <code>AbstractServiceListener</code> defines the base
 * abstraction of all service listeners. It provides the
 * basic logging functionality for when a service event
 * occurs.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public abstract class AbstractServiceListener implements IServiceListener {
	/**
	 * The <code>ExecutionService</code> instance.
	 */
	private ExecutionService servcie;

	@Override
	public final void capacityReached() {
		final StringBuilder builder = new StringBuilder();
		final Iterable<Executor> executors = this.servcie.getExecutors();
		for (final Executor executor : executors) {
			builder.append((executor.getName())).append("\n");
			builder.append(FileLogger.buildStacktrace(executor.thread.getStackTrace())).append("\n");
		}
		this.capacityReached(builder.toString());
	}
	
	/**
	 * Notify that the service has reached its maximum
	 * capacity.
	 * @param stacktrace The current stack track of all
	 * executor threads.
	 */
	protected abstract void capacityReached(final String stacktrace);
	
	/**
	 * Set the execution service that owns this listener.
	 * @param service The <code>ExecutionService</code>.
	 */
	void setExecutionService(final ExecutionService service) {
		this.servcie = service;
	}
}
