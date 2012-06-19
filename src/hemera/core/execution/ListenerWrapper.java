package hemera.core.execution;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import hemera.core.execution.interfaces.IExceptionHandler;
import hemera.core.execution.interfaces.IServiceListener;

/**
 * <code>ListenerWrapper</code> defines the utility unit
 * that contains a <code>IServiceListener</code> that
 * should be notified about the critical events in the
 * service. It provides the frequency management to
 * allow the service simply invoke this handler unit
 * whenever an event occurs, however, this handler will
 * dispatch the invocations according to the listener
 * defined frequency.
 * <p>
 * This implementation fully supports concurrency and
 * thread-safety allowing the wrapper to be invoked by
 * the service in any thread.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public class ListenerWrapper implements IServiceListener {
	/**
	 * The <code>IServiceListener</code> instance.
	 */
	private final IServiceListener listener;
	/**
	 * The <code>IExceptionHandler</code> instance used
	 * by the service.
	 */
	private final IExceptionHandler handler;
	/**
	 * The invocation <code>Lock</code>.
	 * <p>
	 * This lock guarantees that only a single thread
	 * can send a notification at a time.
	 */
	private final Lock lock;
	/**
	 * The <code>long</code> notification frequency in
	 * nanoseconds.
	 */
	private long frequencyNano;
	/**
	 * The <code>long</code> last capacity-reached event
	 * notification time in nanoseconds.
	 * <p>
	 * This value is guarded by the invocation lock.
	 */
	private long lastCapcityReachedTime;
	
	/**
	 * Constructor of <code>ListenerWrapper</code>.
	 * @param listener The <code>IServiceListener</code>
	 * instance.
	 * @param handler The <code>IExceptionHandler</code>
	 * instance used by the service.
	 */
	public ListenerWrapper(final IServiceListener listener, final IExceptionHandler handler) {
		this.listener = listener;
		this.handler = handler;
		this.lock = new ReentrantLock();
		this.frequencyNano = this.listener.getFrequency(TimeUnit.NANOSECONDS);
	}

	@Override
	public void capacityReached() {
		// Only allow a single thread to pass through at a time.
		this.lock.lock();
		try {
			final long currenttime = System.nanoTime();
			final long elapsed = currenttime - this.lastCapcityReachedTime;
			if (elapsed > this.frequencyNano) {
				this.lastCapcityReachedTime = currenttime;
				this.listener.capacityReached();
			}
		} catch (final Exception e) {
			this.handler.handle(e);
		} finally {
			this.lock.unlock();
		}
	}

	@Override
	public long getFrequency(final TimeUnit unit) {
		return this.listener.getFrequency(unit);
	}
}
