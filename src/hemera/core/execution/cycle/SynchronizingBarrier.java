package hemera.core.execution.cycle;

import java.util.Queue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import hemera.core.execution.interfaces.cycle.ISynchronizingBarrier;
import hemera.core.execution.interfaces.time.ITimeHandle;
import hemera.core.execution.interfaces.time.ITimer;
import hemera.core.execution.time.NanoTimer;

/**
 * <code>SynchronizingBarrier</code> is an implementation
 * of an execution barrier that forces all the reaching
 * executors to synchronize their new cycle start time.
 * In other words, this barrier ensures all the executor
 * threads start a new execution cycle at the exact same
 * time.
 * <p>
 * <code>SynchronizingBarrier</code> provides the
 * functionality to put all reaching executors to sleep
 * to conserve computational resources if the current
 * cycle did not use up all the allowed execution time.
 * <p>
 * <code>SynchronizingBarrier</code> avoids using any
 * synchronization mechanism when there is only a single
 * executor instance reaching for this barrier. This is
 * done to improve the performance of the barrier.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public class SynchronizingBarrier implements ISynchronizingBarrier {
	/**
	 * The <code>AtomicBoolean</code> broken flag.
	 */
	private final AtomicBoolean broken;
	/**
	 * The <code>AtomicLong</code> of cycle time limit
	 * in nanoseconds.
	 */
	private final AtomicLong limit;
	/**
	 * The <code>CyclicBarrier</code> used to guarantee
	 * all executors start a new execution cycle at the
	 * same time.
	 * <p>
	 * Without this guarantee, the last arriving executor-
	 * (A) can potentially start generating new event
	 * tasks onto executors that are still spinning
	 * inside the assist loop, since it will directly
	 * skip the assist loop. If indeed this occurs, The
	 * spinning executor(B) will execute the newly
	 * generated event tasks inside the assist loop
	 * before executing its own cyclic task. If unfortunately
	 * that this spinning executor(B) requires longer
	 * time to execute its cyclic task in the new cycle
	 * than executor(A), the new execution cycle will
	 * end up taking longer execution time than it
	 * actually needs. Since when executor(B) is
	 * executing its cyclic task in the new cycle, after
	 * it assisted executor(A), no other executors can
	 * assist it as it is executing a cyclic task. This
	 * will force executor(A) to spin-lock without doing
	 * any actual work if it completes its execution
	 * cycle before executor(B) does in the new cycle,
	 * if executor(B)'s cyclic task does not generate
	 * any event tasks in the new execution cycle.
	 * <p>
	 * This <code>CyclicBarrier</code> provides the
	 * synchronization point to perform any new execution
	 * cycle setup tasks and last cycle join tasks.
	 */
	private final CyclicBarrier barrier;
	/**
	 * The <code>ITimer</code> instance used to measure
	 * the actual execution time of an execution cycle.
	 */
	private final ITimer timer;
	/**
	 * The <code>Runnable</code> <code>EndOfCycleTask</code>
	 * that needs to be run at the end of each cycle.
	 */
	private final Runnable endTask;
	/**
	 * The <code>Queue</code> of <code>Thread</code>
	 * parked by the <code>LockSupport</code> for
	 * cycle sleep.
	 */
	private final Queue<Thread> parked;
	/**
	 * The <code>Long</code> sleep time during the last
	 * execution cycle in nanoseconds.
	 * <p>
	 * This value needs to guarantee its memory visibility
	 * since the last writing thread may be different from
	 * the next reading thread.
	 */
	private volatile long lastsleep;
	/**
	 * The <code>Long</code> current cycle sleep time in
	 * nanoseconds.
	 * <p>
	 * This value needs to guarantee its memory visibility
	 * since all executor threads need to read this value
	 * but it is only written by the last exiting thread.
	 */
	private volatile long sleeptime;
	
	/**
	 * Constructor of <code>SynchronizingBarrier</code>.
	 * <p>
	 * This constructor automatically constructs the
	 * cyclic barrier unit with a built-in book-keeping
	 * end of cycle task and the timer instance.
	 * @param count The <code>Integer</code> number
	 * of executors that will be reaching for this
	 * barrier.
	 */
	public SynchronizingBarrier(final int count) {
		if(count < 1) throw new IllegalArgumentException("Must be at least one reaching executor.");
		this.broken = new AtomicBoolean(false);
		this.limit = new AtomicLong(0);
		if(count == 1) this.barrier = null;
		else this.barrier = new CyclicBarrier(count, this.endTask);
		this.timer = new NanoTimer();
		this.endTask = new EndOfCycleTask();
		this.parked = new ConcurrentLinkedQueue<Thread>();
	}
	
	@Override
	public void reach() {
		try {
			if(this.broken.get()) return;
			// Synchronize at barrier.
			if(this.barrier != null) this.barrier.await();
			// No synchronization if only a single
			// reaching executor. Just run task.
			else this.endTask.run();
			// All executor threads sleep if necessary.
			final long sleeptime = this.sleeptime;
			if(sleeptime > 0) {
				final Thread thread = Thread.currentThread();
				this.parked.add(thread);
				LockSupport.parkNanos(sleeptime);
				this.parked.remove(thread);
			}
		} catch (InterruptedException e) {
			// Should not occur.
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			// Shutdown.
		}
	}

	@Override
	public void breakBarrier() {
		if(!this.broken.compareAndSet(false, true)) return;
		this.sleeptime = -1;
		if(this.barrier != null) this.barrier.reset();
		while(!this.parked.isEmpty()) {
			LockSupport.unpark(this.parked.poll());
		}
	}
	
	@Override
	public void setTimeLimit(final long value, final TimeUnit unit) {
		if(value < 0) throw new IllegalArgumentException("Time limit cannot be less than zero.");
		else if(unit == null) throw new IllegalArgumentException("Time unit cannot be null.");
		final long nano = unit.toNanos(value);
		this.limit.set(nano);
	}

	@Override
	public long getTimeLimit(final TimeUnit unit) {
		if(unit == null) throw new IllegalArgumentException("Time unit cannot be null.");
		final long nano = this.limit.get();
		return unit.convert(nano, TimeUnit.NANOSECONDS);
	}

	@Override
	public ITimeHandle getTimeHandle() {
		return this.timer;
	}

	@Override
	public boolean isBroken() {
		return this.broken.get();
	}

	/**
	 * <code>EndOfCycleTask</code> defines the task
	 * that is executed by the last reaching executor
	 * thread via the cyclic barrier to perform the
	 * book-keeping operations.
	 *
	 * @author Yi Wang (Neakor)
	 * @version Creation date: 11-30-2009 19:11 EST
	 * @version Modified date: 11-30-2009 19:14 EST
	 */
	private final class EndOfCycleTask implements Runnable {
		/**
		 * The <code>TimeUnit</code> used by the timer.
		 */
		private final TimeUnit unit;
		
		/**
		 * Constructor of <code>EndOfCycleTask</code>.
		 */
		private EndOfCycleTask() {
			this.unit = timer.getUnit();
		}
		
		@Override
		public void run() {
			// Update timer.
			timer.update();
			// Calculate the true execution time for the
			// past execution cycle.
			final long total = this.unit.toNanos(timer.getInterpolation());
			final long executionTime = total - lastsleep;
			// Calculate current cycle sleep time using
			// the true execution time with clamping.
			final long result = getTimeLimit(TimeUnit.NANOSECONDS) - executionTime;
			sleeptime = (result>0) ? result : 0;
			// Record current cycle sleep time as last
			// sleep time.
			lastsleep = sleeptime;
		}
	}
}
