package hemera.core.execution.interfaces.cycle;

/**
 * <code>ICycleObserver</code> defines the interface of
 * an observer unit that is notified when a single
 * execution cycle execution is completed. The observer
 * unit should then perform various book keeping tasks
 * such as logging and cycle re-initialization.
 * <p>
 * <code>ICycleObserver</code> is invoked at the end of
 * every execution cycle by one of the executor threads.
 * It is guaranteed that there is only a single thread
 * that invokes the observer, though the invoking thread
 * may vary from execution cycle to cycle.
 * <p>
 * It is important that the implementations provide a
 * good hashing function, since various internal data
 * structure operations performance rely on good hashing
 * function.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface ICycleObserver {

	/**
	 * Notify the observer that a execution cycle has
	 * been completed.
	 * <p>
	 * This method is guaranteed to be only invoked by
	 * a single thread at a time. However, there is no
	 * guarantee that the invocations are made by the
	 * same thread from execution cycle to cycle.
	 */
	public void notifyEvent();
}
