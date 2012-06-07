package hemera.core.execution;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import hemera.core.execution.enumn.ETaskOrder;
import hemera.core.execution.interfaces.cycle.ICycleObserver;
import hemera.core.execution.interfaces.cycle.INotifyingBarrier;
import hemera.core.execution.interfaces.task.ICyclicTask;
import hemera.core.execution.interfaces.task.IEventTask;
import hemera.core.execution.interfaces.task.IResultTask;
import hemera.core.execution.interfaces.task.handle.ICyclicTaskHandle;
import hemera.core.execution.interfaces.task.handle.IEventTaskHandle;
import hemera.core.execution.interfaces.task.handle.IResultTaskHandle;

/**
 * <code>CyclicExecutable</code> defines implementation
 * of an executable unit that directly corresponds to a
 * cyclic task unit. It provides the cyclic task type-
 * specific result handling.
 * <p>
 * <code>CyclicExecutable</code> internally provides a
 * package accessible <code>INotifyingBarrier</code> for
 * cycle synchronization and automatic cycle observer
 * notifications.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
class CyclicExecutable extends Executable implements ICyclicTaskHandle {
	/**
	 * The <code>ICyclicTask</code> to be executed.
	 */
	private final ICyclicTask task;
	/**
	 * The <code>Queue</code> of <code>EventExecutable</code>
	 * front event task buffer.
	 * <p>
	 * Since multiple threads can potentially attempt to
	 * buffer and poll executables concurrently, thread
	 * safety needs to be guaranteed.
	 * <p>
	 * This data structure should be optimized for the
	 * middle ground performance of both insertion and
	 * iteration as documented by the interface.
	 */
	private final Queue<EventExecutable> frontBuffer;
	/**
	 * The <code>Queue</code> of <code>EventExecutable</code>
	 * back event task buffer.
	 * <p>
	 * Since multiple threads can potentially attempt to
	 * buffer and poll executables concurrently, thread
	 * safety needs to be guaranteed.
	 * <p>
	 * This data structure should be optimized for the
	 * middle ground performance of both insertion and
	 * iteration as documented by the interface.
	 */
	private final Queue<EventExecutable> backBuffer;
	/**
	 * The <code>INotifyingBarrier</code> unit assigned
	 * at construction time by the executing executor
	 * instance to allow the executable handle observer
	 * related operations.
	 */
	private final INotifyingBarrier barrier;
	
	/**
	 * Constructor of <code>CyclicExecutable</code>.
	 * @param task The <code>ICyclicTask</code> to be
	 * executed.
	 * @param barrier The <code>INotifyingBarrier</code>
	 * unit assigned at construction time by the executing
	 * executor instance to allow the executable handle
	 * observer related operations.
	 */
	public CyclicExecutable(final ICyclicTask task, final INotifyingBarrier barrier) {
		super();
		this.task = task;
		this.frontBuffer = new ConcurrentLinkedQueue<EventExecutable>();
		this.backBuffer = new ConcurrentLinkedQueue<EventExecutable>();
		this.barrier = barrier;
	}

	@Override
	protected void doExecute() throws Exception {
		// Execute front buffer.
		while(!this.frontBuffer.isEmpty()) this.frontBuffer.poll().execute();
		// Execute cycle logic.
		this.task.execute();
		// Execute back buffer.
		while(!this.backBuffer.isEmpty()) this.backBuffer.poll().execute();
	}

	@Override
	public IEventTaskHandle inject(final ETaskOrder order, final IEventTask task) {
		// Early checking to avoid object creation.
		if(task == null) throw new IllegalArgumentException("Task cannot be null.");
		else if(order == null) throw new IllegalArgumentException("Buffer tag cannot be null.");
		// Perform assignment based on order tag.
		final EventExecutable executable = new EventExecutable(task);
		this.doInject(order, executable);
		return executable;
	}

	@Override
	public <T> IResultTaskHandle<T> inject(final ETaskOrder order, final IResultTask<T> task) {
		// Early checking to avoid object creation.
		if(task == null) throw new IllegalArgumentException("Task cannot be null.");
		else if(order == null) throw new IllegalArgumentException("Buffer tag cannot be null.");
		// Perform assignment based on order tag.
		final ResultExecutable<T> executable = new ResultExecutable<T>(task);
		this.doInject(order, executable);
		return executable;
	}
	
	/**
	 * Perform the injection logic based on given order
	 * tag and executable. This method does not perform
	 * any <code>null</code> checking, it assumes all
	 * the checking has been completed.
	 * @param order The <code>ETaskOrder</code> value.
	 * @param executable The <code>E</code> executable.
	 */
	private void doInject(final ETaskOrder order, final EventExecutable executable) {
		switch(order) {
		case Front: this.frontBuffer.add(executable); break;
		case Back: this.backBuffer.add(executable); break;
		default: throw new IllegalArgumentException("Unsupported buffer tag:".concat(order.toString()));
		}
	}

	@Override
	public boolean register(final ICycleObserver observer) {
		return this.barrier.register(observer);
	}

	@Override
	public boolean unregister(final ICycleObserver observer) {
		return this.barrier.unregister(observer);
	}

	@Override
	public boolean cancel() throws Exception {
		this.canceled = true;
		this.barrier.breakBarrier();
		this.task.shutdown();
		return true;
	}

	@Override
	public void setTimeLimit(final long value, final TimeUnit unit) {
		this.barrier.setTimeLimit(value, unit);
	}

	@Override
	public long getTimeLimit(final TimeUnit unit) {
		return this.barrier.getTimeLimit(unit);
	}

	@Override
	public Iterable<ICycleObserver> getObservers() {
		return this.barrier.getObservers();
	}

	@Override
	public void clearObservers() {
		this.barrier.clearObservers();
	}
}
