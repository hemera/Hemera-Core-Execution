package hemera.core.execution.unittest.scalable;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import hemera.core.execution.interfaces.task.handle.IEventTaskHandle;
import hemera.core.execution.interfaces.task.handle.IResultTaskHandle;
import hemera.core.execution.unittest.task.IOEventTask;
import hemera.core.execution.unittest.task.IOResultTask;

/**
 * This test first submits more tasks than the service
 * can handle to see the service scales up. Then it
 * tries to see if the service scales down.
 * 
 * Recordings:
 * Executed 2000 tasks in average 16370 nanoseconds.
 */
public class TestScalableService extends AbstractScalableTest {

	private final int taskcount;
	private final ArrayList<IEventTaskHandle> eventhandles;
	private final ArrayList<IResultTaskHandle<Integer>> resulthandles;
	private final long waittime;
	private CountDownLatch submittedLatch;

	public TestScalableService() {
		this.taskcount = 500;
		this.eventhandles = new ArrayList<IEventTaskHandle>(this.taskcount);
		this.resulthandles = new ArrayList<IResultTaskHandle<Integer>>(this.taskcount);
		final long temp = TimeUnit.SECONDS.convert(this.timeoutValue*2, this.timeoutUnit);
		this.waittime = (temp<2) ? 2 : temp;
	}

	public void test() throws Exception {
		final int iteration = 10;
		long duration = 0;
		for (int i = 0; i < iteration; i++) {
			duration += this.runSingleTest(i);
		}
		final long average = duration / iteration;
		System.out.println("Executed " + this.taskcount*2 + " tasks in average " + average + " nanoseconds.");
	}

	private long runSingleTest(int index) throws Exception {
		System.out.println("========================= Running single test " + index + " =============================");
		this.submittedLatch = new CountDownLatch(2);
		this.printExecutorCounts();

		final long start = System.currentTimeMillis();
		this.runSubmitThread();

		System.out.println("Waiting for all tasks to be submitted before continuing so the service won't shutdown...");
		this.submittedLatch.await();
		this.printExecutorCounts();

		System.out.println("Waiting for tasks to complete...");
		for (int i = 0; i < this.taskcount; i++) {
			final IEventTaskHandle handle = this.eventhandles.get(i);
			boolean executed = handle.await(1000, TimeUnit.MILLISECONDS);
			while (!executed) {
				this.printExecutorCounts();
				executed = handle.await(1000, TimeUnit.MILLISECONDS);
			}
		}
		for (int i = 0; i < this.taskcount; i++) {
			final IResultTaskHandle<Integer> handle = this.resulthandles.get(i);
			Object result = handle.getAndWait(1000, TimeUnit.MILLISECONDS);
			while (result == null) {
				this.printExecutorCounts();
				result = handle.getAndWait(1000, TimeUnit.MILLISECONDS);
			}
			assertEquals(i, result);
		}
		final long end = System.currentTimeMillis();
		final long duration = end - start;

		System.out.println("All tasks are completed.");
		this.printExecutorCounts();

		System.out.println("Wait " + this.waittime + " seconds to check service status...");
		TimeUnit.SECONDS.sleep(this.waittime);
		this.printExecutorCounts();
		
		return duration;
	}

	private void runSubmitThread() {
		final Thread eventthread = new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < taskcount; i++) {
					final IOEventTask task = new IOEventTask();
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
					final IOResultTask task = new IOResultTask(i);
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
}
