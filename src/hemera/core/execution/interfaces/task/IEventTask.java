package hemera.core.execution.interfaces.task;

/**
 * <code>IEventTask</code> defines the interface of an
 * atomic unit of logic that directly corresponds to
 * an application event. The logic contained in the
 * task is only executed once in a single execution
 * cycle. There is no guarantee as to which executor
 * thread executes the task, thus the contained logic
 * should not have any concurrency restrictions.
 * <p>
 * <code>IEventTask</code> can be canceled via the
 * returned task handle unit. An event task may only
 * be canceled if the execution has not yet started.
 * Once the execution begins, the canceling the task
 * has no effect.
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
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface IEventTask {

	/**
	 * Execute the event task logic.
	 * <p>
	 * The thread safety semantics of this method should
	 * be determined based on the execution context of
	 * this task. It is guaranteed that only a single
	 * executor thread will invoke this method, however,
	 * there is no guarantee as which thread performs the
	 * invocation.
	 * @throws Exception If any processing failed.
	 */
	public void execute() throws Exception;
}
