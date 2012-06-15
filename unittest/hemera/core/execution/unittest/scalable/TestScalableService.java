package hemera.core.execution.unittest.scalable;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import hemera.core.execution.interfaces.task.IEventTask;
import hemera.core.execution.interfaces.task.IResultTask;
import hemera.core.execution.interfaces.task.handle.IEventTaskHandle;
import hemera.core.execution.interfaces.task.handle.IResultTaskHandle;

/**
 * This test first submits more tasks than the service
 * can handle to see the service scales up. Then it
 * tries to see if the service scales down.
 */
public class TestScalableService extends AbstractScalableTest {

	private final int taskcount;
	private final ArrayList<IEventTaskHandle> eventhandles;
	private final ArrayList<IResultTaskHandle<Integer>> resulthandles;
	private final long waittime;
	private CountDownLatch submittedLatch;
	private AtomicInteger executedCount;

	public TestScalableService() {
		this.taskcount = this.max;
		this.eventhandles = new ArrayList<IEventTaskHandle>(this.taskcount);
		this.resulthandles = new ArrayList<IResultTaskHandle<Integer>>(this.taskcount);
		final long temp = TimeUnit.SECONDS.convert(this.timeoutValue*2, this.timeoutUnit);
		this.waittime = (temp<2) ? 2 : temp;
	}

	public void test() throws Exception {
		for (int i = 0; i < 10; i++) {
			this.runSingleTest(i);
		}
	}

	private void runSingleTest(int index) throws Exception {
		System.out.println("========================= Running single test " + index + " =============================");
		this.submittedLatch = new CountDownLatch(2);
		this.executedCount = new AtomicInteger();

		this.printExecutorCounts();
		this.runSubmitThread();

		System.out.println("Submit thread started. Wait " + this.waittime + " seconds to see service status...");
		TimeUnit.SECONDS.sleep(this.waittime);
		this.printExecutorCounts();

		System.out.println("Wait for all tasks to be submitted before continuing so the service won't shutdown.");
		this.submittedLatch.await();

		System.out.println("Executed count: " + this.executedCount.get());
		System.out.println("Waiting for event task to finish...");
		for (int i = 0; i < this.taskcount; i++) {
			final IEventTaskHandle handle = this.eventhandles.get(i);
			boolean executed = handle.await(1000, TimeUnit.MILLISECONDS);
			while (!executed) {
				System.out.println("Executed count: " + this.executedCount.get());
				this.printExecutorCounts();
				executed = handle.await(1000, TimeUnit.MILLISECONDS);
			}
		}
		System.out.println("Waiting for result task to finish...");
		for (int i = 0; i < this.taskcount; i++) {
			final IResultTaskHandle<Integer> handle = this.resulthandles.get(i);
			Object result = handle.getAndWait(1000, TimeUnit.MILLISECONDS);
			while (result == null) {
				System.out.println("Executed count: " + this.executedCount.get());
				this.printExecutorCounts();
				result = handle.getAndWait(1000, TimeUnit.MILLISECONDS);
			}
			assertEquals(i, result);
		}

		System.out.println("All tasks are completed.");
		this.printExecutorCounts();

		System.out.println("Wait " + this.waittime + " seconds to check service status...");
		TimeUnit.SECONDS.sleep(this.waittime);
		this.printExecutorCounts();
	}

	private void runSubmitThread() {
		final Thread eventthread = new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < taskcount; i++) {
					final EventTask task = new EventTask();
					final IEventTaskHandle handle = service.submit(task);
					eventhandles.add(handle);
				}
				submittedLatch.countDown();
			}
		});
		eventthread.start();
		final Thread resultthread = new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < taskcount; i++) {
					final ResultTask task = new ResultTask(i);
					final IResultTaskHandle<Integer> handle = service.submit(task);
					resulthandles.add(handle);
				}
				submittedLatch.countDown();
			}
		});
		resultthread.start();
	}

	private void printExecutorCounts() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Executor count: ").append(this.service.getCurrentExecutorCount());
		builder.append("; ");
		builder.append("Available count: ").append(this.service.getAvailableCount());
		System.out.println(builder.toString());
	}

	private class EventTask implements IEventTask {

		@Override
		public void execute() throws Exception {
			// Simulate processing.
			final long start = System.currentTimeMillis();
			while (true) {
				final long current = System.currentTimeMillis();
				final long elapsed = current - start;
				if (elapsed >= 1000) break;
			}
			// Simulate IO.
			TimeUnit.SECONDS.sleep(1);
			executedCount.incrementAndGet();
		}
	}

	private class ResultTask implements IResultTask<Integer> {
		private final int value;

		private ResultTask(final int value) {
			this.value = value;
		}

		@Override
		public Integer execute() throws Exception {
			// Simulate processing.
			final long start = System.currentTimeMillis();
			while (true) {
				final long current = System.currentTimeMillis();
				final long elapsed = current - start;
				if (elapsed >= 1000) break;
			}
			// Simulate IO.
			TimeUnit.SECONDS.sleep(1);
			executedCount.incrementAndGet();
			return this.value;
		}
	}
}
