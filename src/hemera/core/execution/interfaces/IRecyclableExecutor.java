package hemera.core.execution.interfaces;

import hemera.core.execution.interfaces.task.IEventTask;
import hemera.core.execution.interfaces.task.IResultTask;
import hemera.core.execution.interfaces.task.handle.IEventTaskHandle;
import hemera.core.execution.interfaces.task.handle.IResultTaskHandle;

/**
 * <code>IRecyclableExecutor</code> defines the interface
 * of an on-demand executor unit that is created based
 * on current work-load and recycled when it finishes
 * its assigned task. This is typically suited for the
 * type of tasks that require dedicated execution thread
 * and may have an end time for the execution life-time.
 * Typical background tasks are perfect examples of such
 * tasks.
 * <p>
 * <code>IRecyclableExecutor</code> accepts event and
 * result tasks that need to be executed in background
 * by its own dedicated thread. Event and result tasks
 * that contain long blocking operations are well
 * suited for this case. Unlike the foreground
 * <code>IAssistExecutor</code>, it does not support
 * work-stealing load balancing.
 * <p>
 * <code>IRecyclableExecutor</code> does not support
 * the execution of multiple background tasks. Since
 * the blocking-time for background tasks vary greatly,
 * buffering a background task behind an existing one
 * may starve the newly buffered task. Since thread
 * context-switches are very cheap on most operating
 * systems, it is acceptable to create new recyclable
 * executors whenever needed.
 * <p>
 * <code>IRecyclableExecutor</code> is constructed with
 * an instance of executor recycle bin as a centralized
 * structure to recycle executors for assignments of
 * new tasks or executor disposal. A recyclable executor
 * automatically recycles itself after the assigned task
 * is completed.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface IRecyclableExecutor extends IExecutor {
	
	/**
	 * Assign the given event task to this executor
	 * for background execution if there isn't already
	 * a task assigned.
	 * <p>
	 * This method guarantees its thread safety by
	 * delegating synchronization mechanism down to its
	 * thread safe internal data structures.
	 * @param task The <code>IEventTask</code> to be
	 * executed in background.
	 * @return The <code>IEventTaskHandle</code> for the
	 * assigned event task. <code>null</code> if there
	 * is already a task assigned or the executor has
	 * been terminated.
	 */
	public IEventTaskHandle assign(final IEventTask task);

	/**
	 * Assign the given result task to this executor
	 * for background execution if there isn't already
	 * a task assigned.
	 * <p>
	 * This method guarantees its thread safety by
	 * delegating synchronization mechanism down to its
	 * thread safe internal data structures.
	 * @param V The even task result type.
	 * @param task The <code>IResultTask</code> to be
	 * executed in background.
	 * @return The <code>IResultTaskHandle</code> for the
	 * assigned result task. <code>null</code> if there
	 * is already a task assigned or the executor has
	 * been terminated.
	 */
	public <V> IResultTaskHandle<V> assign(final IResultTask<V> task);
}
