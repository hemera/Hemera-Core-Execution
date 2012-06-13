package hemera.core.execution.interfaces;

import java.util.concurrent.TimeUnit;

import hemera.core.execution.interfaces.task.IEventTask;
import hemera.core.execution.interfaces.task.IResultTask;
import hemera.core.execution.interfaces.task.handle.IEventTaskHandle;
import hemera.core.execution.interfaces.task.handle.IResultTaskHandle;

/**
 * <code>IExecutionService</code> defines the interface
 * of the centralized thread management service that is
 * responsible for the construction and maintenance of
 * all executor instances. It should be considered as a
 * single entry point to task execution for the rest of
 * the system. There should not be any direct accesses
 * to individual executor units or native threads.
 * <p>
 * Tasks submitted to the execution service are assigned
 * to a set of executors each running a dedicated thread
 * for execution. Task assignment strategy is defined by
 * the actual type of the service.
 * <p>
 * <code>IExecutionService</code> returns task handles
 * upon successful submissions. These handles allow the
 * caller to perform various task maintenance operations.
 * Depending on the type of the submitted task, the
 * returned handle provides task type specific handling
 * functionalities. See various task handles for detailed
 * documentation.
 * <p>
 * To ensure that all executor threads do not crash due
 * to <code>Exception</code>, execution service requires
 * an <code>IExceptionHandler</code> instance at the
 * construction time so it can pass this handler unit
 * to all executors allowing them to gracefully handle
 * exceptions at runtime. The exception handler unit is
 * automatically registered as a system shutdown hook.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface IExecutionService {

	/**
	 * Activate the execution service.
	 * <p>
	 * This method provides the necessary thread-safety
	 * to only allow the very first invocation to pass
	 * by and perform the activation process. Subsequent
	 * invocations are directly returned.
	 */
	public void activate();

	/**
	 * Gracefully shutdown the execution service and all
	 * its executors.
	 * <p>
	 * This method may return before all executor threads
	 * are actually terminated. If invoking thread should
	 * wait for all threads to terminate, use the method
	 * <code>shutdownAndWait</code>.
	 * <p>
	 * Some executors may not terminate immediately if
	 * they are already in the process of execution. It
	 * is guaranteed that all executors will terminate
	 * eventually after they complete started execution.
	 * <p>
	 * This method provides the necessary thread-safety
	 * to only allow the very first invocation to pass
	 * by and perform the shutdown process. Subsequent
	 * invocations are directly returned.
	 */
	public void shutdown();

	/**
	 * Gracefully shutdown the execution service and all
	 * its executors and block until all executor threads
	 * are actually terminated.
	 * <p>
	 * This method provides the necessary thread-safety
	 * to only allow the very first invocation to pass
	 * by and perform the shutdown process. Subsequent
	 * invocations are directly returned.
	 * @throws InterruptedException If waiting process
	 * is interrupted.
	 */
	public void shutdownAndWait() throws InterruptedException;
	
	/**
	 * Forcefully shutdown the execution service and all
	 * its executors.
	 * <p>
	 * This method guarantees to terminate all executors
	 * immediately. If an executor is in the process of
	 * execution, the execution is interrupted.
	 * <p>
	 * This method provides the necessary thread-safety
	 * to only allow the very first invocation to pass
	 * by and perform the shutdown process. Subsequent
	 * invocations are directly returned.
	 */
	public void forceShutdown();
	
	/**
	 * First attempt to gracefully shutdown the service
	 * and all executors. Then after the given time has
	 * elapsed, forcefully shutdown the execution service
	 * and all its executors.
	 * <p>
	 * This method provides the necessary thread-safety
	 * to only allow the very first invocation to pass
	 * by and perform the shutdown process. Subsequent
	 * invocations are directly returned.
	 * @param time The <code>Long</code> time value.
	 * @param unit The <code>TimeUnit</code> of the value.
	 * @throws InterruptedException If waiting process is
	 * interrupted.
	 */
	public void forceShutdown(final long time, final TimeUnit unit) throws InterruptedException;
	
	/**
	 * Submit the given event task for execution.
	 * <p>
	 * This method internally performs load balancing
	 * with all assist executors automatically using
	 * work-stealing technique.
	 * <p>
	 * Invocations of this method has three possible
	 * outcomes:
	 * 1. If there is an available executor to handle
	 * the task, the task is assigned to the executor
	 * and the invocation returns immediately.
	 * 2. If there are no executors available and the
	 * maximum executor count has not been reached,
	 * a new on-demand executor is created and the
	 * task is assigned to the executor. Then the
	 * invocation returns without blocking.
	 * 3. If there are no executors available and the
	 * maximum executor count has been reached, this
	 * invocation blocks until an executor becomes
	 * available to accept the task, at which time,
	 * the invocation returns.
	 * @param task The <code>IEventTask</code> to be
	 * submitted.
	 * @return The <code>IEventTaskHandle</code> instance.
	 */
	public IEventTaskHandle submit(final IEventTask task) throws InterruptedException;

	/**
	 * Submit the given result task for execution.
	 * <p>
	 * This method internally performs load balancing
	 * with all assist executors automatically using
	 * work-stealing technique.
	 * <p>
	 * Invocations of this method has three possible
	 * outcomes:
	 * 1. If there is an available executor to handle
	 * the task, the task is assigned to the executor
	 * and the invocation returns immediately.
	 * 2. If there are no executors available and the
	 * maximum executor count has not been reached,
	 * a new on-demand executor is created and the
	 * task is assigned to the executor. Then the
	 * invocation returns without blocking.
	 * 3. If there are no executors available and the
	 * maximum executor count has been reached, this
	 * invocation blocks until an executor becomes
	 * available to accept the task, at which time,
	 * the invocation returns.
	 * @param <V> The result task result return type.
	 * @param task The <code>IResultTask</code> to be
	 * submitted.
	 * @return The <code>IResultTaskHandle</code> instance.
	 */
	public <V> IResultTaskHandle<V> submit(final IResultTask<V> task) throws InterruptedException;
	
	/**
	 * Retrieve the current number of executors in the
	 * service.
	 * @return The <code>int</code> number of executors.
	 */
	public int getCurrentExecutorCount();

	/**
	 * Retrieve the averaged executor (thread) count
	 * per processing core. This value is calculated
	 * using the total number of executors in the
	 * system divide by the total number of processing
	 * cores.
	 * @return The <code>double</code> average executor
	 * count per processing core.
	 */
	public double getExecutorCountPerCore();
}
