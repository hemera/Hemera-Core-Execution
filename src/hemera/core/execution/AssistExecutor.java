package hemera.core.execution;

import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import hemera.core.execution.interfaces.IExceptionHandler;
import hemera.core.execution.interfaces.assisted.IAssistExecutor;
import hemera.core.execution.interfaces.assisted.IAssistedService;
import hemera.core.execution.interfaces.task.IEventTask;
import hemera.core.execution.interfaces.task.IResultTask;
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
class AssistExecutor extends Executor implements IAssistExecutor {
	/**
	 * The <code>IAssistedService</code> shared by all
	 * assist executors in the service.
	 */
	final IAssistedService group;
	/**
	 * The <code>Deque</code> of local task buffer
	 * of <code>Executable</code>.
	 * <p>
	 * This data structure needs to support a high
	 * level of concurrency to allow multiple threads
	 * to poll elements from it, since work-stealing
	 * operates on the tail end and local execution
	 * operates on the head end. Assignments are put
	 * at the head.
	 */
	final Deque<Executable> buffer;
	/**
	 * The <code>long</code> executor idle time value.
	 */
	final long idletime;
	/**
	 * The <code>TimeUnit</code> executor idle time
	 * unit.
	 */
	final TimeUnit idleunit;
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
	 * @param idletime The <code>long</code> eager-
	 * idling waiting time value.
	 * @param idleunit The <code>TimeUnit</code> eager-
	 * idling waiting time unit.
	 */
	AssistExecutor(final String name, final IExceptionHandler handler, final IAssistedService group,
			final long idletime, final TimeUnit idleunit) {
		super(name, handler);
		this.group = group;
		// TODO Replace with Java 7 ConcurrentLinkedDeque.
		this.buffer = new LinkedBlockingDeque<Executable>();
		this.lock = new ReentrantLock();
		this.idle = this.lock.newCondition();
		this.idletime = idletime;
		this.idleunit = idleunit;
	}

	@Override
	public boolean assist() {
		final Executable executable = this.buffer.pollLast();
		if (executable == null) return false;
		try {
			executable.execute();
		} catch (final Exception e) {
			this.handler.handle(e);
		}
		return true;
	}

	@Override
	final void doRun() throws Exception {
		// Execute local task buffer until empty.
		// Poll from head to lower contention since
		// other assisting executors poll from tail.
		Executable executable = this.buffer.pollFirst();
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
			if (this.terminated) return;
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

	@Override
	public final void terminate() {
		super.terminate();
		// Wake up idling.
		this.lock.lock();
		try {
			this.idle.signalAll();
		} finally {
			this.lock.unlock();
		}
	}
	
	@Override
	public final IEventTaskHandle assign(final IEventTask task) {
		// Check for termination early to avoid object construction.
		if (this.terminated) return null;
		// Perform assignment.
		final EventExecutable executable = new EventExecutable(task);
		this.doAssign(executable);
		return executable;
	}

	@Override
	public final <V> IResultTaskHandle<V> assign(final IResultTask<V> task) {
		// Check for termination early to avoid object construction.
		if (this.terminated) return null;
		// Perform assignment.
		final ResultExecutable<V> executable = new ResultExecutable<V>(task);
		this.doAssign(executable);
		return executable;
	}
	
	/**
	 * Perform the assignment of given executable.
	 * @param <E> The <code>Executable</code> type.
	 * @param executable The <code>E</code> to be
	 * assigned.
	 */
	private final <E extends Executable> void doAssign(final E executable) {
		// Insert to the head since only local thread
		// is operating on the head where other assist
		// executors operate on the tail, thus lowering
		// thread contention.
		this.buffer.offerFirst(executable);
		// Wake up idling.
		this.lock.lock();
		try {
			this.idle.signalAll();
		} finally {
			this.lock.unlock();
		}
	}

	@Override
	public final int getQueueLength() {
		return this.buffer.size();
	}
}
