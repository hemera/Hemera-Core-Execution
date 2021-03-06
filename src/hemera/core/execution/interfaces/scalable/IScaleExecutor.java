package hemera.core.execution.interfaces.scalable;

import hemera.core.execution.interfaces.IExecutor;
import hemera.core.execution.interfaces.task.ICyclicTask;
import hemera.core.execution.interfaces.task.IEventTask;
import hemera.core.execution.interfaces.task.IResultTask;
import hemera.core.execution.interfaces.task.handle.ICyclicTaskHandle;
import hemera.core.execution.interfaces.task.handle.IEventTaskHandle;
import hemera.core.execution.interfaces.task.handle.IResultTaskHandle;

/**
 * <code>IScaleExecutor</code> defines an executor
 * extension that is used in a scalable execution
 * service. It only allows a single task to be
 * assigned to it at any given time. New tasks can
 * be assigned after the current task is completed.
 * <p>
 * <code>IScaleExecutor</code> only executes assigned
 * task once. It internally handles discarding of
 * executed task automatically. Each assigned task is
 * guaranteed to be executed once and once only.
 * <p>
 * <code>IScaleExecutor</code> is constructed with a
 * shared instance of <code>IScalableService</code>
 * that allows the executor to be recycled back into
 * the pool once it finishes executing its assigned
 * task.
 * <p>
 * An instance of <code>IScaleExecutor</code> can
 * be an on-demand executor. This means that the
 * instance is created when more tasks are submitted
 * to the service than the number of executors that
 * are currently available to handle them. If an
 * executor is an on-demand executor, it will be
 * terminated after it completes its assigned task
 * and returns to the pool, and a defined timeout
 * period elapses without any new tasks assigned to
 * it. This allows the pool to grow and shrink back
 * to its minimum size when demand reduces.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface IScaleExecutor extends IExecutor {
	
	/**
	 * Assign the given event task to this executor
	 * if and only if there is no existing assigned
	 * task yet.
	 * <p>
	 * This method guarantees its thread safety by
	 * delegating synchronization mechanism down to
	 * its thread safe internal data structures.
	 * @param task The <code>IEventTask</code> to be
	 * executed.
	 * @return The <code>IEventTaskHandle</code> for
	 * the assigned event task.
	 * @throws IllegalStateException If there is a
	 * task assigned to this executor already.
	 */
	public IEventTaskHandle assign(final IEventTask task) throws IllegalStateException;
	
	/**
	 * Assign the given cyclic task to this executor
	 * if and only if there is no existing assigned
	 * task yet.
	 * <p>
	 * This method guarantees its thread safety by
	 * delegating synchronization mechanism down to
	 * its thread safe internal data structures.
	 * @param task The <code>ICyclicTask</code> to be
	 * executed.
	 * @return The <code>ICyclicTaskHandle</code> for
	 * the assigned cyclic task.
	 * @throws IllegalStateException If there is a
	 * task assigned to this executor already.
	 */
	public ICyclicTaskHandle assign(final ICyclicTask task) throws IllegalStateException;
	
	/**
	 * Assign the given result task to this executor
	 * if and only if there is no existing assigned
	 * task yet.
	 * <p>
	 * This method guarantees its thread safety by
	 * delegating synchronization mechanism down to
	 * its thread safe internal data structures.
	 * @param V The result task result type.
	 * @param task The <code>IResultTask</code> to be
	 * executed.
	 * @return The <code>IResultTaskHandle</code> for
	 * the assigned result task.
	 * @throws IllegalStateException If there is a
	 * task assigned to this executor already.
	 */
	public <V> IResultTaskHandle<V> assign(final IResultTask<V> task) throws IllegalStateException;

	/**
	 * Check if this instance of executor is an on-
	 * demand instance.
	 * @return <code>true</code> if the instance is
	 * an on-demand instance. <code>false</code>
	 * otherwise.
	 */
	public boolean isOndemand();
}
