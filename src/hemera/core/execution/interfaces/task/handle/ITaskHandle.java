package hemera.core.execution.interfaces.task.handle;

/**
 * <code>ITaskHandle</code> defines the interface of a
 * common task handle that is returned to the submitting
 * thread once the task is successfully submitted. It
 * provides the common functionality of all types of
 * task handles.
 * <p>
 * <code>ITaskHandle</code> only provides basic common
 * definition of various type-specific task handles. These
 * type-specific handles may provide additional handling
 * functionalities oriented to support the particular task
 * type.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface ITaskHandle {

	/**
	 * Cancel the task execution.
	 * <p>
	 * This method guarantees its result memory visibility.
	 * No synchronization locking is provided or needed.
	 * @return The <code>Boolean</code> cancellation success
	 * flag. <code>false</code> if the cancellation failed.
	 * <code>true</code> if execution is canceled.
	 */
	public boolean cancel();
	
	/**
	 * Check if the task execution has started.
	 * @return <code>true</code> if the execution has
	 * already started. <code>false</code> otherwise.
	 */
	public boolean hasStarted();
	
	/**
	 * Check if the task execution has finished.
	 * @return <code>true</code> if the execution has
	 * already finished. <code>false</code> otherwise.
	 */
	public boolean hasFinished();

	/**
	 * Check if the task has been canceled.
	 * @return The <code>Boolean</code> canceled flag.
	 */
	public boolean isCanceled();
}
