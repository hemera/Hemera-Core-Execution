package hemera.core.execution.interfaces.task;

import java.util.concurrent.TimeUnit;

import hemera.core.execution.interfaces.time.ITimeHandle;

/**
 * <code>ICyclicTask</code> defines the interface of an
 * atomic unit of logic that requires to be executed for
 * every execution cycle or at least many execution
 * cycles in a dedicated cyclic task executor thread.
 * The execution thread of a cyclic task does not change
 * over time, i.e. the logic of a cyclic task is always
 * executed by the same execution thread. A cyclic task
 * only ceases execution due to system shutdown or
 * external cancellation via its corresponding instance
 * of <code>ICyclicTaskHandle</code>.
 * <p>
 * <code>ICyclicTask</code> is provided as a means to
 * allow efficient cyclic task execution without
 * introducing the re-submission overhead. It is very
 * important that the task logic does not contain any
 * long looped executions. The looping mechanism is
 * internally provided and managed by the executor and
 * execution service. Implementation is only required
 * to provide a single execution cycle logic, which
 * will be repeatedly executed for many or every single
 * execution cycle.
 * <p>
 * <code>ICyclicTask</code> unlike event tasks, are not
 * distinguished as foreground or background tasks. All
 * cyclic tasks are treated in the same way in terms of
 * execution procedure. There is no particular blocking
 * operation restrictions on cyclic task logic.
 * <p>
 * <code>ICyclicTask</code> may be canceled with the
 * <code>ICyclicTaskHandle</code>. The handle is
 * returned upon success of submission of the cyclic
 * task.
 * <p>
 * <code>ICyclicTask</code> is injected with a time
 * handle unit before the very first logic execution
 * occurs. This process is performed immediately after
 * an executor is assigned with the cyclic task. The
 * time handle allows the task logic to retrieve time
 * interpolation value of execution cycles that may be
 * useful in some cases.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface ICyclicTask {

	/**
	 * Execute the cyclic task logic.
	 * <p>
	 * This method is guaranteed to be invoke by a single
	 * dedicated executor thread during the entire life-
	 * time of this task.
	 */
	public void execute();
	
	/**
	 * Signal this cyclic task to gracefully shutdown
	 * its internal logic due to execution service being
	 * shut down.
	 */
	public void shutdown();
	
	/**
	 * Inject the time handle unit into this cyclic task.
	 * <p>
	 * This method is guaranteed to be invoked before the
	 * very first execution occurs. This allows the cyclic
	 * task logic to properly store and setup the given
	 * time handle if necessary.
	 * @param handle The <code>ITimeHandle</code> unit.
	 */
	public void setTimeHandle(final ITimeHandle handle);
	
	/**
	 * Retrieve the initial maximum allowed execution
	 * time in between two consecutive executions.
	 * This value reflects the desired execution rate
	 * of the task. The rate is defined as the inverse
	 * of this value.
	 * <p>
	 * If the returned value is less than or equal to
	 * 0, the cyclic task will be executed as fast as
	 * the execution of logic can be.
	 * <p>
	 * This value may be modified after submission via
	 * the returned <code>ICyclicTaskHandle</code> unit.
	 * @param unit The <code>TimeUnit</code> returned
	 * value is in.
	 * @return The <code>Long</code> cycle time limit in
	 * given time unit.
	 */
	public long getCycleLimit(final TimeUnit unit);
}
