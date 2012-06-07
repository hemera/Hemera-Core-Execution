package hemera.core.execution.time;

import java.util.concurrent.TimeUnit;

/**
 * <code>NanoTimer</code> defines the implementation
 * of a timer unit that uses nanosecond as its base
 * time unit. It extends <code>AbstractTimer</code>
 * to inherit the thread safe time value calculation
 * implementation.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public class NanoTimer extends AbstractTimer {
	
	/**
	 * Constructor of <code>NanoTimer</code>.
	 */
	public NanoTimer() {}

	@Override
	protected long getCurrent() {
		return System.nanoTime();
	}

	@Override
	public TimeUnit getUnit() {
		return TimeUnit.NANOSECONDS;
	}
}
