package hemera.core.execution.test.timer;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import hemera.core.execution.interfaces.time.ITimer;
import hemera.core.execution.time.NanoTimer;

public class TestTimer {
	
	private final int framerate;
	private final ITimer timer;
	private final long sleepTime;
	
	private volatile boolean finished;

	private TestTimer(int framerate) {
		this.framerate = framerate;
		System.err.println("Given frame rate: " + this.framerate);
		this.timer = new NanoTimer();
		this.sleepTime = 1000/this.framerate;
	}
	
	private void start() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				while(!finished) {
					timer.update();
					try {
						TimeUnit.MILLISECONDS.sleep(sleepTime);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				while(!finished) {
					try {
						TimeUnit.MILLISECONDS.sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					final long interpolation = timer.getInterpolation();
					final long milliseconds = TimeUnit.MILLISECONDS.convert(interpolation, TimeUnit.NANOSECONDS);
					System.out.println("Interpolation: " + milliseconds + "ms");
					System.out.println("Frame rate: " + timer.getUpdateRate());
				}
			}
		}).start();
		final Scanner scanner = new Scanner(System.in);
		while(true) {
			final String input = scanner.nextLine();
			if(input.equalsIgnoreCase("exit")) {
				this.finished = true;
				return;
			}
		}
	}

	public static void main(String[] args) {
		new TestTimer(60).start();
	}
}
