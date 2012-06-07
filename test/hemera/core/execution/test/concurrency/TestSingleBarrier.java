package hemera.core.execution.test.concurrency;

import java.util.Scanner;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class TestSingleBarrier {
	
	private volatile boolean finished = false;
	
	public void start() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				final CyclicBarrier barrier = new CyclicBarrier(1);
				while(!finished) {
					try {
						barrier.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (BrokenBarrierException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
		new Thread(new Runnable() {
			@Override
			public void run() {
				final CyclicBarrier barrier = new CyclicBarrier(1);
				while(!finished) {
					try {
						barrier.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (BrokenBarrierException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	public static void main(String[] args) {
		final TestSingleBarrier test = new TestSingleBarrier();
		test.start();
		final Scanner scanner = new Scanner(System.in);
		while(true) {
			final String input = scanner.nextLine();
			if(input.equalsIgnoreCase("exit")) {
				test.finished = true;
				return;
			}
		}
	}
}
