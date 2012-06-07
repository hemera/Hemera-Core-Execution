package hemera.core.execution.interfaces.cycle;

import java.util.concurrent.TimeUnit;

/**
 * <code>ICycle</code> defines the interface of the
 * abstraction of an execution cycle. It provides the
 * very basic definition of an execution cycle. More
 * specifically, it defines the cycle execution time
 * limit value.
 * <p>
 * The cycle execution time limit is set as a rough
 * approximation where the actual execution time per
 * cycle may differ. However, implementations should
 * make a best attempt to ensure the time limit is met.
 * <p>
 * <code>ICycle</code> guarantees the thread-safety on
 * all provided methods. The sub-interfaces should also
 * guarantee the thread-safety of additional methods
 * provided. The cycle is meant to be used in a highly
 * concurrent environment.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface ICycle {

	/**
	 * Set the execution cycle time limit in given time
	 * unit.
	 * @param value The <code>Long</code> limit value.
	 * @param unit The <code>TimeUnit</code> the value
	 * is in.
	 */
	public void setTimeLimit(final long value, final TimeUnit unit);
	
	/**
	 * Retrieve the execution cycle time limit in given
	 * time unit. If the returned value is less than or
	 * equal to 0, the cycle will be executed as fast as
	 * the execution of logic can be.
	 * @param unit The <code>TimeUnit</code> the value
	 * returned should be in.
	 * @return The <code>Long</code> value in given
	 * time unit.
	 */
	public long getTimeLimit(final TimeUnit unit);
}
