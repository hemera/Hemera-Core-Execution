package hemera.core.execution;

import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import hemera.core.execution.interfaces.IAssistExecutor;
import hemera.core.execution.interfaces.cycle.IAssistBarrier;
import hemera.core.execution.interfaces.exception.IExceptionHandler;
import hemera.core.execution.interfaces.task.IEventTask;
import hemera.core.execution.interfaces.task.IResultTask;
import hemera.core.execution.interfaces.task.handle.IEventTaskHandle;
import hemera.core.execution.interfaces.task.handle.IResultTaskHandle;

/**
 * <code>AssistExecutor</code> defines implementation
 * of an executor unit that is responsible for the
 * execution of foreground event or result tasks with
 * work-stealing assisting functionality to improve
 * the overall execution performance.
 * <p>
 * <code>AssistExecutor</code> is an internal structure
 * that should never be used outside the scope of the
 * execution service due to its dependency on various
 * internal implementations.
 * <p>
 * <code>AssistExecutor</code> should be created at the
 * initialization time of the execution service in a
 * fixed quantity determined based on running hardware
 * configurations. All these assist executors should be
 * constructed with a single <code>IAssistBarrier</code>
 * to allow them perform work-stealing assist with each
 * other at runtime.
 * <p>
 * <code>AssistExecutor</code> follows the execution
 * procedure outlined by <code>IAssistBarrier</code>.
 * 1. Execute local task buffer until empty.
 * 2. Reach barrier to assist other executors until
 * all executor buffers are empty.
 * 3. Block until a new task is assigned to the local
 * buffer or eager-idling time expires. Go to step 1.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
class AssistExecutor extends Executor implements IAssistExecutor {
	/**
	 * The <code>IAssistBarrier</code> unit shared by
	 * all assist executors in the service.
	 */
	private final IAssistBarrier barrier;
	/**
	 * The <code>Deque</code> of assigned task units
	 * in <code>EventExecutable</code> container.
	 * <p>
	 * This data structure needs to support a high
	 * level of concurrency to allow multiple threads
	 * to poll elements from it, since work-stealing
	 * operates on the tail end and local execution
	 * operates on the head end.
	 */
	private final Deque<EventExecutable> buffer;
	/**
	 * The <code>Lock</code> used to form the idling
	 * condition.
	 */
	private final Lock lock;
	/**
	 * The <code>Condition</code> for idling when no
	 * tasks to execute. Signaled when new tasks are
	 * assigned.
	 */
	private final Condition idle;

	/**
	 * Constructor of <code>AssistExecutor</code>.
	 * @param name The <code>String</code> name of this
	 * executor thread.
	 * @param handler The <code>IExceptionHandler</code>
	 * used for task execution graceful exception handling.
	 * @param barrier The <code>IAssistBarrier</code> unit
	 * shared by all assist executors in the service.
	 */
	public AssistExecutor(final String name, final IExceptionHandler handler, final IAssistBarrier barrier) {
		super(name, handler);
		this.barrier = barrier;
		// TODO Replace with Java 7 ConcurrentLinkedDeque.
		this.buffer = new LinkedBlockingDeque<EventExecutable>();
		this.lock = new ReentrantLock();
		this.idle = this.lock.newCondition();
	}

	@Override
	public void run() {
		try {
			while (!this.terminated) {
				try {
					// Execute local task buffer until empty.
					// Poll from head to lower contention since
					// other assisting executors poll from tail.
					EventExecutable executable = this.buffer.pollFirst();
					while (executable != null) {
						executable.execute();
						executable = this.buffer.pollFirst();
					}
					// Reach barrier to assist other executors
					// until all executor buffers are empty.
					this.barrier.assist();
					// Block until a new task is assigned to the
					// local buffer. Loop back.
					this.lock.lock();
					try {
						// Check for termination and buffer.
						if (this.terminated) break;
						// Continue loop if there's tasks.
						else if (!this.buffer.isEmpty()) continue;
						// Idle otherwise.
						// Eager wake up to recover other executors from dead-lock.
						else this.idle.await(500, TimeUnit.MILLISECONDS);
					} finally {
						this.lock.unlock();
					}
				// Catch any runtime exceptions with exception handler.
				} catch (final Exception e) {
					this.handler.handle(e);
				}
			}
		} finally {
			this.threadTerminated = true;
		}
	}

	@Override
	public IEventTaskHandle assign(final IEventTask task) {
		// Check for termination early to avoid object
		// construction.
		if (this.terminated) return null;
		// Perform assignment.
		final EventExecutable executable = new EventExecutable(task);
		this.doAssign(executable);
		return executable;
	}

	@Override
	public <V> IResultTaskHandle<V> assign(final IResultTask<V> task) {
		// Check for termination early to avoid object
		// construction.
		if (this.terminated) return null;
		// Perform assignment.
		final ResultExecutable<V> executable = new ResultExecutable<V>(task);
		this.doAssign(executable);
		return executable;
	}

	/**
	 * Perform the assignment and idling notification.
	 * @param <E> The <code>EventExecutable</code> type.
	 * @param executable The <code>E</code> to be assigned.
	 */
	private <E extends EventExecutable> void doAssign(final E executable) {
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
	public boolean executeTail() {
		if (this.terminated) return false;
		final EventExecutable executable = this.buffer.pollLast();
		if (executable == null) return false;
		try {
			executable.execute();
		} catch (Exception e) {
			this.handler.handle(e);
		}
		return true;
	}

	@Override
	public void terminate() throws Exception {
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
	public int getQueueLength() {
		return this.buffer.size();
	}
}
