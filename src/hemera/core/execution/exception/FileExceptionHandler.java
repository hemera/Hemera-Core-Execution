package hemera.core.execution.exception;

import hemera.core.execution.interfaces.exception.IExceptionHandler;
import hemera.core.utility.logging.FileLogger;

/**
 * <code>FileExceptionHandler</code> defines the
 * implementation of an exception handler that handles
 * the given <code>Exception</code> by logging stack-
 * trace of the exception to the standard error stream
 * and an external log file.
 * <p>
 * <code>FileExceptionHandler</code> utilizes the
 * <code>FileLogger</code> utility unit to perform the
 * logging process. It does not perform any procedures
 * during JVM shutdown as exception stack-trace data
 * is logged immediately as they are handled.
 * <p>
 * Since a <code>FileLogger</code> is used internally
 * for exception logging, the log directory of this
 * implementation depends on <code>ELoggingConfig</code>
 * <code>Directory</code> value.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public class FileExceptionHandler implements IExceptionHandler {

	@Override
	public void run() {}

	@Override
	public void handle(final Exception e) {
		FileLogger.getLogger(this.getClass()).exception(e);
	}
}
