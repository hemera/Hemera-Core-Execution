package hemera.core.execution.time;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import hemera.core.execution.interfaces.time.ITimer;

/**
 * <code>AbstractTimer</code> defines the abstraction
 * of all timer utility units. It provides all the
 * necessary basic implementations. It does not define
 * a specific time unit to be used. It is up to the
 * subclasses to provide the functionality to retrieve
 * the current time in the specified unit.
 * <p>
 * Extensive caution must be exercised when overriding
 * provided implementations. The synchronization
 * policies defined by <code>ITimeHandle</code> and
 * <code>ITimer</code> interfaces must be met.
 * <p>
 * The data fields of <code>interpolation</code> and
 * <code>updateRate</code> are correlated invariants.
 * Their modification operations need to be performed
 * atomically together.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public abstract class AbstractTimer implements ITimer {
	/**
	 * The <code>Lock</code> guarding all maintenance
	 * operations.
	 */
	protected final Lock lock;
	/**
	 * The <code>AtomicLong</code> current interpolation.
	 * <p>
	 * Since retrieval operation may be invoked by
	 * multiple threads concurrently, concurrent access
	 * needs to be supported. Also the update/reset
	 * operation may be invoked by a different thread
	 * from the retrieval thread, memory visibility of
	 * the value needs to be guaranteed.
	 */
	protected final AtomicLong interpolation;
	/**
	 * The <code>Float</code> current update rate.
	 * <p>
	 * Since the update/reset write-operations may be
	 * invoked by a different thread from the retrieval
	 * thread, memory visibility of the value needs to
	 * be guaranteed.
	 */
	protected volatile float updateRate;
	/**
	 * The <code>Long</code> starting time.
	 * <p>
	 * Since this value is only written with the update
	 * lock held, the memory visibility of this value is
	 * automatically guaranteed by the lock.
	 */
	private long startTime;
	/**
	 * The <code>Long</code> last update time since
	 * starting time.
	 * <p>
	 * Since this value is only written with the update
	 * lock held, the memory visibility of this value is
	 * automatically guaranteed by the lock.
	 */
	private long lastUpdate;

	/**
	 * Constructor of <code>AbstractTimer</code>.
	 */
	protected AbstractTimer() {
		this.lock = new ReentrantLock();
		this.interpolation = new AtomicLong(0);
		this.reset();
	}

	@Override
	public void update() {
		// Lock to prevent interleaving with other maintenance operations.
		this.lock.lock();
		try {
			final long current = this.getCurrent();
			// Prevent system time change causing invalid
			// time values. Reset time values if current
			// time is smaller than the start time.
			if(current < this.startTime) {
				this.reset();
				return;
			}
			// Calculate interpolation.
			final long interpolation = this.updateInterpolation(current);
			// Calculate update rate from interpolation.
			this.updateRate(interpolation);
		} finally {
			this.lock.unlock();
		}
	}

	/**
	 * Calculate the interpolation value based on given
	 * current time value.
	 * @param current The <code>Long</code> current time
	 * value in the timer's time unit.
	 * @return The <code>Long</code> interpolation value.
	 */
	private final long updateInterpolation(final long current) {
		final long elapsed = current - this.startTime;
		final long newInterpolation = elapsed - this.lastUpdate;
		this.lastUpdate = elapsed;
		this.interpolation.set(newInterpolation);
		return newInterpolation;
	}

	/**
	 * Calculate the update rate value based on given
	 * interpolation value in the timer's time unit.
	 * @param interpolation The <code>Long</code>
	 * interpolation value in the timer's time unit.
	 */
	private final void updateRate(final long interpolation) {
		float rate = 0;
		final long nanoInterpolation = this.getUnit().toNanos(interpolation);
		if(nanoInterpolation <= 0) rate = -1;
		else rate = 1000000000.0f / (float)nanoInterpolation;
		this.updateRate = rate;
	}

	@Override
	public void offset(final long value, final TimeUnit unit) {
		final long offset = this.getUnit().convert(value, unit);
		if(offset == 0) return;
		this.lock.lock();
		try {
			// Retrieve the current time during last update.
			final long offsetCurrent = this.lastUpdate + this.startTime + offset;
			// Directly offset interpolation.
			final long offsetInterpolation = this.interpolation.addAndGet(offset);
			// Offset the last update value to be based on the offset current time.
			this.lastUpdate = offsetCurrent - this.startTime;
			// Re-calculate update rate value.
			this.updateRate(offsetInterpolation);
		} finally {
			this.lock.unlock();
		}
	}

	/**
	 * Retrieve the current time in proper time unit.
	 * @return The <code>Long</code> current time.
	 */
	protected abstract long getCurrent();

	@Override
	public float getUpdateRate() {
		return this.updateRate;
	}

	@Override
	public long getInterpolation() {
		final long interpolation = this.interpolation.get();
		return interpolation;
	}

	@Override
	public void reset() {
		// Lock to prevent interleaving with other maintenance operations.
		this.lock.lock();
		try {
			this.interpolation.set(0);
			this.updateRate = 0;
			this.startTime = this.getCurrent();
			this.lastUpdate = 0;
		} finally {
			this.lock.unlock();
		}
	}
}
