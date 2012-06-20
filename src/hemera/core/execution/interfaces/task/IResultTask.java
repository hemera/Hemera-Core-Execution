package hemera.core.execution.interfaces.task;

/**
 * <code>IResultTask</code> defines the interface of
 * an atomic unit of logic that directly corresponds
 * to a particular application event that produces a
 * result after completion of execution. The logic
 * contained in the task is only executed once in a
 * single execution cycle. There is no guarantee as
 * to which executor thread executes the task, thus
 * the contained logic should not have any concurrency
 * restrictions.
 * <p>
 * <code>IResultTask</code> produced result can be
 * retrieved via returned <code>IResultTaskHandle</code>.
 * Cancellation of this task may also be performed via
 * the task handle. A result task may only be canceled
 * if the execution has not yet started. Once the task
 * execution begins, the canceling has no effect.
 * <p>
 * Task instance reuse is not encouraged, since the
 * same instance of task submitted to the execution
 * service may be executed concurrently by two separate
 * executor threads. Unless thread-safety measures are
 * taken, such cases could cause thread-safety issues
 * within the task logic.
 * <p>
 * It is very important to ensure that the task does
 * not use the corresponding task handle to wait on
 * itself. This will for sure cause dead-lock. Though,
 * it is possible to cancel the task within the logic
 * execution of the task itself using the handle, in
 * which case the task would be a self-canceling task.
 * <p>
 * @param <R> The result task result return type.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface IResultTask<R> {

	/**
	 * Execute the result task logic.
	 * <p>
	 * The thread safety semantics of this method should
	 * be determined based on the execution context of
	 * this task. It is guaranteed that only a single
	 * executor thread will invoke this method, however,
	 * there is no guarantee as which thread performs the
	 * invocation.
	 * @return The <code>R</code> execution result.
	 * @throws Exception If any processing failed.
	 */
	public R execute() throws Exception;
}
