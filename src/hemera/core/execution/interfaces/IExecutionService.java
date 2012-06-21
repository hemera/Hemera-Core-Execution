package hemera.core.execution.interfaces;

import java.util.concurrent.TimeUnit;

import hemera.core.execution.interfaces.task.ICyclicTask;
import hemera.core.execution.interfaces.task.IEventTask;
import hemera.core.execution.interfaces.task.IResultTask;
import hemera.core.execution.interfaces.task.handle.ICyclicTaskHandle;
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
	 * See type specific service interface for detailed
	 * method behavior.
	 * @param task The <code>IEventTask</code> to be
	 * submitted.
	 * @return The <code>IEventTaskHandle</code> of
	 * the submitted task.
	 */
	public IEventTaskHandle submit(final IEventTask task);
	
	/**
	 * Submit the given cyclic task for repeated task
	 * execution.
	 * <p>
	 * See type specific service interface for detailed
	 * method behavior.
	 * @param task The <code>ICyclicTask</code> to be
	 * submitted.
	 * @return The <code>ICyclicTaskHandle</code> of
	 * the submitted task.
	 */
	public ICyclicTaskHandle submit(final ICyclicTask task);

	/**
	 * Submit the given result task for execution.
	 * <p>
	 * See type specific service interface for detailed
	 * method behavior.
	 * @param <V> The result task result return type.
	 * @param task The <code>IResultTask</code> to be
	 * submitted.
	 * @return The <code>IResultTaskHandle</code> of
	 * the submitted task.
	 */
	public <V> IResultTaskHandle<V> submit(final IResultTask<V> task);
	
	/**
	 * Retrieve the current number of executors in the
	 * service.
	 * @return The <code>int</code> number of executors.
	 */
	public int getCurrentExecutorCount();
	
	/**
	 * Retrieve the exception handler used by the
	 * execution service to handle exceptions that
	 * occur during task executions.
	 * @return The <code>IExceptionHandler</code>.
	 */
	public IExceptionHandler getExceptionHandler();
}
