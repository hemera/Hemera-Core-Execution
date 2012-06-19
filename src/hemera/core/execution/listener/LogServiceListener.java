package hemera.core.execution.listener;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import hemera.core.execution.exception.LogExceptionHandler;
import hemera.core.execution.interfaces.IServiceListener;

/**
 * <code>LogServiceListener</code> defines the
 * implementation of an execution service listener
 * instance that logs the received critical events
 * using <code>Logger</code> to the standard output
 * every 10 seconds at the warning logging level.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public class LogServiceListener implements IServiceListener {
	/**
	 * The <code>Logger</code> instance.
	 */
	private final Logger logger;
	
	/**
	 * Constructor of <code>LogServiceListener</code>.
	 */
	public LogServiceListener() {
		this.logger = Logger.getLogger(LogExceptionHandler.class.getName());
	}

	@Override
	public void capacityReached() {
		this.logger.warning("Execution service maximum processing capacity reached!");
	}

	@Override
	public long getFrequency(final TimeUnit unit) {
		return unit.convert(10, TimeUnit.SECONDS);
	}
}
