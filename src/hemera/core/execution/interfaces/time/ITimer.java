package hemera.core.execution.interfaces.time;

import java.util.concurrent.TimeUnit;

/**
 * <code>ITimer</code> defines interface of an utility
 * unit that provides the actual time related maintenance
 * functionalities. It extends <code>ITimeHandle</code>
 * to support all time value retrieval functionalities.
 * <p>
 * Typically there exist a single instance of timer unit
 * within an application runtime. It is maintained and
 * updated by the executor context every execution cycle
 * to guarantee accurate and synchronized time values.
 * <p>
 * <code>ITimer</code> needs to be updated during each
 * execution cycle to properly track time and calculate
 * various time values. The implementation should apply
 * the proper synchronization mechanisms to all methods
 * in order to guarantee the timer value consistency.
 * <p>
 * <code>ITimer</code> performs synchronization locking
 * on all the maintenance methods to guarantee thread
 * safety. There is no restriction on which thread
 * performs the actual update and reset invocations.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface ITimer extends ITimeHandle {

	/**
	 * Update the timer.
	 * <p>
	 * This method is guarded by the timer lock that also
	 * guards the reset method.
	 * <p>
	 * Within the runtime system, this method should only
	 * be invoked by the <code>CycleBarrier</code>. User-
	 * level invocations can cause inconsistent states.
	 */
	public void update();
	
	/**
	 * Offset the time value by the given value in given
	 * time unit. In other words, set the time values
	 * such that it appears the timer was last updated
	 * at the last update time plus the offset.
	 * <p>
	 * For instance, if the timer was last updated at
	 * time t. After offset the timer with value f,
	 * the timer will become as if it was last updated
	 * at time (t+f).
	 * @param value The <code>Long</code> value.
	 * @param unit The <code>TimeUnit</code> of value.
	 */
	public void offset(final long value, final TimeUnit unit);
	
	/**
	 * Reset the timer.
	 * <p>
	 * This method is guarded by the timer lock that also
	 * guards the update method.
	 */
	public void reset();
}
