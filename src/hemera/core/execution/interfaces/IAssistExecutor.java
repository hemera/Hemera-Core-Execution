package hemera.core.execution.interfaces;

import hemera.core.execution.interfaces.task.IEventTask;
import hemera.core.execution.interfaces.task.IResultTask;
import hemera.core.execution.interfaces.task.handle.IEventTaskHandle;
import hemera.core.execution.interfaces.task.handle.IResultTaskHandle;

/**
 * <code>IAssistExecutor</code> defines the interface
 * of an executor unit that is capable of assisting
 * other <code>IAssistExecutor</code> instances that
 * are grouped together to form a work-stealing group.
 * It defines the necessary method to allow executor
 * instances in the group to assist with the tasks
 * assigned to this instance.
 * <p>
 * <code>IAssistExecutor</code> should only be assigned
 * with short-running, computational-based tasks due to
 * the nature of the work-stealing design. This directly
 * corresponds to foreground <code>IEventTask</code>
 * and <code>IResultTask</code> instances. It accepts
 * multiple instances of such tasks and performs the
 * appropriate internal buffering. For every single
 * successfully assigned task instance, it internally
 * constructs an appropriate task handle and returns it
 * back to the caller for external handling.
 * <p>
 * <code>IAssistExecutor</code> only executes assigned
 * tasks once. It internally handles discarding of
 * executed tasks automatically. Each assigned task is
 * guaranteed to be executed once and once only.
 * <p>
 * <code>IAssistExecutor</code> is constructed with a
 * shared instance of <code>IAssistBarrier</code> that
 * supports work-stealing based task load balancing
 * mechanism. This allows this instance of executor to
 * perform work-stealing with the executor group.
 * <p>
 * <code>IAssistExecutor</code> provides eager-idling
 * that suspends thread execution when there's no tasks
 * to be executed. This allows the executor to conserve
 * system resources when there is no work to be done.
 * At the same time, eager-idling allows the execution
 * thread to wake up eagerly to assist other executors
 * in the assist group. This feature can help assist
 * executors recover from a dead-lock situation. If an
 * assist executor is waiting on a task to complete,
 * while the task to be completed is also assigned to
 * the same waiting executor, dead-lock occurs. Using
 * eager-idling, other assist executors in the group
 * can eagerly wake up to execute the queued task thus
 * allowing the dead-locked executor to recover. However,
 * task implementations should never rely on this eager
 * idling feature to solve dead-lock issues, since all
 * assist executors in the system may be dead-locked,
 * then the system will not be able to recover.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface IAssistExecutor extends IExecutor {
	
	/**
	 * Assign the given event task to this executor
	 * for foreground execution as soon as possible.
	 * <p>
	 * This assignment is not final, in the sense
	 * that work-stealing may be performed resulting
	 * in the task to be executed by another instance
	 * of assist executor in the executor group.
	 * <p>
	 * This method guarantees its thread safety by
	 * delegating synchronization mechanism down to
	 * its thread safe internal data structures.
	 * @param task The <code>IEventTask</code> to be
	 * executed.
	 * @return The <code>IEventTaskHandle</code> for
	 * the assigned event task.
	 */
	public IEventTaskHandle assign(final IEventTask task);
	
	/**
	 * Assign the given result task to this executor
	 * for foreground execution as soon as possible.
	 * <p>
	 * This assignment is not final, in the sense
	 * that work-stealing may be performed resulting
	 * in the task to be executed by another instance
	 * of assist executor in the executor group.
	 * <p>
	 * This method guarantees its thread safety by
	 * delegating synchronization mechanism down to
	 * its thread safe internal data structures.
	 * @param V The result task result type.
	 * @param task The <code>IResultTask</code> to be
	 * executed.
	 * @return The <code>IResultTaskHandle</code> for
	 * the assigned result task.
	 */
	public <V> IResultTaskHandle<V> assign(final IResultTask<V> task);
	
	/**
	 * Execute the tail task, buffered internally in
	 * this assist executor.
	 * <p>
	 * The invoking thread of this method will directly
	 * execute the tail task in the internal task buffer.
	 * This method is provided as a means to assist this
	 * executor to perform its buffered tasks.
	 * <p>
	 * Typically this method should only be invoked by
	 * other assist executors within execution service
	 * as part of the work-stealing load-balancing
	 * process. External invocation should be avoided.
	 * @return <code>true</code> if the tail task has
	 * been executed. <code>false</code> if the internal
	 * task buffer is already empty.
	 */
	public boolean executeTail();
	
	/**
	 * Retrieve the length of the waiting task queue.
	 * @return The <code>int</code> length of the queue
	 * of waiting tasks. This value should be used as a
	 * part of the system workload information.
	 */
	public int getQueueLength();
}
