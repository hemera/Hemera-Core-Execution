package hemera.core.execution.interfaces;

/**
 * <code>IExecutor</code> defines the interface of the
 * most fundamental utility unit that is responsible
 * for the actual execution of task logic within its
 * own internal thread. Corresponding implementations
 * should provide the necessary thread safety guarantees
 * specified by individual method documentations.
 * <p>
 * <code>IExecutor</code> internally maintains its own
 * dedicated thread that performs the execution of the
 * tasks assigned to the executor. It is advised to
 * avoid construction of individual executor instances
 * outside the execution service. It is suggested to use
 * execution service as the centralized thread resource
 * management unit to better utilize system resources.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface IExecutor extends Runnable {

	/**
	 * Start this executor.
	 * <p>
	 * This method is guarded by the intrinsic lock of
	 * the executor itself. Early fail is also provided
	 * if the executor has already been started.
	 */
	public void start();
	
	/**
	 * Gracefully terminate this executor and cancel
	 * all assigned tasks.
	 * <p>
	 * The executor may not terminate immediately if
	 * it is already in the process of execution. It
	 * is guaranteed that the executor will terminate
	 * after it completes the started execution.
	 * @throws Exception If termination logic failed.
	 */
	public void terminate() throws Exception;
	
	/**
	 * Forcefully terminate the executor.
	 * <p>
	 * This method guarantees to terminate the executor
	 * immediately. If the executor is in the process of
	 * execution, the execution is interrupted.
	 * @throws Exception If termination logic failed.
	 */
	public void forceTerminate() throws Exception;
	
	/**
	 * Check if the executor has been started.
	 * <p>
	 * This method returns the most update to date result
	 * as its implementation should ensure the memory
	 * visibility of the started flag.
	 * @return The <code>Boolean</code> started flag.
	 */
	public boolean hasStarted();
	
	/**
	 * Check if the executor has been terminated.
	 * <p>
	 * This method returns the most update to date result
	 * as its implementation should ensure the memory
	 * visibility of the terminated flag.
	 * @return The <code>Boolean</code> terminated flag.
	 */
	public boolean hasTerminated();
}
