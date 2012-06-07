package hemera.core.execution.interfaces.time;

import java.util.concurrent.TimeUnit;

/**
 * <code>ITimeHandle</code> defines the interface of an
 * utility handle unit that allows retrieval of execution
 * cycle time related values in the defined time unit.
 * <p>
 * User defined time handle can also be constructed and
 * maintained by other means for other purposes. For such
 * usage, please see <code>ITimer</code> for detailed
 * maintenance documentation.
 * <p>
 * All retrieval operations provided by the time handle
 * unit are thread safe. Returned values are guaranteed
 * to reflect the most up to date version of updated and
 * calculated values. There is no synchronization locking
 * performed on any of the retrieval operations, only
 * memory consistency synchronization is used to allow
 * maximum retrieval concurrency.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface ITimeHandle {
	
	/**
	 * Retrieve the time unit.
	 * @return The <code>TimeUnit</code> enumeration.
	 */
	public TimeUnit getUnit();

	/**
	 * Retrieve the update rate per second. This value
	 * can be used as execution rate.
	 * @return The <code>Float</code> update rate per
	 * second.
	 */
	public float getUpdateRate();

	/**
	 * Retrieve the update interpolation value that
	 * represents the time between the last update and
	 * the new update in the time unit.
	 * @return The <code>Long</code> update interpolation.
	 */
	public long getInterpolation();
}
