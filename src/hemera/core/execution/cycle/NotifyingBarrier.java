package hemera.core.execution.cycle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import hemera.core.execution.interfaces.IExecutionService;
import hemera.core.execution.interfaces.cycle.ICycleObserver;
import hemera.core.execution.interfaces.cycle.INotifyingBarrier;
import hemera.core.execution.interfaces.exception.IExceptionHandler;
import hemera.core.execution.interfaces.task.IEventTask;
import hemera.core.execution.interfaces.task.handle.IEventTaskHandle;

/**
 * <code>NotifyingBarrier</code> defines implementation
 * of an extended synchronizing barrier that provides
 * the additional functionality to automatically notify
 * registered cycle observers via <code>IEventTask</code>
 * execution in parallel. Since notifying barrier
 * should be used by a single executor, it is more
 * efficient to use event task execution provided by
 * execution service to parallelize the notification
 * process.
 * <p>
 * <code>NotifyingBarrier</code> should be used when
 * only a single executor reaches for the barrier unit.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public class NotifyingBarrier extends SynchronizingBarrier implements INotifyingBarrier {
	/**
	 * The <code>IExecutionService</code> instance used
	 * to execute the observer notification event tasks.
	 */
	private final IExecutionService service;
	/**
	 * The <code>Map</code> of <code>ICycleObserver</code>
	 * instances to their corresponding instances of
	 * <code>ObserverTask</code>.
	 * <p>
	 * This data structure needs to be thread-safe, since
	 * there may be concurrent modifications performed to
	 * the data structure.
	 */
	private final Map<ICycleObserver, ObserverTask> observerMap;
	/**
	 * The <code>List</code> of <code>ObserverTask</code>
	 * instances that represent the registered observers.
	 * <p>
	 * This data structure needs to be thread-safe and
	 * optimized for iteration performance, since there
	 * may be modifications to the data structure while
	 * the data structure is being iterated through.
	 */
	private final List<ObserverTask> observerTasks;
	/**
	 * The temporary <code>List</code> used to store
	 * the submitted observer <code>IEventTaskHandle</code>.
	 */
	private final List<IEventTaskHandle> handles;
	/**
	 * The <code>IExecptionHandler</code> used for the
	 * result retrieval of observer event tasks.
	 */
	private final IExceptionHandler handler;
	
	/**
	 * Constructor of <code>NotifyingBarrier</code>.
	 * @param count The <code>Integer</code> number
	 * of executors that will be reaching for this
	 * barrier.
	 * @param service The <code>IExecutionService</code>
	 * instance used to execute the observer notification
	 * event tasks.
	 * @param handler The <code>IExceptionHandler</code>
	 * instance.
	 */
	public NotifyingBarrier(final int count, final IExecutionService service, final IExceptionHandler handler) {
		super(count);
		this.service = service;
		this.observerMap = new ConcurrentHashMap<ICycleObserver, ObserverTask>(10);
		this.observerTasks = new CopyOnWriteArrayList<ObserverTask>();
		this.handles = new ArrayList<IEventTaskHandle>(10);
		this.handler = handler;
	}

	@Override
	public void reach() {
		// Check broken.
		if(this.isBroken()) return;
		// Notify observers.
		this.notifyObservers();
		// Reach for synchronizing.
		super.reach();
	}
	
	/**
	 * Notify all the observers.
	 */
	private final void notifyObservers() {
		// Submit the event tasks.
		for(final ObserverTask task : this.observerTasks) {
			final IEventTaskHandle handle = this.service.submitForeground(task);
			if(handler != null) this.handles.add(handle);
		}
		// Make sure the tasks are executed before move on.
		final int size = this.handles.size();
		for(int i = 0; i < size; i++) {
			final IEventTaskHandle handle = this.handles.get(i);
			try {
				handle.await();
			} catch (InterruptedException e) {
				// Should not occur.
				e.printStackTrace();
			}
		}
		this.handles.clear();
	}
	
	@Override
	public boolean register(final ICycleObserver observer) {
		if(this.isBroken() || observer == null) return false;
		else if(this.observerMap.containsKey(observer)) return false;
		final ObserverTask task = new ObserverTask(observer);
		this.observerMap.put(observer, task);
		this.observerTasks.add(task);
		return true;
	}

	@Override
	public boolean unregister(final ICycleObserver observer) {
		if(this.isBroken() || observer == null) return false;
		final ObserverTask task = this.observerMap.remove(observer);
		if(task == null) return false;
		return this.observerTasks.remove(task);
	}
	
	@Override
	public Iterable<ICycleObserver> getObservers() {
		return this.observerMap.keySet();
	}

	@Override
	public void clearObservers() {
		this.observerMap.clear();
		this.observerTasks.clear();
	}
	
	/**
	 * <code>ObserverTask</code> defines the event task
	 * that is used to notify a single observer registered
	 * to this cycle. Two instances are considered equal
	 * if represent the same observer instance.
	 *
	 * @author Yi Wang (Neakor)
	 * @version Creation date: 11-30-2009 20:11 EST
	 * @version Modified date: 08-26-2010 11:24 EST
	 */
	private final class ObserverTask implements IEventTask {
		/**
		 * The <code>ICycleObserver</code> instance.
		 */
		private final ICycleObserver observer;
		
		/**
		 * Constructor of <code>ObserverTask</code>.
		 * @param observer The <code>ICycleObserver</code> instance.
		 */
		private ObserverTask(final ICycleObserver observer) {
			this.observer = observer;
		}

		@Override
		public void execute() {
			this.observer.notifyEvent();
		}
		
		@Override
		public boolean equals(final Object o) {
			if(o == null) return false;
			else if(o instanceof ObserverTask) {
				final ObserverTask given = (ObserverTask)o;
				return (this.observer == given.observer);
			}
			return false;
		}
		
		@Override
		public int hashCode() {
			return this.observer.hashCode();
		}
	}
}
