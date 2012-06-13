package hemera.core.execution.interfaces.task.handle;

import java.util.concurrent.TimeUnit;

/**
 * <code>IResultTaskHandle</code> defines the interface
 * of a task handle that is returned after a result task
 * is successfully submitted. It provides additional
 * result task specific handling functionalities.
 * <p>
 * <code>IResultTaskHandle</code> inherits event task
 * handle as result tasks are event tasks in nature.
 * It provides the additional result handling methods.
 * <p>
 * @param <R> The result task result return type.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface IResultTaskHandle<R> extends IEventTaskHandle {
	
	/**
	 * Wait for the task to be completed and retrieve
	 * the result.
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
	 * immediately.
	 * @return The <code>R</code> task result.
	 * @throws InterruptedException If waiting process
	 * is interrupted.
	 */
	public R getAndWait() throws InterruptedException;
	
	/**
	 * Wait for the given amount of time with given time
	 * unit and retrieve the result if the execution has
	 * completed within the waiting time period.
	 * <p>
	 * This retrieval operation is an advised method for
	 * task result retrieval. It does not permanently
	 * block the caller thread, therefore, will not cause
	 * deadlock. If the result is not available for the
	 * specified time, <code>null</code> is returned.
	 * <p>
	 * If the task is canceled before this method
	 * invocation, <code>null</code> is returned.
	 * If the task execution is completed before the
	 * method invocation, result is directly returned
	 * immediately.
	 * <p>
	 * If given time value is less than or equal to 0
	 * or given time unit is <code>null</code>, then
	 * unconditional wait is used. The invocation will
	 * be equivalent to an invocation to the method
	 * <code>getAndWait</code>.
	 * @param value The <code>Long</code> time amount.
	 * @param unit The <code>TimeUnit</code> enumeration.
	 * @return The <code>R</code> task result. If the
	 * task execution is not completed within the given
	 * time period, <code>null</code> if returned.
	 * @throws InterruptedException If waiting process
	 * is interrupted.
	 */
	public R getAndWait(final long value, final TimeUnit unit) throws InterruptedException;
}
