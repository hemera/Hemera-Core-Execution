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
 * of the centralized execution service responsible
 * for the construction and maintenance of all executor
 * instances. It should be considered as a single entry
 * point to task execution for the rest of the system.
 * There should not be any direct accesses to individual
 * executor units or native threads.
 * <p>
 * <code>IExecutionService</code> supports submission
 * of all the defined types of tasks. Tasks submitted
 * as event and result tasks are executed once only,
 * where as tasks submitted as cyclic tasks are
 * automatically executed repeatedly every execution
 * cycle until canceled or system shut down.
 * <p>
 * <code>IExecutionService</code> supports submission
 * of event and result tasks as either foreground or
 * background tasks. However, all cyclic tasks are
 * treated in the same way. They are not distinguished
 * as either foreground or background tasks.
 * <p>
 * Event and result tasks submitted to the execution
 * service as foreground tasks are assigned to a fixed
 * set of assist executors based on load-balancing
 * information. Multiple tasks may be executed by a
 * single assist executor. The execution service
 * internally utilizes assignment balancing using a
 * round-robin strategy and execution time work-
 * stealing to provide automated load balancing for
 * these tasks thus task assignments are not final.
 * In other words, tasks assigned to one executor may
 * actually be executed by a different executor due
 * to load-balancing issues.
 * <p>
 * Event and result Tasks submitted as background tasks
 * are assigned with their own dedicated background
 * executors. Execution service internally performs
 * recycling of background executors to minimize the
 * overhead. There is no work-stealing performed on
 * background tasks as they require dedicated thread
 * execution. Due to the on-demand background executor
 * creation and disposal, the submission process of
 * background tasks is sightly more expensive than the
 * foreground one. Also background task execution may
 * consume more system resources than foreground type.
 * <p>
 * Cyclic tasks submitted are assigned with their own
 * dedicated cyclic executors. The execution service
 * internally performs recycling of cyclic executors
 * to minimize overhead. There is no work-stealing
 * performed on cyclic tasks as they require dedicated
 * thread execution. Due to on-demand cyclic executor
 * creation and recycling, the submission process of
 * cyclic tasks is sightly slower than the foreground
 * task submission. Also cyclic task execution consumes
 * more system resources.
 * <p>
 * <code>IExecutionService</code> returns result handles
 * upon successful task submissions. These handles allow
 * the caller to perform various task maintenance
 * operations. Depending on the type of the submitted
 * task, the returned handle provides task type specific
 * handling functionalities. See various task handles
 * for detailed documentation.
 * <p>
 * To ensure that all executor threads do not crash due
 * to <code>Exception</code>, execution service requires
 * an <code>IExceptionHandler</code> instance at the
 * construction time so it can pass this handler unit
 * to all executors allowing them to gracefully handle
 * exceptions at runtime. The exception handler unit is
 * automatically registered as a system shutdown hook.
 * <p>
 * <code>IExecutionService</code> internally coordinates
 * cyclic and background executors disposal process
 * dynamically at runtime. It adjusts the disposal time
 * limit based on runtime profiling of disposal rates in
 * an attempt to minimize the number of disposals.
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
	 * Submit the given task as a cyclic task that is
	 * executed by a dedicated executor every execution
	 * cycle, until canceled or system shutdown.
	 * <p>
	 * This submission process may be relatively more
	 * expensive than foreground event or result task
	 * submission process due to the fact that a new
	 * executor may be created if there isn't one
	 * available at the time of submission. Moreover,
	 * cyclic task execution requires more memory
	 * resources than foreground event or result tasks,
	 * since a dedicated executor instance is required.
	 * <p>
	 * Invocations of this method returns immediately
	 * without executing the task in the caller thread.
	 * The task will be executed by an executor thread.
	 * @param task The <code>ICyclicTask</code> to be
	 * submitted.
	 * @return The <code>ICyclicTaskHandle</code> instance.
	 */
	public ICyclicTaskHandle submit(final ICyclicTask task);
	
	/**
	 * Submit the given event task for execution in
	 * the foreground. The task is executed once and
	 * discarded.
	 * <p>
	 * This method internally performs load balancing
	 * with all assist executors automatically using
	 * work-stealing technique.
	 * <p>
	 * Invocations of this method returns immediately
	 * without executing the task in the caller thread.
	 * The task will be executed by an executor thread.
	 * @param task The <code>IEventTask</code> to be
	 * submitted.
	 * @return The <code>IEventTaskHandle</code> instance.
	 */
	public IEventTaskHandle submitForeground(final IEventTask task);
	
	/**
	 * Submit the given event task for execution in
	 * the background. The task is executed once and
	 * discarded.
	 * <p>
	 * This submission process may be relatively more
	 * expensive than foreground event task submission
	 * process due to the fact that a new executor
	 * may be created if there isn't one available at
	 * the time of submission. Moreover, background
	 * event task execution requires more memory
	 * resources than foreground event tasks, since a
	 * dedicated executor instance is required.
	 * <p>
	 * Invocations of this method returns immediately
	 * without executing the task in the caller thread.
	 * The task will be executed by an executor thread.
	 * @param task The <code>IEventTask</code> to be
	 * submitted.
	 * @return The <code>IEventTaskHandle</code> instance.
	 */
	public IEventTaskHandle submitBackground(final IEventTask task);

	/**
	 * Submit the given result task for execution in
	 * the foreground. The task is executed once and
	 * discarded.
	 * <p>
	 * This method internally performs load balancing
	 * with all assist executors automatically using
	 * work-stealing technique.
	 * <p>
	 * Invocations of this method returns immediately
	 * without executing the task in the caller thread.
	 * The task will be executed by an executor thread.
	 * @param <V> The result task result return type.
	 * @param task The <code>IResultTask</code> to be
	 * submitted.
	 * @return The <code>IResultTaskHandle</code> instance.
	 */
	public <V> IResultTaskHandle<V> submitForeground(final IResultTask<V> task);
	
	/**
	 * Submit the given result task for execution in
	 * the background. The task is executed once and
	 * discarded.
	 * <p>
	 * This submission process may be relatively more
	 * expensive than foreground result task submission
	 * process due to the fact that a new executor
	 * may be created if there isn't one available at
	 * the time of submission. Moreover, background
	 * result task execution requires more memory
	 * resources than foreground result tasks, since a
	 * dedicated executor instance is required.
	 * <p>
	 * Invocations of this method returns immediately
	 * without executing the task in the caller thread.
	 * The task will be executed by an executor thread.
	 * @param <V> The result task result return type.
	 * @param task The <code>IResultTask</code> to be
	 * submitted.
	 * @return The <code>IResultTaskHandle</code> instance.
	 */
	public <V> IResultTaskHandle<V> submitBackground(final IResultTask<V> task);
	
	/**
	 * Retrieve the current CPU workload in fraction
	 * format. 1.0 being full capacity and 0.0 being
	 * completely idle.
	 * @return The <code>double</code> current workload
	 * in fraction format between 0.0 to 1.0.
	 */
	public double getCPULoad();
	
	/**
	 * Retrieve the current memory usage in fraction
	 * format. 1.0 being full capacity and 0.0 being
	 * completely free.
	 * @return The <code>double</code> current usage
	 * in fraction format between 0.0 to 1.0.
	 */
	public double getMemoryUsage();
	
	/**
	 * Retrieve the average waiting task queue length
	 * of assist executors. This value is calculated
	 * using the total number of waiting tasks on all
	 * assist executors divide by the number of assist
	 * executors.
	 * @return The <code>double</code> average waiting
	 * task queue length.
	 */
	public double getAverageQueueLength();
	
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
