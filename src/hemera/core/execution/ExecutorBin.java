package hemera.core.execution;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import hemera.core.execution.interfaces.ICyclicExecutor;
import hemera.core.execution.interfaces.IExecutor;
import hemera.core.execution.interfaces.IRecyclableExecutor;
import hemera.core.execution.interfaces.task.ICyclicTask;
import hemera.core.execution.interfaces.task.IEventTask;
import hemera.core.execution.interfaces.task.IResultTask;
import hemera.core.execution.interfaces.task.handle.ICyclicTaskHandle;
import hemera.core.execution.interfaces.task.handle.IEventTaskHandle;
import hemera.core.execution.interfaces.task.handle.IResultTaskHandle;
import hemera.core.execution.interfaces.task.handle.ITaskHandle;

/**
 * <code>ExecutorBin</code> defines the implementation
 * of the internal recyclable executor recycle bin data
 * structure.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
class ExecutorBin implements IExecutorBin {
	/**
	 * The <code>Queue</code> of <code>RecycledExecutor</code>
	 * for <code>ICyclicExecutor</code>.
	 * <p>
	 * This data structure needs to support concurrent
	 * modification operations since it may be modified
	 * by multiple threads concurrently to poll and add
	 * elements.
	 * <p>
	 * The queue data structure is chosen because the
	 * FIFO ordering property needs to be guaranteed as
	 * the oldest recycled executor should be polled off
	 * first to minimize disposal rate.
	 */
	private final Queue<RecycledExecutor<ICyclicExecutor>> cyclicBin;
	/**
	 * The <code>Queue</code> of <code>RecycledExecutor</code>
	 * for <code>IRecyclableExecutor</code>.
	 * <p>
	 * This data structure needs to support concurrent
	 * modification operations since it may be modified
	 * by multiple threads concurrently to poll and add
	 * elements.
	 * <p>
	 * The queue data structure is chosen because the
	 * FIFO ordering property needs to be guaranteed as
	 * the oldest recycled executor should be polled off
	 * first to minimize disposal rate.
	 */
	private final Queue<RecycledExecutor<IRecyclableExecutor>> backgroundBin;
	/**
	 * The <code>Long</code> disposal expiration time
	 * limit in nanoseconds.
	 * <p>
	 * The memory visibility of this value needs to be
	 * guaranteed since concurrent read/write operations
	 * may occur.
	 */
	private volatile long expiration;
	/**
	 * The <code>Boolean</code> shutdown flag.
	 * <p>
	 * The memory visibility of this flag needs to be
	 * guaranteed since writing and reading may be
	 * performed in different threads.
	 */
	private volatile boolean shutdown;
	
	/**
	 * Constructor of <code>ExecutorBin</code>.
	 * @param expiration The <code>Long</code> disposal
	 * expiration time limit in nanoseconds.
	 */
	ExecutorBin(final long expiration) {
		this.cyclicBin = new ConcurrentLinkedQueue<RecycledExecutor<ICyclicExecutor>>();
		this.backgroundBin = new ConcurrentLinkedQueue<RecycledExecutor<IRecyclableExecutor>>();
		this.expiration = expiration;
		this.shutdown = false;
	}

	@Override
	public void shutdown() {
		this.shutdown = true;
		this.wakeAll(this.cyclicBin);
		this.wakeAll(this.backgroundBin);
	}
	
	/**
	 * Wake up all the recycled executor from their
	 * recycle blocking mode in the given bin and
	 * remove them from the bin.
	 * @param E The <code>IExecutor</code> type.
	 * @param bin The <code>Queue</code> of recycle
	 * bin of <code>RecycledExecutor</code>.
	 */
	private <E extends IExecutor> void wakeAll(final Queue<RecycledExecutor<E>> bin) {
		while(!bin.isEmpty()) {
			final RecycledExecutor<E> executor = bin.poll();
			if(executor != null) executor.signalShutdown();
		}
	}

	@Override
	public boolean recycle(final ICyclicExecutor executor) {
		if(executor == null) throw new IllegalArgumentException("Given argument must be \"this\"");
		// Early return check to avoid object creation.
		// Statuses are checked again before entering
		// blocking mode.
		if(executor.hasTerminated() || this.shutdown) return false;
		final RecycledExecutor<ICyclicExecutor> recycled = new RecycledCyclicExecutor(executor);
		return this.recycle(recycled, this.cyclicBin);
	}

	@Override
	public boolean recycle(final IRecyclableExecutor executor) {
		if(executor == null) throw new IllegalArgumentException("Given argument must be \"this\"");
		// Early return check to avoid object creation.
		// Statuses are checked again before entering
		// blocking mode.
		if(executor.hasTerminated() || this.shutdown) return false;
		final RecycledExecutor<IRecyclableExecutor> recycled = new RecycledRecyclableExecutor(executor);
		return this.recycle(recycled, this.backgroundBin);
	}
	
	/**
	 * Recycle the given recycled executor with the
	 * given recycle executor bin.
	 * @param E The <code>IExecutor</code> type.
	 * @param recycled The <code>RecycledExecutor</code>.
	 * @param bin The appropriate <code>Queue</code> of
	 * recycle bin for the executor.
	 * @return <code>true</code> if executor is recycled
	 * with a new task assigned. <code>false</code> if
	 * it is disposed and terminated.
	 */
	private <E extends IExecutor> boolean recycle(final RecycledExecutor<E> recycled, final Queue<RecycledExecutor<E>> bin) {
		bin.add(recycled);
		// Try to dispose.
		final boolean disposed = recycled.tryDispose();
		// Remove from bin.
		bin.remove(recycled);
		return !disposed;
	}

	@Override
	public ICyclicTaskHandle pollAssign(final ICyclicTask task) {
		return (ICyclicTaskHandle)this.pollAssign(this.cyclicBin, task);
	}

	@Override
	public IEventTaskHandle pollAssign(final IEventTask task) {
		return (IEventTaskHandle)this.pollAssign(this.backgroundBin, task);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <V> IResultTaskHandle<V> pollAssign(final IResultTask<V> task) {
		return (IResultTaskHandle<V>)this.pollAssign(this.backgroundBin, task);
	}

	/**
	 * Poll the first successful signaled recycled
	 * executor from given queue.
	 * @param E The <code>IExecutor</code> type.
	 * @param queue The <code>Queue</code> of the
	 * <code>RecycledExecutor</code> to be polled from.
	 * @param task The <code>Object</code> task to be
	 * assigned.
	 * @return The <code>ITaskHandle</code> if recycle
	 * assign succeeded. <code>null</code> if there is
	 * no available recycled executors.
	 */
	private <E extends IExecutor> ITaskHandle pollAssign(final Queue<RecycledExecutor<E>> queue, final Object task) {
		if(this.shutdown) return null;
		// Keep trying until assignment succeeds.
		while(!queue.isEmpty()) {
			final RecycledExecutor<E> recycled = queue.poll();
			final ITaskHandle handle = recycled.tryAssign(task);
			if(handle != null) return handle;
		}
		return null;
	}
	
	@Override
	public void setExpirationTime(final long value, final TimeUnit unit) {
		if(value < 0) {
			this.expiration = 0;
			return;
		}
		if(unit == null) throw new IllegalArgumentException("Time unit cannot be null.");
		this.expiration = unit.toNanos(value);
	}
	
	@Override
	public long getExpirationTime(final TimeUnit unit) {
		return unit.convert(this.expiration, TimeUnit.NANOSECONDS);
	}

	/**
	 * <code>RecycledExecutor</code> defines the internal
	 * implementation of an utility structure that contains
	 * a recycled executor. It provides the functionality
	 * to try to dispose the executor and recover if the
	 * waiting is interrupted.
	 * <p>
	 * Cannot infer generic typing on task type due to
	 * the fact that a <code>IRecyclableExecutor</code>
	 * has two distinct task types associated with.
	 * <p>
	 * @param E The executor type.
	 *
	 * @author Yi Wang (Neakor)
	 * @version Creation date: 12-01-2009 23:21 EST
	 * @version Modified date: 01-03-2010 22:50 EST
	 */
	private abstract class RecycledExecutor<E extends IExecutor> {
		/**
		 * The recycled <code>E</code> executor.
		 */
		protected final E executor;
		/**
		 * The disposal <code>Lock</code>.
		 */
		private final Lock disposalLock;
		/**
		 * The disposal blocking <code>Condition</code>.
		 */
		private final Condition disposalCondition;
		/**
		 * The <code>Boolean</code> signaled flag.
		 * <p>
		 * This value is guarded by the disposal lock.
		 */
		private boolean signaled;
		
		/**
		 * Constructor of <code>RecycledExecutor</code>.
		 * @param executor The recycled <code>E</code>.
		 */
		private RecycledExecutor(final E executor) {
			this.executor = executor;
			this.disposalLock = new ReentrantLock();
			this.disposalCondition = this.disposalLock.newCondition();
			this.signaled = false;
		}
		
		/**
		 * Signal blocking executors to wake up. This
		 * method should only be used to wake executor
		 * from its disposal blocking mode due to the
		 * system shutdown.
		 */
		private void signalShutdown() {
			this.disposalLock.lock();
			try {
				this.signaled = true;
				this.disposalCondition.signalAll();
			} finally {
				this.disposalLock.unlock();
			}
		}

		/**
		 * Try to dispose the recycled executor and put
		 * the calling thread in blocking mode for the
		 * expiration duration.
		 * @return <code>true</code> if executor is
		 * disposed and terminated. <code>false</code>
		 * if it is recycled for new task assignment.
		 */
		private boolean tryDispose() {
			boolean disposed = false;
			this.disposalLock.lock();
			try {
				try {
					// Sleep unless already signaled, shutdown
					// or terminated.
					if(!this.signaled && !shutdown && !this.executor.hasTerminated()) {
						this.disposalCondition.await(expiration, TimeUnit.NANOSECONDS);
					}
					// Cannot depend on returned boolean flag, since
					// time may elapse during signaling process.
					// If not signaled, terminate.
					if(!this.signaled) {
						this.executor.terminate();
						disposed = true;
					}
				} catch (InterruptedException e) {
					// Should not occur.
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} finally {
				this.disposalLock.unlock();
			}
			return disposed;
		}
		
		/**
		 * Try assign the given task to this recycled
		 * executor, thus interrupting the disposal
		 * blocking.
		 * @param task The <code>Object</code> task to assign
		 * to the recycled executor.
		 * @return The <code>ITaskHandle</code> if recycle
		 * assign succeeded. <code>null</code> if there is
		 * already a task assigned or the executor has been
		 * terminated.
		 */
		private ITaskHandle tryAssign(final Object task) {
			this.disposalLock.lock();
			try {
				// If terminated before this lock is acquired.
				if(this.executor.hasTerminated()) return null;
				// Set flag in case this method is invoked before
				// wait started or elapsed in between signaling
				// process.
				this.signaled = true;
				// Signal and assign atomically.
				this.disposalCondition.signalAll();
				return this.assign(task);
			} finally {
				this.disposalLock.unlock();
			}
		}
		
		/**
		 * Assign the given task appropriately based
		 * on the executor type.
		 * @param task The <code>Object</code> task to assign
		 * to the recycled executor.
		 * @return The <code>ITaskHandle</code> if recycle
		 * assign succeeded. <code>null</code> if executor
		 * has been shut down.
		 */
		protected abstract ITaskHandle assign(final Object task);
	}
	
	/**
	 * <code>RecycledCyclicExecutor</code> defines the
	 * internal data structure that contains the recycled
	 * <code>ICyclicExecutor</code>.
	 *
	 * @author Yi Wang (Neakor)
	 * @version Creation date: 12-03-2009 13:22 EST
	 * @version Modified date: 01-03-2010 22:52 EST
	 */
	private final class RecycledCyclicExecutor extends RecycledExecutor<ICyclicExecutor> {
		
		/**
		 * Constructor of <code>RecycledCyclicExecutor</code>.
		 * @param executor The recycled <code>ICyclicExecutor</code>.
		 */
		private RecycledCyclicExecutor(final ICyclicExecutor executor) {
			super(executor);
		}

		@Override
		protected ITaskHandle assign(final Object task) {
			return this.executor.assign((ICyclicTask)task);
		}
	}
	
	/**
	 * <code>RecycledRecyclableExecutor</code> defines an
	 * internal data structure that contains the recycled
	 * <code>IRecyclableExecutor</code>.
	 *
	 * @author Yi Wang (Neakor)
	 * @version Creation date: 12-03-2009 13:23 EST
	 * @version Modified date: 01-03-2010 22:53 EST
	 */
	private final class RecycledRecyclableExecutor extends RecycledExecutor<IRecyclableExecutor> {
		
		/**
		 * Constructor of <code>RecycledRecyclableExecutor</code>.
		 * @param executor The recycled <code>IRecyclableExecutor</code>.
		 */
		private RecycledRecyclableExecutor(final IRecyclableExecutor executor) {
			super(executor);
		}

		@Override
		protected ITaskHandle assign(final Object task) {
			if(task instanceof IEventTask) return this.executor.assign((IEventTask)task);
			else if(task instanceof IResultTask<?>) return this.executor.assign((IResultTask<?>)task);
			throw new IllegalArgumentException("Invalid task type: ".concat(task.getClass().getName()));
		}
	}
}
