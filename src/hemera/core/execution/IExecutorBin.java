package hemera.core.execution;

import java.util.concurrent.TimeUnit;

import hemera.core.execution.interfaces.ICyclicExecutor;
import hemera.core.execution.interfaces.IRecyclableExecutor;
import hemera.core.execution.interfaces.task.ICyclicTask;
import hemera.core.execution.interfaces.task.IEventTask;
import hemera.core.execution.interfaces.task.IResultTask;
import hemera.core.execution.interfaces.task.handle.ICyclicTaskHandle;
import hemera.core.execution.interfaces.task.handle.IEventTaskHandle;
import hemera.core.execution.interfaces.task.handle.IResultTaskHandle;

/**
 * <code>IExecutorBin</code> defines the interface of
 * an intermediate communication data structure that
 * is designed to allow recyclable executors to be re-
 * used or disposed when they complete their assigned
 * tasks.
 * <p>
 * <code>IExecutorBin</code> is only an internal data
 * structure interface that may change if more types
 * of recyclable executors are added to the system. It
 * is by no means a published interface and should
 * never be used outside the scope of the internal
 * execution service structure.
 * <p>
 * <code>IExecutorBin</code> provides various atomic
 * reuse-polling and task assignment operations for all
 * supported task types. If the polling and assignment
 * operation was not atomic, the executor will attempt
 * to directly recycle itself after being waken up from
 * disposal wait, since assignment operation has not
 * completed. The atomic operations provided by this
 * interface prevents it from occurring. These atomic
 * operations are provided with very little locking to
 * allow maximum concurrency.
 * <p>
 * <code>IExecutorBin</code> provides the functionality
 * to automatically dispose recycled executors after
 * they have stayed in the executor bin for a defined
 * amount of time, the expiration time.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
interface IExecutorBin {
	
	/**
	 * Shutdown the executor bin to prevent further
	 * recycling. This method also wakes up all the
	 * blocking recycled executors and removes them
	 * from the recycle bins.
	 * <p>
	 * This method should be invoked after all the
	 * active executors have already been terminated.
	 */
	public void shutdown();

	/**
	 * Recycle the given cyclic executor for new task
	 * assignment or disposal.
	 * <p>
	 * This method should be directly invoked by the
	 * executor that is being recycled, since this
	 * method will block for the disposal expiration
	 * then invoke <code>terminate</code> on the given
	 * executor instance. In other words, the argument
	 * passed in when invoking should always be
	 * <code>this</code>.
	 * @param executor The <code>ICyclicExecutor</code>
	 * to be recycled.
	 * @return <code>true</code> if executor is recycled
	 * with a new task assigned. <code>false</code> if
	 * it is disposed and terminated.
	 */
	public boolean recycle(final ICyclicExecutor executor);

	/**
	 * Recycle the given recyclable executor for new
	 * task assignment or disposal.
	 * <p>
	 * This method should be directly invoked by the
	 * executor that is being recycled, since this
	 * method will block for the disposal expiration
	 * then invoke <code>terminate</code> on the given
	 * executor instance. In other words, the argument
	 * passed in when invoking should always be
	 * <code>this</code>.
	 * @param executor The <code>IRecyclableExecutor</code>
	 * to be recycled.
	 * @return <code>true</code> if executor is recycled
	 * with a new task assigned. <code>false</code> if
	 * it is disposed and terminated.
	 */
	public boolean recycle(final IRecyclableExecutor executor);
	
	/**
	 * Atomically poll a recycled cyclic executor from
	 * the recycle bin and assign it with the given task.
	 * <p>
	 * There is very little locking performed to ensure
	 * maximum concurrency.
	 * @return The <code>ICyclicTaskHandle</code> unit
	 * if recycled assign succeeded. <code>null</code>
	 * if there is no available recycled executors or
	 * bin has been shut down.
	 */
	public ICyclicTaskHandle pollAssign(final ICyclicTask task);
	
	/**
	 * Atomically poll a recycled background executor
	 * from the recycle bin and assign it with the
	 * given event task.
	 * <p>
	 * There is very little locking performed to ensure
	 * maximum concurrency.
	 * @param task The <code>IEventTask</code> to be
	 * assigned.
	 * @return The <code>IEventTaskHandle</code> unit
	 * if recycled assign succeeded. <code>null</code>
	 * if there is no available recycled executors or
	 * bin has been shut down.
	 */
	public IEventTaskHandle pollAssign(final IEventTask task);
	
	/**
	 * Atomically poll a recycled background executor
	 * from the recycle bin and assign it with the
	 * given result task.
	 * <p>
	 * There is very little locking performed to ensure
	 * maximum concurrency.
	 * @param V The result type of the result task.
	 * @param task The <code>IResultTask</code> to be
	 * assigned.
	 * @return The <code>IResultTaskHandle</code> unit
	 * if recycled assign succeeded. <code>null</code>
	 * if there is no available recycled executors or
	 * bin has been shut down.
	 */
	public <V> IResultTaskHandle<V> pollAssign(final IResultTask<V> task);
	
	/**
	 * Set the expiration time to the given value in
	 * given time unit.
	 * @param value The <code>Long</code> value to be
	 * set.
	 * @param unit The <code>TimeUnit</code> the value
	 * is in.
	 */
	public void setExpirationTime(final long value, final TimeUnit unit);
	
	/**
	 * Retrieve the current disposal expiration time
	 * limit in the given time unit.
	 * @param unit The <code>TimeUnit</code> to have
	 * the returned value in.
	 * @return The <code>Long</code> current disposal
	 * expiration time limit in given unit.
	 */
	public long getExpirationTime(final TimeUnit unit);
}
