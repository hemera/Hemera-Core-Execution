package hemera.core.execution.interfaces.cycle;

/**
 * <code>IObservableCycle</code> defines the interface
 * of an extension to the basic <code>ICycle</code>
 * with the additional functionality of registration of
 * cycle observers that are notified when an execution
 * cycle is completed. An execution cycle is completed
 * when all monitored executors have notified completion
 * to this cycle instance.
 * <p>
 * <code>IObservableCycle</code> defines the means to
 * allow external <code>ICycleObserver</code> units to
 * perform various operations, typically bookkeeping
 * operations when an execution cycle is completed,
 * since they are notified at cycle completion time.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface IObservableCycle extends ICycle {
	
	/**
	 * Register the given observer to the cycle. The
	 * observer is notified when an execution cycle is
	 * completed.
	 * <p>
	 * This method guarantees its thread safety by
	 * delegating synchronization mechanism down to its
	 * thread safe data structures.
	 * <p>
	 * Observer registered to the cycle is guaranteed
	 * to be invoked by a single thread at once. There
	 * will be no concurrent invocations. However, there
	 * is no guarantee that the observer is invoked by
	 * the same thread from cycle to cycle.
	 * <p>
	 * The underlying data structure is optimized for
	 * fast inexpensive iteration operation which occurs
	 * at the end of every execution cycle. Due to this
	 * optimization property, the register operation is
	 * relatively expensive. And therefore, should be
	 * invoked during initialization process. Frequent
	 * invocations at runtime should be avoided.
	 * @param observer The <code>IObserver</code> to
	 * register.
	 * @return The <code>Boolean</code> success flag.
	 */
	public boolean register(final ICycleObserver observer);

	/**
	 * Unregister the given observer from receiving
	 * execution cycle completion notifications.
	 * <p>
	 * This method guarantees its thread safety by
	 * delegating synchronization mechanism down to
	 * its thread safe buffer.
	 * <p>
	 * The underlying data structure is optimized for
	 * fast inexpensive iteration operation which occurs
	 * at the end of every execution cycle. Due to this
	 * optimization property, the unregister operation
	 * is relatively expensive. And therefore, should be
	 * invoked during the initialization process. Frequent
	 * invocations at runtime should be avoided.
	 * @param observer The <code>IObserver</code> to
	 * unregister.
	 * @return The <code>Boolean</code> success flag.
	 */
	public boolean unregister(final ICycleObserver observer);
	
	/**
	 * Retrieve the currently registered cycle observer
	 * units from this cycle.
	 * @return The <code>Iterable</code> of registered
	 * <code>ICycleObserver</code> units.
	 */
	public Iterable<ICycleObserver> getObservers();
	
	/**
	 * Remove all registered cycle observer units from
	 * this cycle.
	 */
	public void clearObservers();
}
