package hemera.core.execution.test.concurrency;

import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TestCPUCapacity {
	
	private final int threadcount;
	private final CountDownLatch startLatch;
	private final CountDownLatch endLatch;
	private final AtomicLong count;
	private volatile boolean finished;
	
	public TestCPUCapacity(final int threadcount) {
		this.threadcount = threadcount;
		this.startLatch = new CountDownLatch(1);
		this.endLatch = new CountDownLatch(threadcount);
		this.count = new AtomicLong(0);
		this.finished = false;
	}

	public long start() {
		for(int i = 0; i < this.threadcount; i++) {
			this.createThread().start();
		}
		final Scanner scanner = new Scanner(System.in);
		System.out.println("Ready for start command.");
		while(true) {
			final String input = scanner.nextLine();
			if(input.equalsIgnoreCase("start")) {
				this.startLatch.countDown();
				System.out.println("Calculation started.");
				break;
			}
		}
		scanner.close();
		// Perform 30 seconds of calculation.
		try {
			TimeUnit.SECONDS.sleep(30);
			this.finished = true;
			System.out.println("Terminating calculation.");
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		try {this.endLatch.await();} catch (InterruptedException e) {e.printStackTrace();}
		return this.count.get();
	}
	
	private final Thread createThread() {
		return new Thread(new Runnable() {
			@Override
			public void run() {
				try {startLatch.await();} catch (InterruptedException e) {e.printStackTrace();}
				final Calculator calculator = new Calculator();
				int localcount = 0;
				@SuppressWarnings("unused")
				int result = 0;
				while(!finished) {
					result += calculator.calculate(30);
					localcount++;
				}
				count.addAndGet(localcount);
				endLatch.countDown();
			}
		});
	}
	
	private final class Calculator {
		
		private int calculate(final int n) {
			if(n < 0) throw new IllegalArgumentException("Invalid index.");
			else if(n < 2) return n;
			final int result = this.calculate(n-1) + this.calculate(n-2);
			return result;
		}
	}

	public static void main(String[] args) {
		final Scanner scanner = new Scanner(System.in);
		System.out.println("Please enter the number of threads to use.");
		final String input = scanner.nextLine();
		final int n = Integer.valueOf(input);
		System.out.println("Thread count: " + n);
		final long count = new TestCPUCapacity(n).start();
		System.out.println("Operation count: " + count);
	}
}
