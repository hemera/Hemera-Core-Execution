package hemera.core.execution.interfaces.task.handle;

import hemera.core.execution.enumn.ETaskOrder;
import hemera.core.execution.interfaces.cycle.IObservableCycle;
import hemera.core.execution.interfaces.task.IEventTask;
import hemera.core.execution.interfaces.task.IResultTask;

/**
 * <code>ICyclicTaskHandle</code> defines the interface
 * of a cyclic task handle that is returned after a
 * cyclic task is successfully submitted. It provides
 * the additional functionalities related to in-process
 * task injection.
 * <p>
 * <code>ICyclicTaskHandle</code> provides the methods
 * to allow an external caller to inject event and
 * result tasks to the cyclic task's executor. This
 * guarantees that these injected tasks are executed
 * within the thread of the cyclic task executor thread.
 * This is especially useful if the injected tasks
 * require a confined thread execution environment such
 * as all {@link OpenGL} method invocations, which
 * require to be executed within the rendering thread.
 * This can be generalized to include all tasks that
 * require expensive context switch operations.
 * <p>
 * <code>ICyclicTaskHandle</code> provides two set of
 * task buffers, front and back for both types of task.
 * The front buffer is executed before the actual
 * cyclic task logic is executed and the back buffer
 * is executed after the completion of the cyclic task
 * logic. Depending on the actual logic of injected
 * task, either buffer can be used to suit the task's
 * specific needs. Upon successful injection, a task
 * handle of the appropriate type is returned to allow
 * external handling of the injected tasks.
 * <p>
 * <code>ICyclicTaskHandle</code> inherits cancellation
 * support from <code>ITaskHandle</code>. Canceling a
 * cyclic task has the effect of ceasing the repeated
 * execution of the task logic prematurely. If the cyclic
 * task execution of the current execution cycle has
 * already been started, the cancellation will take
 * effect in the succeeding execution cycle. If the
 * cancellation is made before the current execution
 * cycle execution starts, the cyclic task will not be
 * executed for the current execution cycle. Once the
 * cancellation succeeds, the cyclic task will be
 * discarded from its executor.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface ICyclicTaskHandle extends ITaskHandle, IObservableCycle {

	/**
	 * Inject the given task to the specified buffer as
	 * an event task for later execution by cyclic task
	 * executor.
	 * <p>
	 * This method guarantees its thread safety by
	 * delegating the synchronization mechanism to the
	 * underlying task buffer data structure.
	 * @param order The <code>ETaskOrder</code>
	 * enumeration value.
	 * @param task The <code>IEventTask</code> to be
	 * injected.
	 * @return The <code>IEventTaskHandle</code> of
	 * injected task.
	 */
	public IEventTaskHandle inject(final ETaskOrder order, final IEventTask task);

	/**
	 * Inject the given task to the specified buffer as
	 * a result task for later execution by cyclic task
	 * executor.
	 * <p>
	 * This method guarantees its thread safety by
	 * delegating the synchronization mechanism to the
	 * underlying task buffer data structure.
	 * @param order The <code>ETaskOrder</code>
	 * enumeration value.
	 * @param task The <code>IResultTask</code> to be
	 * injected.
	 * @param T The result task result return type.
	 * @return The <code>IResultTaskHandle</code> of
	 * injected task.
	 */
	public <V> IResultTaskHandle<V> inject(final ETaskOrder order, final IResultTask<V> task);
}
