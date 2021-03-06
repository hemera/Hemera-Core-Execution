package hemera.core.execution.interfaces;

import hemera.core.execution.interfaces.task.ICyclicTask;
import hemera.core.execution.interfaces.task.IEventTask;
import hemera.core.execution.interfaces.task.IResultTask;
import hemera.core.execution.interfaces.task.handle.ICyclicTaskHandle;
import hemera.core.execution.interfaces.task.handle.IEventTaskHandle;
import hemera.core.execution.interfaces.task.handle.IResultTaskHandle;

/**
 * <code>IExecutor</code> defines the interface of the
 * most fundamental execution unit that is responsible
 * for the execution of task logic within its internal
 * dedicated thread.
 * <p>
 * <code>IExecutor</code> only allows a single task to
 * be assigned. For every successfully assigned task,
 * it returns an appropriate task handle back to the
 * caller for task handling. Subsequent task assignment
 * to an executor which has already been assigned with
 * a task, but has not yet completed the execution will
 * directly return the task handle corresponding to the
 * first assigned task.
 * <p>
 * <code>IExecutor</code> only executes the assigned
 * task once. It internally handles discarding of the
 * executed task automatically. Each assigned task is
 * guaranteed to be executed once and once only.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface IExecutor extends Runnable {

	/**
	 * Start this executor.
	 * <p>
	 * This method is guarded by the intrinsic lock of
	 * the executor itself. Early exit is also provided
	 * if the executor has already been started.
	 */
	public void start();
	
	/**
	 * Forcefully terminate the executor.
	 * <p>
	 * This method guarantees to terminate the executor
	 * immediately. If the executor is in the process of
	 * execution, the execution is interrupted.
	 */
	public void forceTerminate();
	
	/**
	 * Gracefully terminate this executor and cancel
	 * all assigned tasks.
	 * <p>
	 * The executor may not terminate immediately if
	 * it is already in the process of execution. It
	 * is guaranteed that the executor will terminate
	 * after it completes the started execution.
	 */
	public void requestTerminate();

	/**
	 * Assign the given event task to this executor.
	 * <p>
	 * This method guarantees its thread safety by
	 * delegating synchronization mechanism down to
	 * its thread safe internal data structures.
	 * <p>
	 * The blocking behavior of this method depends
	 * on the specific types of executors. This is
	 * documented in the corresponding service.
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
	 * The blocking behavior of this method depends
	 * on the specific types of executors. This is
	 * documented in the corresponding service.
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
	 * The blocking behavior of this method depends
	 * on the specific types of executors. This is
	 * documented in the corresponding service.
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
	 * Retrieve the name of this executor.
	 * @return The <code>String</code> name.
	 */
	public String getName();
	
	/**
	 * Check if the executor has been started.
	 * <p>
	 * This method returns the most update to date result
	 * as its implementation should ensure the memory
	 * visibility of the status.
	 * @return <code>true</code> if the executor has
	 * started. <code>false</code> otherwise.
	 */
	public boolean hasStarted();
	
	/**
	 * Check if the executor has been terminated.
	 * <p>
	 * This method returns the most update to date result
	 * as its implementation should ensure the memory
	 * visibility of the status.
	 * @return <code>true</code> if the executor has
	 * terminated. <code>false</code> otherwise.
	 */
	public boolean hasTerminated();
	
	/**
	 * Check if the executor has been requested to
	 * terminate as soon as possible.
	 * <p>
	 * This method returns the most update to date result
	 * as its implementation should ensure the memory
	 * visibility of the status.
	 * @return <code>true</code> if the executor has
	 * been requested to terminate as soon as possible.
	 * <code>false</code> otherwise.
	 */
	public boolean hasRequestedTermination();
}
