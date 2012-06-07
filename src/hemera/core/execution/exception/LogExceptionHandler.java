package hemera.core.execution.exception;

import java.util.logging.Logger;

import hemera.core.execution.interfaces.exception.IExceptionHandler;

/**
 * <code>LogExceptionHandler</code> defines a handler
 * unit that handles given <code>Exception</code> by
 * directly logging the exception stack-trace using a
 * <code>Logger</code> to standard error stream.
 * <p>
 * <code>LogExceptionHandler</code> does not perform
 * any operations during JVM shutdown as a shutdown-
 * hook. It provides its thread-safety by delegating
 * the operation to the <code>Logger</code> instance.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public final class LogExceptionHandler implements IExceptionHandler {
	/**
	 * The <code>Logger</code> instance.
	 */
	private final Logger logger;
	
	/**
	 * Constructor of <code>LogExceptionHandler</code>.
	 */
	public LogExceptionHandler() {
		this.logger = Logger.getLogger(LogExceptionHandler.class.getName());
	}

	@Override
	public void handle(final Exception e) {
		final String log = this.buildLog(e);
		this.logger.severe(log);
	}
	
	/**
	 * Build the log string from the given exception.
	 * @param exception The <code>Exception</code> to
	 * built from.
	 * @return The <code>String</code> log.
	 */
	private String buildLog(final Exception exception) {
		final StringBuilder builder = new StringBuilder();
		builder.append(exception.toString()).append("\n");
		for(StackTraceElement e : exception.getStackTrace()) {
			builder.append("		at ").append(e.toString()).append("\n");
		}
		return builder.toString();
	}

	@Override
	public void run() {}
}
