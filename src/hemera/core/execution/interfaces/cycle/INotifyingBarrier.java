package hemera.core.execution.interfaces.cycle;

/**
 * <code>INotifyingBarrier</code> defines the interface
 * of a barrier unit that extends the functionalities
 * provided by <code>ISynchronizingBarrier</code> with
 * the additional functionality defined by observable
 * cycle interface, in which it automatically notifies
 * registered cycle observers when a cycle is completed.
 * <p>
 * <code>INotifyingBarrier</code> utilizes the event
 * task execution provided by execution service to
 * perform the various notifications in parallel. It
 * submits each cycle observer notification as an
 * event task to the execution service for processing.
 * <p>
 * <code>INotifyingBarrier</code> should be typically
 * used by a single executor thread rather than a number
 * of executors, since it uses event tasks to perform
 * the notification process. Usually when multiple
 * executors reach for the same barrier, it is more
 * efficient to perform the notification process locally
 * using the reaching executors directly.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface INotifyingBarrier extends ISynchronizingBarrier, IObservableCycle {

	/**
	 * Signal an execution cycle is completed and wait
	 * for all other executors to complete their execution
	 * cycle. Notify all the registered cycle observers
	 * before starting next execution cycle.
	 * <p>
	 * This method should only be invoked by the internal
	 * executor threads. External invocations can cause
	 * system error.
	 */
	@Override
	public void reach();
}
