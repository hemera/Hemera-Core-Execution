package hemera.core.execution.interfaces;

import java.util.concurrent.TimeUnit;

/**
 * <code>IServiceListener</code> defines the interface
 * of a listener unit that is notified when a critical
 * event such as maximum capacity reached occurs.
 * <p>
 * <code>IServiceListener</code> defines a notification
 * frequency, where the save event is only notified at
 * that frequency at the most. For instance, if the
 * service listener defines the capacity as one hour,
 * then if the service has reached its maximum capacity
 * 10 times within the same hour, only the first time
 * will trigger a notification.
 * <p>
 * The notifications are guaranteed to be invoked by
 * a single thread at a time. However, there is no
 * guarantee that it is the same thread that invokes
 * the methods over time.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface IServiceListener {

	/**
	 * Notify that the service has reached its maximum
	 * capacity.
	 */
	public void capacityReached();
	
	/**
	 * Retrieve the frequency this listener wants to
	 * be notified about the same event in the given
	 * time unit.
	 * @param unit The <code>TimeUnit</code> the return
	 * value is in.
	 * @return The <code>long</code> frequency value
	 * in the given time unit.
	 */
	public long getFrequency(final TimeUnit unit);
}
