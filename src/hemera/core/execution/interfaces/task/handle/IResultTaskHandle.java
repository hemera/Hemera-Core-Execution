package hemera.core.execution.interfaces.task.handle;

import java.util.concurrent.TimeUnit;

/**
 * <code>IResultTaskHandle</code> defines an extension
 * to the <code>IEventTaskHandle</code> to provide the
 * additional support related to a result task.
 * <p>
 * <code>IResultTaskHandle</code> allows a thread to
 * wait for task execution completion, then retrieve
 * the result produced by the task, in addition to the
 * just waiting for task completion.
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
