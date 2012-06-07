package hemera.core.execution;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import hemera.core.execution.cycle.AssistBarrier;
import hemera.core.execution.interfaces.IAssistExecutor;
import hemera.core.execution.interfaces.ICyclicExecutor;
import hemera.core.execution.interfaces.IExecutionService;
import hemera.core.execution.interfaces.IExecutor;
import hemera.core.execution.interfaces.IRecyclableExecutor;
import hemera.core.execution.interfaces.cycle.IAssistBarrier;
import hemera.core.execution.interfaces.exception.IExceptionHandler;
import hemera.core.execution.interfaces.task.ICyclicTask;
import hemera.core.execution.interfaces.task.IEventTask;
import hemera.core.execution.interfaces.task.IResultTask;
import hemera.core.execution.interfaces.task.handle.ICyclicTaskHandle;
import hemera.core.execution.interfaces.task.handle.IEventTaskHandle;
import hemera.core.execution.interfaces.task.handle.IResultTaskHandle;
import hemera.core.execution.interfaces.time.ITimeHandle;

/**
 * <code>ExecutionService</code> defines implementation
 * of the centralized threading management unit that is
 * responsible for task execution for the entire system.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public class ExecutionService implements IExecutionService {
	/**
	 * The <code>Integer</code> executor count.
	 */
	private final int count;
	/**
	 * The <code>IExceptionHandler</code> instance used
	 * to gracefully allow executors handle exceptions.
	 */
	private final IExceptionHandler handler;
	/**
	 * The <code>Integer</code> last executor index.
	 */
	private final int lastIndex;
	/**
	 * The array of <code>IAssistExecutor</code> for
	 * foreground event and result task execution.
	 * <p>
	 * The contents of this array should never be altered
	 * after service construction.
	 */
	private final IAssistExecutor[] assistExecutors;
	/**
	 * The <code>Queue</code> of all the currently active
	 * <code>IExecutor</code> instances.
	 * <p>
	 * This data structure needs to support concurrent
	 * modification operations, since the bookkeeping
	 * terminated executor removal cyclic task runs in
	 * a different thread then the new executor creation
	 * threads.
	 * <p>
	 * Since a majority of the operations performed on
	 * this data structure are modifications, this data
	 * structure should optimize for the modification
	 * operations performance.
	 */
	private final Queue<IExecutor> activeExecutors;
	/**
	 * The <code>IExecutorBin</code> for recycled cyclic
	 * and recyclable executors.
	 */
	private final IExecutorBin recycleBin;
	/**
	 * The <code>AtomicInteger</code> of next available
	 * executor index for event task.
	 * <p>
	 * This unit is used for automatic load balancing for
	 * event task submissions. Since multiple threads may
	 * perform submission operations concurrently, atomic
	 * check, compare and set operations are required.
	 */
	private final AtomicInteger assistIndex;
	/**
	 * The <code>AtomicInteger</code> of the number of
	 * cyclic executors have been created.
	 */
	private final AtomicInteger cyclicCount;
	/**
	 * The <code>AtomicInteger</code> of the number of
	 * recyclable executors have been created.
	 */
	private final AtomicInteger recyclableCount;
	/**
	 * The <code>AtomicBoolean</code> activated flag.
	 * <p>
	 * Since concurrent modification invocations may
	 * occur, atomic compare and set operation must
	 * be provided.
	 */
	private final AtomicBoolean activated;
	/**
	 * The <code>AtomicBoolean</code> shutdown flag.
	 * <p>
	 * Since concurrent modification invocations may
	 * occur, atomic compare and set operation must
	 * be provided.
	 */
	private final AtomicBoolean shutdown;
	/**
	 * The <code>OperatingSystemMXBean</code> instance
	 * for retrieving CPU workload information.
	 */
	private final OperatingSystemMXBean cpuInfo;
	/**
	 * The <code>MemoryMXBean</code> instance for the
	 * retrieval of memory usage information.
	 */
	private final MemoryMXBean memoryInfo;
	/**
	 * The <code>ICyclicTaskHandle</code> for recycling
	 * time limit monitoring cyclic task.
	 */
	private ICyclicTaskHandle monitorHandle;
	/**
	 * The <code>ICyclicTaskHandle</code> for exception
	 * handler cyclic task.
	 * <p>
	 * This value may be <code>null</code> as custom
	 * exception handler may be used.
	 */
	private ICyclicTaskHandle exceptionHandle;

	/**
	 * Constructor of <code>ExecutionService</code>.
	 * @param count The <code>Integer</code> desired
	 * number of assist executors. Typically this
	 * number should be equal to the number of
	 * processor cores on the running hardware.
	 * @param handler The <code>IExceptionHandler</code>
	 * instance.
	 */
	public ExecutionService(final int count, final IExceptionHandler handler) {
		// Count and exception handler check.
		if(count < 1) throw new IllegalArgumentException("Must have at least one foregournd event executor.");
		else if(handler == null) throw new IllegalArgumentException("Exception handler cannot be null.");
		// Initial values.
		this.count = count;
		this.handler = handler;
		this.lastIndex = this.count-1;
		this.assistExecutors = new IAssistExecutor[this.count];
		this.activeExecutors = new ConcurrentLinkedQueue<IExecutor>();
		// Initial expiration time set to 1 minute.
		this.recycleBin = new ExecutorBin(TimeUnit.MINUTES.toNanos(1));
		this.assistIndex = new AtomicInteger(0);
		this.cyclicCount = new AtomicInteger(0);
		this.recyclableCount = new AtomicInteger(0);
		this.activated = new AtomicBoolean(false);
		this.shutdown = new AtomicBoolean(false);
		this.cpuInfo = ManagementFactory.getOperatingSystemMXBean();
		this.memoryInfo = ManagementFactory.getMemoryMXBean();
		// Initialize assist executors.
		this.initAssistExecutors();
		// Eager initialize some cyclic and recyclable executors.
		this.initDedicatedExecutors();
		// Add exception handler as system shutdown hook.
		Runtime.getRuntime().addShutdownHook(new Thread(handler));
	}
	
	/**
	 * Initialize assist executors.
	 */
	private void initAssistExecutors() {
		final IAssistBarrier assist = new AssistBarrier(this.assistExecutors);
		for(int i = 0; i < this.count; i++) {
			final String name = "AssistExecutor-".concat(String.valueOf(i));
			final AssistExecutor executor = new AssistExecutor(name, handler, assist);
			this.assistExecutors[i] = executor;
			this.activeExecutors.add(executor);
		}
	}
	
	/**
	 * Initialize cyclic and recyclable executors.
	 */
	private void initDedicatedExecutors() {
		final int half = this.count==1 ? 1 : this.count/2;
		for(int i = 0; i < half; i++) {
			final String cyclicname = "CyclicExecutor-".concat(String.valueOf(i));
			final String recyclablename = "RecyclableExecutor-".concat(String.valueOf(i));
			final ICyclicExecutor cyclic = new CyclicExecutor(cyclicname, handler, this, this.recycleBin);
			final IRecyclableExecutor recyclable = new RecyclableExecutor(recyclablename, handler, this.recycleBin);
			this.activeExecutors.add(cyclic);
			this.activeExecutors.add(recyclable);
		}
		this.cyclicCount.set(half);
		this.recyclableCount.set(half);
	}

	@Override
	public void activate() {
		// Allow first invocation to pass.
		if(!this.activated.compareAndSet(false, true)) return;
		// Start all eager instantiated executors.
		for(final IExecutor executor : this.activeExecutors) executor.start();
		// Submit recycle monitor task.
		this.monitorHandle = this.submit(new RecycleMonitorTask());
		// If built-in exception handler, submit as cyclic.
		if(this.handler instanceof ICyclicTask) this.exceptionHandle = this.submit((ICyclicTask)this.handler);
		else this.exceptionHandle = null;
	}

	@Override
	public void shutdown() {
		if(!this.shutdownBasics()) return;
		// Gracefully terminate and remove all active
		// executors.
		while(!this.activeExecutors.isEmpty()) {
			final IExecutor executor = this.activeExecutors.poll();
			try {
				executor.terminate();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// Shut down recycle bin.
		this.recycleBin.shutdown();
	}
	
	@Override
	public void shutdownAndWait() throws InterruptedException {
		if(!this.shutdownBasics()) return;
		// Gracefully terminate all active executors.
		for(final IExecutor executor : this.activeExecutors) {
			try {
				executor.terminate();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// Shut down recycle bin.
		this.recycleBin.shutdown();
		// Remove and wait for all executor threads to
		// terminate.
		while(!this.activeExecutors.isEmpty()) {
			final Executor executor = (Executor)this.activeExecutors.poll();
			while(!executor.threadTerminated) {
				TimeUnit.MILLISECONDS.sleep(5);
			}
		}
	}

	@Override
	public void forceShutdown() {
		if(!this.shutdownBasics()) return;
		// Forcefully terminate and remove all active
		// executors.
		while(!this.activeExecutors.isEmpty()) {
			final IExecutor executor = this.activeExecutors.poll();
			try {
				executor.forceTerminate();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void forceShutdown(final long time, final TimeUnit unit) throws InterruptedException {
		if(!this.shutdownBasics()) return;
		// Gracefully terminate all active executors.
		for(final IExecutor executor : this.activeExecutors) {
			try {
				executor.terminate();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// Shut down recycle bin.
		this.recycleBin.shutdown();
		// Wait for expiration.
		unit.sleep(time);
		// Forcefully terminate and remove all active
		// executors.
		while(!this.activeExecutors.isEmpty()) {
			final IExecutor executor = this.activeExecutors.poll();
			try {
				executor.forceTerminate();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Shutdown the basic internal services and set the
	 * essential basic data fields.
	 * <p>
	 * This method only allows the very first invocation
	 * with a single thread to succeed. All subsequent
	 * invocations are rejected and returned.
	 */
	private boolean shutdownBasics() {
		if(!this.shutdown.compareAndSet(false, true)) return false;
		try {
			this.monitorHandle.cancel();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(this.exceptionHandle != null) {
			try {
				this.exceptionHandle.cancel();
			} catch (Exception e) {
				this.handler.handle(e);
			}
		}
		return true;
	}

	@Override
	public ICyclicTaskHandle submit(final ICyclicTask task) {
		this.exceptionCheck(task);
		// First try recycled.
		ICyclicTaskHandle handle = this.recycleBin.pollAssign(task);
		if(handle != null) return handle;
		// Otherwise create a new cyclic executor.
		final int i = this.cyclicCount.getAndIncrement();
		final String name = "CyclicExecutor-".concat(String.valueOf(i));
		final ICyclicExecutor executor = new CyclicExecutor(name, this.handler, this, this.recycleBin);
		this.activeExecutors.add(executor);
		handle = executor.assign(task);
		executor.start();
		return handle;
	}

	@Override
	public IEventTaskHandle submitForeground(final IEventTask task) {
		this.exceptionCheck(task);
		return this.nextAssistExecutor().assign(task);
	}

	@Override
	public IEventTaskHandle submitBackground(final IEventTask task) {
		this.exceptionCheck(task);
		// First try recycled.
		IEventTaskHandle handle = this.recycleBin.pollAssign(task);
		if(handle != null) return handle;
		// Otherwise create a new recyclable executor.
		final IRecyclableExecutor executor = this.newRecyclableExecutor();
		handle = executor.assign(task);
		executor.start();
		return handle;
	}

	@Override
	public <V> IResultTaskHandle<V> submitForeground(final IResultTask<V> task) {
		this.exceptionCheck(task);
		return this.nextAssistExecutor().assign(task);
	}

	@Override
	public <V> IResultTaskHandle<V> submitBackground(final IResultTask<V> task) {
		this.exceptionCheck(task);
		// First try recycled.
		IResultTaskHandle<V> handle = this.recycleBin.pollAssign(task);
		if(handle != null) return handle;
		// Otherwise create a new recyclable executor.
		final IRecyclableExecutor executor = this.newRecyclableExecutor();
		handle = executor.assign(task);
		executor.start();
		return handle;
	}
	
	/**
	 * Check all the exception causing status.
	 * @param <T> The task type.
	 * @param task The <code>T</code> task to be submitted.
	 */
	private <T> void exceptionCheck(final T task) {
		if(!this.activated.get()) throw new IllegalStateException("Service has not yet been activated.");
		else if(this.shutdown.get()) throw new IllegalStateException("Service has already been shutdown.");
		else if(task == null) throw new IllegalArgumentException("Task is null.");
	}

	/**
	 * Retrieve the next available assist executor
	 * using a round-robin strategy.
	 * <p>
	 * The retrieval logic is based on a circular
	 * assist executor selection process.
	 * @return The next <code>IEventExecutor</code>
	 * for task assignment.
	 */
	private IAssistExecutor nextAssistExecutor() {
		int value = this.assistIndex.getAndIncrement();
		// Loop until obtain a valid index.
		while(value > this.lastIndex) {
			// This set operation may be invoked multiple
			// times by multiple threads. Though it is still
			// cheaper than performing extra getAndIncrement
			// operations, which occurs if this set operation
			// is limited to the thread that holds the index
			// equal to count.
			this.assistIndex.set(0);
			// At this point, most threads will be able to
			// obtain a valid index number.
			value = this.assistIndex.getAndIncrement();
		}
		return this.assistExecutors[value];
	}
	
	/**
	 * Create a new recyclable executor and add it to
	 * the active executor queue. The newly created
	 * executor instance is not yet activated.
	 * @return The <code>IRecyclableExecutor</code>
	 * instance created.
	 */
	private IRecyclableExecutor newRecyclableExecutor() {
		final int i = this.recyclableCount.getAndIncrement();
		final String name = "RecyclableExecutor-".concat(String.valueOf(i));
		final IRecyclableExecutor executor = new RecyclableExecutor(name, this.handler, this.recycleBin);
		this.activeExecutors.add(executor);
		return executor;
	}
	
	@Override
	public double getCPULoad() {
		return this.cpuInfo.getSystemLoadAverage();
	}

	@Override
	public double getMemoryUsage() {
		final MemoryUsage heap = this.memoryInfo.getHeapMemoryUsage();
		final MemoryUsage nonheap = this.memoryInfo.getNonHeapMemoryUsage();
		final double used = heap.getUsed() + nonheap.getUsed();
		final double total = heap.getMax() + nonheap.getMax();
		final double fraction = used / total;
		return fraction;
	}

	@Override
	public double getAverageQueueLength() {
		final int length = this.assistExecutors.length;
		int total = 0;
		for(int i = 0; i <  length; i++) {
			total += this.assistExecutors[i].getQueueLength();
		}
		final double fraction = (double)total / (double)length;
		return fraction;
	}

	@Override
	public double getExecutorCountPerCore() {
		final double ecount = this.activeExecutors.size();
		final double pcount = this.cpuInfo.getAvailableProcessors();
		final double fraction = ecount / pcount;
		return fraction;
	}

	/**
	 * <code>RecycleMonitorTask</code> defines a internal
	 * bookkeeping cyclic task that is responsible for
	 * monitoring the disposal rate of recyclable and
	 * cyclic executors and adjusting the disposal time
	 * limit accordingly. It is also responsible for
	 * cleaning up the references of disposed executors
	 * to prevent memory leaks.
	 * <p>
	 * <code>RecycleMonitorTask</code> attempts to adjust
	 * the disposal time limit to minimize the number of
	 * executors disposed.
	 * <p>
	 * <code>RecycleMonitorTask</code> is a cyclic task,
	 * thus it is executed by a single dedicated executor
	 * thread. Therefore, no synchronization is needed.
	 *
	 * @author Yi Wang (Neakor)
	 * @version Creation date: 12-04-2009 12:04 EST
	 * @version Modified date: 06-27-2009 18:09 EST
	 */
	private final class RecycleMonitorTask implements ICyclicTask {
		/**
		 * The <code>Integer</code> last disposal count.
		 */
		private int lastCount;
		
		/**
		 * Constructor of <code>RecycleMonitorTask</code>.
		 */
		private RecycleMonitorTask() {
			this.lastCount = 0;
		}

		@Override
		public void execute() {
			int count = 0;
			// Remove and count disposed executors.
			final Iterator<IExecutor> iterator = activeExecutors.iterator();
			while(iterator.hasNext()) {
				final IExecutor executor = iterator.next();
				if(executor.hasTerminated()) {
					iterator.remove();
					count++;
				}
			}
			final long currnet = recycleBin.getExpirationTime(TimeUnit.NANOSECONDS);
			long newlimit = 0;
			// If current disposal count is higher, increase
			// disposal expiration time limit by 30%.
			if(count > this.lastCount) {
				newlimit = (long)(currnet * 1.3);
			// If current disposal count is lower or equal,
			// decrease disposal expiration time limit by 15%.
			} else {
				newlimit = (long)(currnet * 0.85);
				// Make sure does not go below 10 seconds.
				if(newlimit < 10000000000l) newlimit = 10000000000l;
			}
			recycleBin.setExpirationTime(newlimit, TimeUnit.NANOSECONDS);
		}

		@Override
		public void shutdown() {}

		@Override
		public void setTimeHandle(final ITimeHandle handle) {}

		@Override
		public long getCycleLimit(final TimeUnit unit) {
			return unit.convert(30, TimeUnit.SECONDS);
		}
	}
}
