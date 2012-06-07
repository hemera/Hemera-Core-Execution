package hemera.core.execution.interfaces.task;

/**
 * <code>IEventTask</code> defines the interface of an
 * atomic unit of logic that directly corresponds to
 * an application event. The logic contained in the
 * task is only executed once in a single execution
 * cycle. There is no guarantee as to which executor
 * thread executes the task, thus the contained logic
 * should not have any thread-based assumptions or
 * restrictions.
 * <p>
 * <code>IEventTask</code> can be submitted as either
 * a foreground or background task to the service.
 * <p>
 * If the task is submitted as a foreground task, it
 * is very important that the logic contained within
 * the result task does not involve any long blocking
 * operations. The logic should be computation centric
 * and involves very little I/O operations. Short I/O
 * operations with no or very short blocking time are
 * acceptable.
 * <p>
 * If the task is submitted as a background task, the
 * contained task logic may involve long blocking I/O
 * operations or any operations with long blocking
 * periods, such as long running database committing
 * operations. Once submitted as a background task,
 * the task will be executed with its own dedicated
 * executor.
 * <p>
 * If the task logic requires to be executed for many
 * execution cycles, or every execution cycle, it is
 * advised to submit the task as a cyclic task to the
 * execution service and let the service take care of
 * the repeated execution process internally. This is
 * much more efficient than manually re-submitting
 * the same task to the execution service every single
 * execution cycle.
 * <p>
 * <code>IEventTask</code> can be canceled via the
 * returned task handle unit. An even task may only
 * be canceled if the execution has not yet started.
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
	 */
	public void execute();
}
