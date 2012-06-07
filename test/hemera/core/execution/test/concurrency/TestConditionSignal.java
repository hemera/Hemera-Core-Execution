package hemera.core.execution.test.concurrency;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TestConditionSignal {

	private final Lock disposalLock;
	private final Condition disposalCondition;
	
	private TestConditionSignal() {
		this.disposalLock = new ReentrantLock();
		this.disposalCondition = this.disposalLock.newCondition();
	}
	
	private void tryDispose() {
		this.disposalLock.lock();
		try {
			try {
				final boolean signaled = this.disposalCondition.await(1, TimeUnit.SECONDS);
				System.out.println("Signaled: " + signaled);
			} catch (InterruptedException e) {
				// Should not occur.
				e.printStackTrace();
			}
		} finally {
			this.disposalLock.unlock();
		}
	}
	
	private void signalReuse() {
		this.disposalLock.lock();
		try {
			System.out.println("Try signal");
			TimeUnit.SECONDS.sleep(5);
			// The sleep time has definitely elapsed by this point.
			this.disposalCondition.signalAll();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			this.disposalLock.unlock();
		}
	}

	public static void main(String[] args) {
		final TestConditionSignal t = new TestConditionSignal();
		new Thread(new Runnable() {
			@Override
			public void run() {
				t.tryDispose();
			}
		}).start();
		new Thread(new Runnable() {
			@Override
			public void run() {
				t.signalReuse();
			}
		}).start();
	}
}
