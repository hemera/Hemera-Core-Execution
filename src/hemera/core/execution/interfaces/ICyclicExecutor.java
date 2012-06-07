package hemera.core.execution.interfaces;

import hemera.core.execution.interfaces.task.ICyclicTask;
import hemera.core.execution.interfaces.task.handle.ICyclicTaskHandle;

/**
 * <code>ICyclicExecutor</code> defines the interface
 * of an executor unit that is only responsible for
 * the execution of a single <code>ICyclicTask</code>.
 * It internally constructs and returns an instance
 * of <code>ICyclicTaskHandle</code> for a successful
 * submission.
 * <p>
 * <code>ICyclicExecutor</code> internally handles the
 * repeated task execution process. It automatically
 * attempts to best confirm with the desired execution
 * rate defined by the cyclic task's execution cycle
 * time limit. However, there is no guarantee that the
 * actual execution rate is at the desired rate.
 * <p>
 * <code>ICyclicExecutor</code> does not support
 * the execution of multiple cyclic tasks. Since the
 * execution cycle time for cyclic tasks vary greatly,
 * buffering a cyclic task behind an existing one may
 * starve the newly buffered task. Since thread
 * context-switches are very cheap on most operating
 * systems, it is acceptable to create new cyclic
 * executors whenever needed.
 * <p>
 * <code>ICyclicExecutor</code> is a recyclable executor
 * that automatically recycles itself when all assigned
 * cyclic tasks cease execution. Therefore, it should be
 * constructed with an executor recycle bin assigned by
 * the <code>IExecutionService</code> as a centralized
 * place to recycle executor for new task assignment or
 * executor disposal.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface ICyclicExecutor extends IExecutor {
	
	/**
	 * Assign the given cyclic task to this executor
	 * for repeated execution.
	 * <p>
	 * This method guarantees its thread safety by
	 * delegating synchronization mechanism down to its
	 * thread safe internal data structures.
	 * @param task The <code>ICyclicTask</code> to be
	 * executed.
	 * @return The <code>ICyclicTaskHandle</code> for
	 * the assigned cyclic task. <code>null</code> if
	 * there is already a task assigned or the executor
	 * has been terminated.
	 */
	public ICyclicTaskHandle assign(final ICyclicTask task);
}
