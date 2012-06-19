package hemera.core.execution.assisted;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import hemera.core.execution.Executor;
import hemera.core.execution.executable.CyclicExecutable;
import hemera.core.execution.executable.EventExecutable;
import hemera.core.execution.executable.ResultExecutable;
import hemera.core.execution.interfaces.IExceptionHandler;
import hemera.core.execution.interfaces.IServiceListener;
import hemera.core.execution.interfaces.assisted.IAssistExecutor;
import hemera.core.execution.interfaces.assisted.IAssistedService;
import hemera.core.execution.interfaces.task.ICyclicTask;
import hemera.core.execution.interfaces.task.IEventTask;
import hemera.core.execution.interfaces.task.IResultTask;
import hemera.core.execution.interfaces.task.handle.ICyclicTaskHandle;
import hemera.core.execution.interfaces.task.handle.IEventTaskHandle;
import hemera.core.execution.interfaces.task.handle.IResultTaskHandle;

/**
 * <code>AssistExecutor</code> defines implementation
 * of an executor unit that conforms with the interface
 * <code>IAssistExecutor</code>.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public class AssistExecutor extends Executor implements IAssistExecutor {
	/**
	 * The <code>IAssistedService</code> shared by all
	 * assist executors in the service.
	 */
	private final IAssistedService group;
	/**
	 * The <code>IServiceListener</code> instance used
	 * to notify critical events.
	 */
	private final IServiceListener listener;
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
	 * The <code>BlockingDeque</code> of local task
	 * buffer of <code>EventExecutable</code>.
	 * <p>
	 * This data structure needs to support a high
	 * level of concurrency to allow multiple threads
	 * to poll elements from it, since work-stealing
	 * operates on the tail end and local execution
	 * operates on the head end. Assignments are put
	 * at the head.
	 */
	private final BlockingDeque<EventExecutable> buffer;
	/**
	 * The idling <code>Lock</code>.
	 */
	private final Lock lock;
	/**
	 * The idling <code>Condition</code>.
	 */
	private final Condition idle;

	/**
	 * Constructor of <code>AssistExecutor</code>.
	 * @param name The <code>String</code> name of this
	 * executor thread.
	 * @param handler The <code>IExceptionHandler</code>
	 * used for task execution graceful exception
	 * handling.
	 * @param group The <code>IAssistedService</code>
	 * shared by all assist executors.
	 * @param listener The <code>IServiceListener</code>
	 * instance used to notify critical events.
	 * @param maxBufferSize The <code>int</code> upper
	 * limit for the internal task buffer.
	 * @param idletime The <code>long</code> eager-
	 * idling waiting time value.
	 * @param idleunit The <code>TimeUnit</code> eager-
	 * idling waiting time unit.
	 */
	public AssistExecutor(final String name, final IExceptionHandler handler, final IAssistedService group,
			final IServiceListener listener, final int maxBufferSize, final long idletime, final TimeUnit idleunit) {
		super(name, handler);
		this.group = group;
		this.listener = listener;
		this.idletime = idletime;
		this.idleunit = idleunit;
		// TODO Replace with Java 7 ConcurrentLinkedDeque.
		this.buffer = new LinkedBlockingDeque<EventExecutable>(maxBufferSize);
		this.lock = new ReentrantLock();
		this.idle = this.lock.newCondition();
	}

	@Override
	public boolean assist() {
		final EventExecutable executable = this.buffer.pollLast();
		if (executable == null) return false;
		try {
			executable.execute();
		} catch (final Exception e) {
			this.handler.handle(e);
		}
		return true;
	}

	@Override
	protected final void doRun() throws Exception {
		// Execute local task buffer until empty.
		// Poll from head to lower contention since
		// other assisting executors poll from tail.
		EventExecutable executable = this.buffer.pollFirst();
		while (executable != null) {
			executable.execute();
			executable = this.buffer.pollFirst();
		}
		// Reach group to assist other executors
		// until all executor buffers are empty.
		this.group.assist();
		// Eager idling.
		this.lock.lock();
		try {
			// Check for termination.
			if (this.hasRequestedTermination()) return;
			// Do not go into idling if there are tasks.
			else if (!this.buffer.isEmpty()) return;
			// Idle otherwise. Eager wake up to recover other
			// executors from dead-lock.
			else {
				this.idle.await(this.idletime, this.idleunit);
			}
		} finally {
			this.lock.unlock();
		}
	}
	
	/**
	 * Signal idling to wake up.
	 */
	private void wakeup() {
		this.lock.lock();
		try {
			this.idle.signalAll();
		} finally {
			this.lock.unlock();
		}
	}

	@Override
	public final void requestTerminate() {
		super.requestTerminate();
		// Wake up idling.
		this.wakeup();
	}
	
	@Override
	protected IEventTaskHandle doAssign(final IEventTask task) {
		final EventExecutable executable = new EventExecutable(task);
		this.doAssign(executable);
		return executable;
	}
	
	@Override
	protected ICyclicTaskHandle doAssign(final ICyclicTask task) {
		final CyclicExecutable executable = new CyclicExecutable(task, this.handler);
		this.doAssign(executable);
		return executable;
	}

	@Override
	protected <V> IResultTaskHandle<V> doAssign(final IResultTask<V> task) {
		final ResultExecutable<V> executable = new ResultExecutable<V>(task);
		this.doAssign(executable);
		return executable;
	}
	
	/**
	 * Perform the assignment of given executable.
	 * @param <E> The <code>EventExecutable</code>
	 * type.
	 * @param executable The <code>E</code> to be
	 * assigned.
	 */
	private final <E extends EventExecutable> void doAssign(final E executable) {
		// Insert to the head since only local thread
		// is operating on the head where other assist
		// executors operate on the tail, thus lowering
		// thread contention.
		// First try to use non-blocking insertion to
		// allow detection of capacity reached event.
		final boolean succeeded = this.buffer.offerFirst(executable);
		if (!succeeded) {
			this.listener.capacityReached();
			// Use blocking insertion to wait until an
			// existing task completes.
			try {
				this.buffer.putFirst(executable);
			} catch (final InterruptedException e) {
				this.handler.handle(e);
				this.doAssign(executable);
			}
		}
		// Wake up idling.
		this.wakeup();
	}

	@Override
	public final int getQueueLength() {
		return this.buffer.size();
	}
}
