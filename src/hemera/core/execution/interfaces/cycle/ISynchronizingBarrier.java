package hemera.core.execution.interfaces.cycle;

import hemera.core.execution.interfaces.time.ITimeHandle;

/**
 * <code>ISynchronizingBarrier</code> defines interface
 * of a barrier unit that guarantees all the reaching
 * executors start the new execution cycle at the same
 * time, after every executor has finished its tasks
 * for the current execution cycle. If the defined cycle
 * time limit has not been reached when all executors
 * reached barrier, the barrier conserves computation
 * resources by putting all executors to sleep until
 * the time limit expires.
 * <p>
 * <code>ISynchronizingBarrier</code> can be used with
 * either a single executor instance or multiple
 * instances to ensure the execution cycle time setting
 * and the synchronized cycle start-up.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface ISynchronizingBarrier extends ICycle {
	
	/**
	 * Reach the barrier to notify that the current
	 * execution cycle of the reaching executor has
	 * been completed. This method guarantees all
	 * reaching executors start new execution cycle
	 * at the same time.
	 * <p>
	 * This method should only be invoked by internal
	 * executor threads. External invocations can cause
	 * system error.
	 */
	public void reach();
	
	/**
	 * Permanently break the barrier to allow all the
	 * current waiting and future reaching executors
	 * to pass directly.
	 * <p>
	 * This method should only be invoked by execution
	 * service internally. External invocations can
	 * cause system error.
	 * <p>
	 * This method provides memory visibility to all the
	 * accessing threads. It guarantees that all waiting
	 * threads are released. No synchronization locking
	 * is provided or needed.
	 */
	public void breakBarrier();
	
	/**
	 * Check if this barrier has been broken.
	 * @return <code>true</code> if this barrier has
	 * been broken. <code>false</code> otherwise.
	 */
	public boolean isBroken();
	
	/**
	 * Retrieve the time handle internally used by this
	 * synchronizing barrier to allow retrieval of time
	 * interpolation and rate values.
	 * @return The <code>ITimeHandle</code> instance.
	 */
	public ITimeHandle getTimeHandle();
}
