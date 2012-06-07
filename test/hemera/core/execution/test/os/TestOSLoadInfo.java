package hemera.core.execution.test.os;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TestOSLoadInfo implements Runnable {

	private final Thread handlerThread;
	private final Thread runnerThread;
	
	private final OperatingSystemMXBean CPU;
	private final MemoryMXBean memory;
	public final List<ByteBuffer> buffers;
	
	private final Lock pauseLock;
	private final Condition pause;
	
	private volatile boolean finished;
	private volatile boolean paused;
	private volatile TimeUnit sleepunit;
	private volatile long sleeptime;
	
	private TestOSLoadInfo() {
		this.handlerThread = new Thread(new Handler());
		this.runnerThread = new Thread(this);
		this.CPU = ManagementFactory.getOperatingSystemMXBean();
		this.memory = ManagementFactory.getMemoryMXBean();
		this.pauseLock = new ReentrantLock();
		this.pause = this.pauseLock.newCondition();
		this.buffers = new ArrayList<ByteBuffer>();
		
		this.sleepunit = TimeUnit.SECONDS;
		this.sleeptime = 1;
	}

	private void start() {
		this.handlerThread.start();
		this.runnerThread.start();
	}

	@Override
	public void run() {
		while(!this.finished) {
			if (this.paused) {
				this.pauseLock.lock();
				try {
					this.pause.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					this.pauseLock.unlock();
				}
			}
			if (this.sleepunit != null && this.sleeptime > 0) {
				try {
					this.sleepunit.sleep(this.sleeptime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private class Handler implements Runnable {
		private final Scanner input;

		private Handler() {
			this.input = new Scanner(System.in);
		}

		@Override
		public void run() {
			while(true) {
				System.out.println("Command:");
				final String command = this.input.nextLine();
				try {
					if (command.equalsIgnoreCase("exit")) {
						finished = true;
						return;
					} else if (command.equalsIgnoreCase("pause")) {
						this.pause();
					} else if (command.equalsIgnoreCase("resume")) {
						this.resume();
					} else if (command.equalsIgnoreCase("allocate")) {
						this.allocate();
					} else if (command.equalsIgnoreCase("free")) {
						this.free();
					} else if (command.contains("sleeptime")) {
						this.pause();
						this.setSleeptime(command);
						this.resume();
					} else if (command.equals("cpu")) {
						this.printCPU();
					} else if (command.equals("memory")) {
						this.printMemory();
					}
				} catch(Throwable e) {
					System.err.println(e.getMessage());
				}
			}
		}
		
		private void pause() {
			paused = true;
		}
		
		private void resume() {
			paused = false;
			pauseLock.lock();
			try {
				pause.signal();
			} finally {
				pauseLock.unlock();
			}
		}
		
		private void allocate() {
			final ByteBuffer buffer = ByteBuffer.allocate(102400000);
			buffers.add(buffer);
		}
		
		private void free() {
			buffers.clear();
			Runtime.getRuntime().gc();
		}
		
		private void printCPU() {
			System.out.println("CPU: " + CPU.getSystemLoadAverage());
		}
		
		private void printMemory() {
			final MemoryUsage heap = memory.getHeapMemoryUsage();
			final MemoryUsage nonheap = memory.getNonHeapMemoryUsage();
			final double committed = heap.getUsed() + nonheap.getUsed();
			final double total = heap.getMax() + nonheap.getMax();
			final double percentage = ((committed / total) * 100);
			System.out.println("Memory: " + percentage + "%");
		}
		
		private void setSleeptime(final String input) {
			final String[] args = input.split(" ");
			sleeptime = Long.valueOf(args[1]);
			final String unitStr = args[2];
			if (unitStr.equals("s")) {
				sleepunit = TimeUnit.SECONDS;
			} else if (unitStr.equals("ms")) {
				sleepunit = TimeUnit.MILLISECONDS;
			}
		}
	}

	public static void main(String[] args) {
		new TestOSLoadInfo().start();
	}
}
