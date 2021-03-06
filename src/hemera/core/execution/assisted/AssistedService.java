package hemera.core.execution.assisted;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import hemera.core.execution.AbstractServiceListener;
import hemera.core.execution.ExecutionService;
import hemera.core.execution.Executor;
import hemera.core.execution.interfaces.IExceptionHandler;
import hemera.core.execution.interfaces.assisted.IAssistExecutor;
import hemera.core.execution.interfaces.assisted.IAssistedService;
import hemera.core.execution.interfaces.task.ICyclicTask;
import hemera.core.execution.interfaces.task.IEventTask;
import hemera.core.execution.interfaces.task.IResultTask;
import hemera.core.execution.interfaces.task.handle.ICyclicTaskHandle;
import hemera.core.execution.interfaces.task.handle.IEventTaskHandle;
import hemera.core.execution.interfaces.task.handle.IResultTaskHandle;
import hemera.core.utility.data.AtomicCyclicInteger;

/**
 * <code>AssistedService</code> defines a execution
 * service implementation that confirms with defined
 * interface <code>IAssistedService</code>.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.2
 */
public class AssistedService extends ExecutionService implements IAssistedService {
	/**
	 * The array of <code>IAssistExecutor</code> pool.
	 */
	private final IAssistExecutor[] executors;
	/**
	 * The <code>int</code> maximum task buffer size
	 * for the executors.
	 */
	private final int maxBufferSize;
	/**
	 * The <code>long</code> executor idle time value.
	 */
	private final long idletime;
	/**
	 * The <code>TimeUnit</code> executor idle time
	 * unit.
	 */
	private final TimeUnit idleunit;
	/**
	 * The <code>AtomicCyclicInteger</code> used to
	 * track the next executor index.
	 */
	private final AtomicCyclicInteger index;

	/**
	 * Constructor of <code>AssistedService</code>.
	 * @param handler The <code>IExceptionHandler</code>
	 * instance.
	 * @param listener The <code>AbstractServiceListener</code>
	 * instance.
	 * @param count The <code>int</code> number of
	 * executors this service should create.
	 * @param maxBufferSize The <code>int</code> maximum
	 * task buffer size for the executors.
	 * @param idletime The <code>long</code> eager-
	 * idling waiting time value.
	 * @param idleunit The <code>TimeUnit</code> eager-
	 * idling waiting time unit.
	 */
	public AssistedService(final IExceptionHandler handler, final AbstractServiceListener listener, final int count,
			final int maxBufferSize, final long idletime, final TimeUnit idleunit) {
		super(handler, listener);
		this.executors = new IAssistExecutor[count];
		this.maxBufferSize = maxBufferSize;
		this.idletime = idletime;
		this.idleunit = idleunit;
		this.index = new AtomicCyclicInteger(0, this.executors.length-1);
	}

	@Override
	protected void doActivate() {
		// Create executors first before activation, since once an
		// executor is activated, it'll start assisting but not all
		// executors are created yet.
		for (int i = 0; i < this.executors.length; i++) {
			final String name = "AssistExecutor-" + i;
			final AssistExecutor executor = new AssistExecutor(name, this.handler, this, this.listener,
					this.maxBufferSize, this.idletime, this.idleunit);
			this.executors[i] = executor;
		}
		// Activate executors.
		for (int i = 0; i < this.executors.length; i++) {
			this.executors[i].start();
		}
	}

	@Override
	protected void doShutdown() {
		for (int i = 0; i < this.executors.length; i++) {
			final IAssistExecutor executor = this.executors[i];
			executor.requestTerminate();
		}
	}

	@Override
	protected void doShutdownAndWait() throws InterruptedException {
		for (int i = 0; i < this.executors.length; i++) {
			final IAssistExecutor executor = this.executors[i];
			executor.requestTerminate();
			// Wait for executor thread to terminate.
			while (!executor.hasTerminated()) {
				TimeUnit.MILLISECONDS.sleep(5);
			}
		}
	}

	@Override
	protected void doForceShutdown() {
		for (int i = 0; i < this.executors.length; i++) {
			final IAssistExecutor executor = this.executors[i];
			executor.forceTerminate();
		}
	}

	@Override
	protected void doForceShutdown(final long time, final TimeUnit unit) throws InterruptedException {
		// Gracefully terminate all executors.
		this.doShutdown();
		// Wait for expiration.
		unit.sleep(time);
		// Forcefully terminate and remove all active executors.
		this.doForceShutdown();
	}

	@Override
	protected IEventTaskHandle doSubmit(final IEventTask task) {
		return this.nextAssistExecutor().assign(task);
	}
	
	@Override
	protected ICyclicTaskHandle doSubmit(final ICyclicTask task) {
		return this.nextAssistExecutor().assign(task);
	}

	@Override
	protected <V> IResultTaskHandle<V> doSubmit(final IResultTask<V> task) {
		return this.nextAssistExecutor().assign(task);
	}
	
	/**
	 * Retrieve the next assist executor using a round-
	 * robin rotation.
	 * @return The <code>IAssistExecutor</code>.
	 */
	private IAssistExecutor nextAssistExecutor() {
		// Record start index.
		final int startIndex = this.index.get();
		// Try to find an executor that is not occupied by a cyclic task.
		while (true) {
			final int index = this.index.incrementAndGet();
			final AssistExecutor executor = (AssistExecutor)this.executors[index];
			// Ensure executor is not currently occupied with a cyclic task,
			// in which case the executor will not be available for a new task.
			if (!executor.isExecutingCyclicTask()) {
				return executor;
			}
			// If we have rotated back to the start index, all executors
			// are occupied by cyclic tasks, service maximum capacity has
			// been reached.
			if (index == startIndex) {
				this.listener.capacityReached();
				// Wait a bit to prevent thrashing when capacity is reached.
				try {
					this.idleunit.sleep(this.idletime);
				} catch (final InterruptedException e) {
					this.handler.handle(e);
				}
			}
		}
	}

	@Override
	public boolean assist() {
		boolean assisted = false;
		for (int i = 0; i < this.executors.length; i++) {
			final IAssistExecutor executor = this.executors[i];
			// Assist a single executor until all of its tasks are finished.
			while (executor.assist()) {
				if (!assisted) assisted = true;
			}
		}
		return assisted;
	}
	
	@Override
	protected Iterable<Executor> getExecutors() {
		final ArrayList<Executor> executors = new ArrayList<Executor>(this.executors.length);
		for (int i = 0; i < this.executors.length; i++) {
			executors.add((Executor)this.executors[i]);
		}
		return executors;
	}

	@Override
	public double getAverageQueueLength() {
		double length = 0;
		for (int i = 0; i < this.executors.length; i++) {
			final IAssistExecutor executor = this.executors[i];
			length += executor.getQueueLength();
		}
		return (length/(double)this.executors.length);
	}

	@Override
	public int getCurrentExecutorCount() {
		return this.executors.length;
	}
}
