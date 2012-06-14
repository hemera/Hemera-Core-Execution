package hemera.core.execution.interfaces;

import hemera.core.execution.interfaces.task.handle.ITaskHandle;

/**
 * <code>IExecutable</code> defines the interface of an
 * intermediate composite unit that is a task to be
 * executed, as well as the handle for the task. This
 * is an internal structure used by execution services.
 * <p>
 * <code>IExecutable</code> implementation is required
 * to provide all necessary support for task handling
 * functionalities defined by <code>ITaskHandle</code>.
 * In addition to result handling, implementations should
 * also provide automatic checks on cancellation flag
 * and duplicated execute invocations. These supports
 * ensure that the <code>execute</code> method to be a
 * single entry point of task execution. Typically, no
 * additional checks are needed prior to invoking the
 * <code>execute</code> method.
 * <p>
 * <code>IExecutable</code> should only be used as an
 * internal data structure. External usage should not
 * occur.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface IExecutable extends ITaskHandle {
	
	/**
	 * Execute the internal task logic and store the
	 * execution result within the executable unit
	 * for later retrieval.
	 * <p>
	 * This method automatically performs cancellation
	 * and completion detection internally to ensure
	 * that a canceled or finished task is early-exited
	 * without actual logic execution being performed.
	 * @throws Exception If any processing failed.
	 */
	public void execute() throws Exception;
}
