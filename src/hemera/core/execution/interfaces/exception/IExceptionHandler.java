package hemera.core.execution.interfaces.exception;

/**
 * <code>IExceptionHandler</code> defines a centralized
 * exception handling unit that is by default set to
 * all available executor threads to allow the runtime
 * exceptions to be handled gracefully without crashing
 * an executor thread or the application.
 * <p>
 * <code>IExceptionHandler</code> does not re-throw the
 * given <code>Exception</code> as that would cause the
 * executor threads or the application system to crash.
 * It typically should buffer the exception stack trace
 * and its messages immediately when an exception is
 * submitted and later log them to an external file when
 * it is convenient to do so.
 * <p>
 * <code>IExceptionHandler</code> is an extension of
 * <code>Runnable</code> to allow the execution service
 * to submit the handler as a shutdown hook to the JVM
 * runtime. This allows exception handler to perform
 * any necessary exception finalization before the JVM
 * shuts down.
 * <p>
 * Once an <code>IExceptionHandler</code> is submitted to
 * an <code>IExecutorService</code>, it is automatically
 * added as a system shutdown hook. The same instance of
 * <code>IExceptionHandler</code> is also passed onto all
 * executors to allow handling of exceptions.
 * <p>
 * <code>IExceptionHandler</code> is required to provide
 * the necessary thread safety guarantees, since the unit
 * is likely to be invoked by multiple executor threads
 * to handle various exceptions simultaneously.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface IExceptionHandler extends Runnable {

	/**
	 * Handle the given exception gracefully.
	 * <p>
	 * Typically this method should simply buffer the given
	 * exception and return quickly to allow the execution
	 * thread to resume its normal task.
	 * @param e The <code>Exception</code> to be handled.
	 */
	public void handle(final Exception e);
}
