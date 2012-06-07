package hemera.core.execution;

import hemera.core.execution.interfaces.task.handle.ITaskHandle;

/**
 * <code>IExecutable</code> defines the interface of an
 * intermediate compound unit that provides the support
 * to store the execution result and various handling
 * methods provided by <code>ITaskHandle</code>.
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
 * <code>IExecutable</code> provides thread-ownership
 * guarantee, which ensures only the very first thread
 * that invokes <code>execute</code> is set as the
 * owner of the executable instance. And since only
 * the owner thread is allowed to proceed with actual
 * executable logic execution, all invocations by other
 * threads are directly ignored. 
 * <p>
 * <code>IExecutable</code> should only be used as an
 * internal data structure. External usage should never
 * occur.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
interface IExecutable extends ITaskHandle {
	
	/**
	 * Execute the internal task logic and store the
	 * execution result within the executable unit
	 * for later retrieval.
	 * <p>
	 * This method provides thread-ownership guarantee,
	 * which ensures only the very first thread that
	 * invokes <code>execute</code> is set as the owner
	 * of the executable instance. And since only the
	 * owner thread is allowed to proceed with actual
	 * executable logic execution, all invocations by
	 * other threads are directly ignored. 
	 * <p>
	 * This method also automatically performs task
	 * cancellation detection internally to ensure that
	 * canceled task is early-exited without actual
	 * logic execution being performed.
	 * @throws Exception If any processing failed.
	 */
	public void execute() throws Exception;
}
