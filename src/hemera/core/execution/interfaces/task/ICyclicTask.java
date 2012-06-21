package hemera.core.execution.interfaces.task;

import java.util.concurrent.TimeUnit;

/**
 * <code>ICyclicTask</code> defines the interface of an
 * atomic unit of logic that requires to be executed in
 * a loop for many cycles. The execution thread of a
 * cyclic task does not change over time, i.e. the logic
 * of a cyclic task is always executed by the same
 * execution thread. A cyclic task only stops execution
 * due to either system shutdown or external termination
 * via its corresponding <code>ICyclicTaskHandle</code>.
 * <p>
 * <code>ICyclicTask</code> is provided as a means to
 * allow efficient cyclic task execution without having
 * to re-submit the same task or maintain the execution
 * loop elsewhere. The looping mechanism is internally
 * provided and managed by the executor and execution
 * service. Implementation is only required to provide a
 * single execution cycle logic, which will be repeatedly
 * executed for many cycles.
 * <p>
 * <code>ICyclicTask</code> defines a maximum frequency
 * at which the task should be executed. This frequency
 * determines how many times the task will be attempted
 * to be executed at the most. This value only serves
 * as the maximum limit, since the task execution time
 * may vary, so the actual execution frequency may be
 * lower than the defined one, but will never be higher.
 * The executor will attempt to evenly distributed the
 * number of cycles over a second. So that if a cycle
 * consumes less time than the evenly divided amount,
 * the executor will wait for the remaining time of the
 * cycle before starting the next one. However, if a
 * cycle consumes more time, then the next cycle will
 * start immediately after the previous completes.
 * <p>
 * <code>ICyclicTask</code> may define a cycle count,
 * which determines how many times the task should be
 * executed before terminating. This is provided as an
 * automatic termination mechanism, such that the task
 * cyclic execution stops after the defined number of
 * cycles have been executed.
 * <p>
 * <code>ICyclicTask</code> may be terminated with the
 * <code>ICyclicTaskHandle</code>. The handle is
 * returned upon success of submission of the cyclic
 * task. When termination is requested by the handle,
 * the task <code>signalTerminate</code> is invoked
 * to allow the task to wake up the execution logic if
 * it is blocked. This allows the execution logic to
 * contain blocking operations, but still allow the
 * task to terminate when requested.
 * <p>
 * If an exception occurs during an execution cycle, the
 * task will not terminate, and the exception will be
 * handled by the execution service's exception handler.
 * This guarantees the cyclic execution nature of the
 * task even when exception occurs.
 * <p>
 * It is very important to ensure that the task does
 * not use the corresponding task handle to wait on
 * itself. This will for sure cause dead-lock. Though,
 * it is possible to cancel or terminate the task
 * within the logic execution of the task itself using
 * the handle, in which case, the task should be a
 * self-terminating task.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface ICyclicTask {
	
	/**
	 * Execute the cyclic task logic.
	 * <p>
	 * The thread safety semantics of this method should
	 * be determined based on the execution context of
	 * this task. It is guaranteed that only a single
	 * executor thread will invoke this method, however,
	 * there is no guarantee as which thread performs the
	 * invocation.
	 * @return <code>true</code> if execution should be
	 * continued onto the next cycle. <code>false</code>
	 * if cyclic task should be terminated.
	 * @throws Exception If any processing failed.
	 */
	public boolean execute() throws Exception;
	
	/**
	 * Cleanup any allocated resources during the logic
	 * execution. Such as closing sockets and release
	 * locks.
	 * <p>
	 * This method is invoked after the cyclic execution
	 * terminates.
	 * @throws Exception If any cleanup failed.
	 */
	public void cleanup() throws Exception;
	
	/**
	 * Signal the task to terminate.
	 * <p>
	 * This method should be used to implement any signal
	 * logic that wakes up the execution logic if it is
	 * blocked, so the task can be terminated when it is
	 * requested to terminate.
	 * @throws Exception If any signaling failed.
	 */
	public void signalTerminate() throws Exception;
	
	/**
	 * Retrieve the number of cycles this task should
	 * be executed.
	 * @return The <code>int</code> number of cycles
	 * this task should be executed. If returned value
	 * is less than or equal to 0, the task will be
	 * executed for infinite amount of cycles until it
	 * is terminated explicitly or execution service
	 * shuts down. If a valid value is returned, the
	 * task will automatically terminate after defined
	 * number of execution cycles have been reached.
	 */
	public int getCycleCount();
	
	/**
	 * Retrieve the maximum allowed execution time in
	 * between two consecutive cycles. This value
	 * determines the desired execution rate of the
	 * task. The rate is defined as the inverse of this
	 * value, which would be frequency.
	 * <p>
	 * If the returned value is less than or equal to
	 * 0, the cyclic task will be executed as fast as
	 * the execution of logic can be.
	 * @param unit The <code>TimeUnit</code> returned
	 * value is in.
	 * @return The <code>long</code> cycle time limit
	 * in the given time unit.
	 */
	public long getCycleLimit(final TimeUnit unit);
}
