package hemera.core.execution.interfaces.scalable;

import hemera.core.execution.interfaces.IExecutionService;
import hemera.core.execution.interfaces.task.IEventTask;
import hemera.core.execution.interfaces.task.IResultTask;
import hemera.core.execution.interfaces.task.handle.IEventTaskHandle;
import hemera.core.execution.interfaces.task.handle.IResultTaskHandle;

/**
 * <code>IScalableService</code> is an extension of
 * <code>IExecutionService</code> that manages a pool
 * of <code>IScaleExecutor</code> that can scale the
 * its size based on the current load of the service.
 * When there are more tasks submitted than the
 * current amount of executors can handle, new on-
 * demand executors are created to handle the new
 * tasks. Once the demand dies down, the created on-
 * demand executors will terminate after a timeout
 * period elapses.
 * <p>
 * This design is best suited for tasks that are I/O
 * bound rather than CPU bound. In such cases, by
 * growing the executor pool, newly submitted tasks
 * can be executed while other tasks submitted to
 * other executors are blocked by I/O operations. By
 * increasing the pool size, more tasks can be handled
 * concurrently, thus improving system performance.
 * <p>
 * <code>IScalableService</code> has a defined minimum
 * executor count and maximum limit. The minimum count
 * defines the initial set of executors the service
 * will maintain always. As demand grows, on-demand
 * executors are created up to the limited amount. When
 * demand tapers off, created on-demand executors are
 * terminated until only the initial set of executors
 * remain.
 * <p>
 * In <code>IScalableService</code>, each executor is
 * only assigned with a single ask at any given time.
 * An executor will only accept another task after it
 * completes the execution of the currently assigned
 * task. Since the pool has a defined upper bound for
 * the number of executors, the service will block new
 * task submissions once the upper limit is reached.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface IScalableService extends IExecutionService {
	
	/**
	 * Recycle the given executor back to the pool
	 * for more task assignment.
	 * @param executor The <code>IScaleExecutor</code>
	 * to recycle.
	 */
	public void recycle(final IScaleExecutor executor);
	
	/**
	 * Remove the given executor from the executor pool.
	 * <p>
	 * This method should only be used internally to
	 * remove a timed-out on-demand executor.
	 * @param executor The <code>IScaleExecutor</code>
	 * to remove.
	 * @return The <code>true</code> if given executor
	 * is an on-demand executor and has been removed.
	 * <code>false</code> if the executor has been polled
	 * for a new task assignment already, or the executor
	 * does not belong to the service or it is not an
	 * on-demand instance.
	 */
	public boolean remove(final IScaleExecutor executor);
	
	/**
	 * Submit the given event task for execution.
	 * <p>
	 * This method uses a basic round-robin assignment
	 * strategy to provide load-balancing.
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
	 * @return The <code>IEventTaskHandle</code> of
	 * the submitted task.
	 */
	public IEventTaskHandle submit(final IEventTask task);

	/**
	 * Submit the given result task for execution.
	 * <p>
	 * This method uses a basic round-robin assignment
	 * strategy to provide load-balancing.
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
	 * @return The <code>IResultTaskHandle</code> of
	 * the submitted task.
	 */
	public <V> IResultTaskHandle<V> submit(final IResultTask<V> task);
	
	/**
	 * Retrieve the number of available executors to
	 * handle new tasks in the service.
	 * @return The <code>int</code> number of available
	 * executors.
	 */
	public int getAvailableCount();
}
