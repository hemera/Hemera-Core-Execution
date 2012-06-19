package hemera.core.execution.interfaces.assisted;

import hemera.core.execution.interfaces.IExecutor;
import hemera.core.execution.interfaces.task.ICyclicTask;
import hemera.core.execution.interfaces.task.IEventTask;
import hemera.core.execution.interfaces.task.IResultTask;
import hemera.core.execution.interfaces.task.handle.ICyclicTaskHandle;
import hemera.core.execution.interfaces.task.handle.IEventTaskHandle;
import hemera.core.execution.interfaces.task.handle.IResultTaskHandle;

/**
 * <code>IAssistExecutor</code> defines the interface
 * of an executor unit that is capable of assisting
 * other <code>IAssistExecutor</code> instances that
 * are grouped together to form a work-stealing group.
 * It defines the necessary method to allow other
 * executor instances in the group to execute the
 * tasks assigned to this particular instance.
 * <p>
 * <code>IAssistExecutor</code> can be assigned with
 * multiple instances of tasks. It internally performs
 * appropriate buffering to lower the level contention
 * on the task buffer during execution and assisting.
 * For every successfully assigned task, it returns an
 * appropriate task handle back to the caller for task
 * handling. The internal task buffer has a defined
 * upper limit to prevent memory leak. When this limit
 * is reached, new task assignments will block.
 * <p>
 * <code>IAssistExecutor</code> only executes assigned
 * tasks once. It internally handles discarding of
 * executed tasks automatically. Each assigned task is
 * guaranteed to be executed once and once only.
 * <p>
 * <code>IAssistExecutor</code> is constructed with a
 * shared instance of <code>IAssistService</code> that
 * supports work-stealing based task load balancing
 * mechanism. This allows this instance of executor to
 * perform work-stealing on other executor instances
 * in the group.
 * <p>
 * <code>IAssistExecutor</code> provides eager-idling
 * that suspends thread execution when there's no tasks
 * assigned in the internal task buffer nor can it
 * steal tasks assigned to other executors in the assist
 * group. This allows the executor to conserve system
 * resources when there is no work to be done. At the
 * same time, eager-idling allows the execution thread
 * to wake up eagerly to try to steal tasks assigned to
 * other executors in the assist group. This eager wake
 * up is independent from the task assignment wake-up,
 * where the executor thread is woken up if a task is
 * assigned to this executor. Eager-idling can help the
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
 * <p>
 * Assist executors in a group follows the execution
 * pattern:
 * 1. Execute local task buffer until empty.
 * 2. Reach group to assist other executors until
 * all executor buffers are empty.
 * 3. Block until a new task is assigned to the local
 * buffer or eager-idling time expires.
 * 4. Go to step 1.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface IAssistExecutor extends IExecutor {
	
	/**
	 * Assist this executor by executing a task from
	 * its internal task buffer within the invoking
	 * thread.
	 * @return <code>true</code> if a task is executed.
	 * <code>false</code> if this executor has no more
	 * tasks in its internal buffer.
	 */
	public boolean assist();
	
	/**
	 * Assign the given event task to this executor.
	 * <p>
	 * This method guarantees its thread safety by
	 * delegating synchronization mechanism down to
	 * its thread safe internal data structures.
	 * <p>
	 * This method will block if the maximum limit of
	 * the internal task buffer has been reached. The
	 * new task assignment will return when an already
	 * assigned task completes.
	 * @param task The <code>IEventTask</code> to be
	 * executed.
	 * @return The <code>IEventTaskHandle</code> for
	 * the assigned event task. 
	 * @throws IllegalStateException If the executor
	 * has been terminated.
	 */
	public IEventTaskHandle assign(final IEventTask task) throws IllegalStateException;
	
	/**
	 * Assign the given cyclic task to this executor.
	 * <p>
	 * This method guarantees its thread safety by
	 * delegating synchronization mechanism down to
	 * its thread safe internal data structures.
	 * <p>
	 * This method will block if the maximum limit of
	 * the internal task buffer has been reached. The
	 * new task assignment will return when an already
	 * assigned task completes.
	 * @param task The <code>ICyclicTask</code> to be
	 * executed.
	 * @return The <code>ICyclicTaskHandle</code> for
	 * the assigned cyclic task. 
	 * @throws IllegalStateException If the executor
	 * has been terminated.
	 */
	public ICyclicTaskHandle assign(final ICyclicTask task) throws IllegalStateException;
	
	/**
	 * Assign the given result task to this executor.
	 * <p>
	 * This method guarantees its thread safety by
	 * delegating synchronization mechanism down to
	 * its thread safe internal data structures.
	 * <p>
	 * This method will block if the maximum limit of
	 * the internal task buffer has been reached. The
	 * new task assignment will return when an already
	 * assigned task completes.
	 * @param V The result task result type.
	 * @param task The <code>IResultTask</code> to be
	 * executed.
	 * @return The <code>IResultTaskHandle</code> for
	 * the assigned result task.
	 * @throws IllegalStateException If the executor
	 * has been terminated.
	 */
	public <V> IResultTaskHandle<V> assign(final IResultTask<V> task) throws IllegalStateException;
	
	/**
	 * Retrieve the length of the waiting task queue.
	 * @return The <code>int</code> number of tasks
	 * assigned to the executor but are in the waiting
	 * state to be executed.
	 */
	public int getQueueLength();
}
