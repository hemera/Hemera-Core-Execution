package hemera.core.execution.test.concurrency;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Volatile ensures a happens-before relationship, thus
 * the integer "value" does not need to be volatile it-
 * self, since it is always accessed after reading the
 * volatile flag and set before writing the flag.
 */
public class VolatileHappensBefore {
	
	private volatile boolean flag;
	
	private int value;
	
	public void start() {
		final AtomicInteger count = new AtomicInteger(0);
		for(int i = 0; i < 1000000; i++) {
			final Thread setter = new Thread(new Runnable() {
				@Override
				public void run() {
					value = 100;
					flag = true;
				}
			});
			final Thread getter = new Thread(new Runnable() {
				@Override
				public void run() {
					if(flag) {
						if(value != 100) System.err.println("Stale value!");
						else count.incrementAndGet();
					} else if(value != 0) System.err.println("Weird!");
				}
			});
			setter.start();
			getter.start();
			try {
				setter.join();
				getter.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Completed");
		System.out.println("Suceeded: ".concat(String.valueOf(count.get())));
	}

	public static void main(String[] args) {
		new VolatileHappensBefore().start();
	}
}
