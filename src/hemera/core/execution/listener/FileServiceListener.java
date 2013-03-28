package hemera.core.execution.listener;

import java.util.concurrent.TimeUnit;

import hemera.core.execution.AbstractServiceListener;
import hemera.core.utility.logging.FileLogger;

/**
 * <code>FileServiceListener</code> defines the
 * implementation of an execution service listener
 * instance that logs the received critical events
 * using <code>FileLogger</code> every 10 seconds
 * at the warning logging level.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public class FileServiceListener extends AbstractServiceListener {
	/**
	 * The <code>FileLogger</code> instance.
	 */
	private final FileLogger logger;
	
	/**
	 * Constructor of <code>FileServiceListener</code>.
	 */
	public FileServiceListener() {
		this.logger = FileLogger.getLogger(this.getClass());
	}

	@Override
	protected void capacityReached(final String stacktrace) {
		this.logger.warning("Execution service maximum processing capacity reached!");
		this.logger.warning(stacktrace);
	}

	@Override
	public long getFrequency(final TimeUnit unit) {
		return unit.convert(10, TimeUnit.SECONDS);
	}
}
