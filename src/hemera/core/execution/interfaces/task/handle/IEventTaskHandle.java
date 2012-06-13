package hemera.core.execution.interfaces.task.handle;

import java.util.concurrent.TimeUnit;

/**
 * <code>IEventTaskHandle</code> defines the interface
 * of a task handle that is returned after an event task
 * is successfully submitted. It provides additional
 * event task specific handling functionalities.
 * <p>
 * <code>IEventTaskHandle</code> inherits cancellation
 * support from the <code>ITaskHandle</code> interface.
 * More specifically, an event task can be canceled if
 * its logic execution has not yet been started. Once
 * the execution is started, the cancellation process
 * will take no effect.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface IEventTaskHandle extends ITaskHandle {
	
	/**
	 * Wait until the task execution is completed.
	 * <p>
	 * This operation should be used with caution since
	 * it blocks the caller until the task is executed.
	 * However, in some situations, the caller thread
	 * may be assigned to execute this task, in which
	 * case a deadlock would occur.
	 * <p>
	 * If the task is completed or canceled before this
	 * method is invoked, invoking this method returns
	 * immediately.
	 * @return <code>true</code> if the task execution
	 * is completed. <code>false</code> if the task is
	 * canceled.
	 * @throws InterruptedException If waiting process
	 * is interrupted.
	 */
	public boolean await() throws InterruptedException;
	
	/**
	 * Wait until either the task execution finishes
	 * or the specified time elapses.
	 * <p>
	 * This operation is an advised method for waiting.
	 * It does not permanently block the caller thread,
	 * therefore, will not cause deadlock.
	 * <p>
	 * If the task is canceled or completed before this
	 * method is invoked, invoking this method returns
	 * immediately.
	 * <p>
	 * If given time value is less than or equal to 0
	 * or given time unit is <code>null</code>, then
	 * unconditional wait is used. The invocation will
	 * be equivalent to an invocation of the method
	 * <code>await</code>.
	 * @param value The <code>Long</code> time value.
	 * @param unit The <code>TimeUnit</code> in the
	 * given value is in.
	 * @return <code>true</code> if the task execution
	 * is completed. <code>false</code> if the task is
	 * canceled or the specified time elapsed but the
	 * execution has not yet completed.
	 * @throws InterruptedException If waiting process
	 * is interrupted.
	 */
	public boolean await(final long value, final TimeUnit unit) throws InterruptedException;
}
