package hemera.core.execution.interfaces.task.handle;

import java.util.concurrent.TimeUnit;

/**
 * <code>IResultTaskHandle</code> defines the interface
 * of a task handle that is used for result tasks. It
 * provides the functionalities related to task result
 * handling. More specifically, <code>IResultTaskHandle</code>
 * allows the result produced by the corresponding result
 * task to be retrieved using this handle. Various result
 * retrieval methods are provided.
 * <p>
 * <code>IResultTaskHandle</code> inherits event task
 * handle as result tasks are event tasks in nature.
 * It provides the additional result handling methods.
 * <p>
 * @param <V> The result task result return type.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface IResultTaskHandle<V> extends IEventTaskHandle {

	/**
	 * Execute the result task then return the result
	 * if the task execution has not yet been started.
	 * The execution of the task will occur within the
	 * caller thread. If the execution of the task has
	 * been started, this method will block until the
	 * execution is completed, then result is returned.
	 * <p>
	 * If the task has been canceled before this method
	 * is invoked, <code>null</code> is returned.
	 * <p>
	 * This retrieval operation is an advised method
	 * for task result retrieval, if the task logic
	 * does not require specific executing thread. It
	 * does not cause deadlock, since the caller thread
	 * can directly execute the task if the execution
	 * has not been started already.
	 * @return The <code>V</code> task result.
	 * @throws Exception If any logic processing failed.
	 */
	public V executeAndGet() throws Exception;
	
	/**
	 * Retrieve the task result and wait if necessary.
	 * <p>
	 * This retrieval operation should be used with
	 * caution since it blocks the caller until the task
	 * is executed. However, in some situations, the
	 * caller thread may be assigned to execute this
	 * task, in which case a deadlock would occur.
	 * <p>
	 * If the task is canceled before this method
	 * invocation, <code>null</code> is returned.
	 * If the task execution is completed before the
	 * method invocation, result is directly returned
	 * without any blocking.
	 * @return The <code>V</code> task result.
	 * @throws InterruptedException If waiting process
	 * is interrupted.
	 */
	public V getAndWait() throws InterruptedException;
	
	/**
	 * Retrieve the task result and wait for the given
	 * amount of time with given time unit.
	 * <p>
	 * This retrieval operation is an advised method for
	 * task result retrieval. It does not permanently
	 * block the caller thread, therefore, cannot cause
	 * deadlock. If the result is not available for the
	 * specified time, <code>null</code> is returned.
	 * <p>
	 * If the task is canceled before this method
	 * invocation, <code>null</code> is returned.
	 * If the task execution is completed before the
	 * method invocation, result is directly returned
	 * without any blocking.
	 * <p>
	 * If given time value is less than or equal to 0
	 * or given time unit is <code>null</code>, then
	 * unconditional wait is used. The invocation will
	 * be equivalent to an invocation to the method
	 * <code>getAndWait()</code>.
	 * @param value The <code>Long</code> time amount.
	 * @param unit The <code>TimeUnit</code> enumeration.
	 * @return The <code>V</code> task result.
	 * @throws InterruptedException If waiting process
	 * is interrupted.
	 */
	public V getAndWait(final long value, final TimeUnit unit) throws InterruptedException;
}
