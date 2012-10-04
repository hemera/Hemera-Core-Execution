package hemera.core.execution.scalable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import hemera.core.execution.Executor;
import hemera.core.execution.executable.CyclicExecutable;
import hemera.core.execution.executable.EventExecutable;
import hemera.core.execution.executable.ResultExecutable;
import hemera.core.execution.interfaces.IExceptionHandler;
import hemera.core.execution.interfaces.scalable.IScalableService;
import hemera.core.execution.interfaces.scalable.IScaleExecutor;
import hemera.core.execution.interfaces.task.ICyclicTask;
import hemera.core.execution.interfaces.task.IEventTask;
import hemera.core.execution.interfaces.task.IResultTask;
import hemera.core.execution.interfaces.task.handle.ICyclicTaskHandle;
import hemera.core.execution.interfaces.task.handle.IEventTaskHandle;
import hemera.core.execution.interfaces.task.handle.IResultTaskHandle;

/**
 * <code>ScaleExecutor</code> defines the executor
 * implementation that confirms with the interface
 * <code>IScaleExecutor</code>.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public class ScaleExecutor extends Executor implements IScaleExecutor {
	/**
	 * The <code>IScalableService</code> group.
	 */
	private final IScalableService group;
	/**
	 * The <code>boolean</code> indicating if this
	 * executor instance is on-demand.
	 */
	private final boolean ondemand;
	/**
	 * The <code>long</code> timeout value used to
	 * terminate the executor if it is an on-demand
	 * executor after task execution completion.
	 */
	private final long timeoutValue;
	/**
	 * The <code>TimeUnit</code> the timeout value
	 * is in.
	 */
	private final TimeUnit timeoutUnit;
	/**
	 * The timing-out <code>Lock</code>.
	 */
	private final Lock lock;
	/**
	 * The waiting <code>Condition</code>.
	 */
	private final Condition wait;
	/**
	 * The <code>AtomicReference</code> of assigned
	 * <code>EventExecutable</code>.
	 * <p>
	 * Atomic check and set operation is needed to
	 * ensure that only a single instance can be
	 * assigned at any given time, and allow the
	 * write and read operations to be performed in
	 * different threads.
	 */
	private final AtomicReference<EventExecutable> task;

	/**
	 * Constructor of <code>ScaleExecutor</code>.
	 * <p>
	 * This constructor creates an initial executor that
	 * is part of the initial minimum executor pool.
	 * @param name The <code>String</code> name of this
	 * executor thread.
	 * @param handler The <code>IExceptionHandler</code>
	 * used for task execution graceful exception
	 * handling.
	 * @param group The <code>IScalableService</code>
	 * shared by all scale executors.
	 */
	public ScaleExecutor(final String name, final IExceptionHandler handler, final IScalableService group) {
		this(name, handler, group, false, -1, null);
	}

	/**
	 * Constructor of <code>ScaleExecutor</code>.
	 * <p>
	 * This constructor creates an on-demand executor
	 * that will terminate if there are no new tasks
	 * assigned to it within the timeout period.
	 * @param name The <code>String</code> name of this
	 * executor thread.
	 * @param handler The <code>IExceptionHandler</code>
	 * used for task execution graceful exception
	 * handling.
	 * @param group The <code>IScalableService</code>
	 * shared by all scale executors.
	 * @param timeoutValue The <code>long</code> time-
	 * out value used to terminate this on-demand
	 * executor after task execution completion without
	 * new task assignment.
	 * @param timeoutUnit The <code>TimeUnit</code> the
	 * timeout value is in.
	 */
	public ScaleExecutor(final String name, final IExceptionHandler handler, final IScalableService group,
			final long timeoutValue, final TimeUnit timeoutUnit) {
		this(name, handler, group, true, timeoutValue, timeoutUnit);
	}

	/**
	 * Constructor of <code>ScaleExecutor</code>.
	 * @param name The <code>String</code> name of this
	 * executor thread.
	 * @param handler The <code>IExceptionHandler</code>
	 * used for task execution graceful exception
	 * handling.
	 * @param group The <code>IScalableService</code>
	 * shared by all scale executors.
	 * @param ondemand <code>true</code> if this instance
	 * is created as an on-demand executor.
	 * @param timeoutValue The <code>long</code> time-
	 * out value used to terminate the executor if it
	 * is an on-demand executor after task execution
	 * completion.
	 * @param timeoutUnit The <code>TimeUnit</code> the
	 * timeout value is in.
	 */
	private ScaleExecutor(final String name, final IExceptionHandler handler, final IScalableService group, final boolean ondemand,
			final long timeoutValue, final TimeUnit timeoutUnit) {
		super(name, handler);
		this.group = group;
		this.ondemand = ondemand;
		this.timeoutValue = timeoutValue;
		this.timeoutUnit = timeoutUnit;
		this.lock = new ReentrantLock();
		this.wait = this.lock.newCondition();
		this.task = new AtomicReference<EventExecutable>(null);
	}

	@Override
	protected final void doRun() throws Exception {
		// Execute local task and set it to null to allow new assignments.
		final EventExecutable executable = this.task.getAndSet(null);
		// Initial activation cycle will not have a task yet.
		if (executable != null) {
			// If executable is cyclic, retain the reference for
			// executor termination.
			if (executable instanceof CyclicExecutable) {
				this.currentCyclicExecutable = (CyclicExecutable)executable;
			} else {
				this.currentCyclicExecutable = null;
			}
			// Execute.
			executable.execute();
			// Recycle for more tasks only after executing one task.
			// This prevents the case where a new on-demand executor is
			// created and will be assigned with the task that triggered
			// the creation, but the initial run cycle can put the
			// executor back into the pool causing it to be assigned
			// with another task.
			this.group.recycle(this);
		}
		// Go into waiting mode.
		boolean signaled = true;
		this.lock.lock();
		try {
			// Check for termination before entering idling while holding lock.
			if (this.hasRequestedTermination()) return;
			// Do not go into waiting if there is a task.
			else if (this.task.get() != null) return;
			// If this executor is on-demand, wait on timeout.
			else if (this.ondemand) {
				signaled = this.wait.await(this.timeoutValue, this.timeoutUnit);
			}
			// Otherwise just wait for next task.
			else {
				this.wait.await();
			}
		} finally {
			this.lock.unlock();
		}
		// Terminate and remove from group if timed-out.
		if (this.ondemand && !signaled) {
			final boolean suceeded = this.group.remove(this);
			// Only request termination if the executor has successfully been removed.
			// The removal might fail if the executor has been polled for a new task.
			if (suceeded) this.requestTerminate();
		}
	}

	/**
	 * Signal waiting to wake up.
	 */
	private void wakeup() {
		this.lock.lock();
		try {
			this.wait.signalAll();
		} finally {
			this.lock.unlock();
		}
	}

	@Override
	public final void requestTerminate() {
		super.requestTerminate();
		// Wake up waiting.
		this.wakeup();
	}

	@Override
	protected IEventTaskHandle doAssign(final IEventTask task) {
		final EventExecutable executable = new EventExecutable(task);
		return this.doAssign(executable);
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
		return this.doAssign(executable);
	}

	/**
	 * Perform the assignment of given executable.
	 * @param <E> The <code>EventExecutable</code>
	 * type.
	 * @param executable The <code>E</code> to be
	 * assigned.
	 * @return The given <code>E</code> executable
	 * if succeeded.
	 * @throws IllegalStateException If there is a
	 * task already assigned.
	 */
	private final <E extends EventExecutable> E doAssign(final E executable) throws IllegalStateException {
		final boolean succeeded = this.task.compareAndSet(null, executable);
		// There is a task assigned already.
		if (!succeeded) {
			throw new IllegalStateException("There is a task already assigned to executor: " + this.getName());
		}
		// Succeeded.
		else {
			// Wake up waiting.
			this.wakeup();
			// Return executable as handle.
			return executable;
		}
	}

	@Override
	public final boolean isOndemand() {
		return this.ondemand;
	}
}
