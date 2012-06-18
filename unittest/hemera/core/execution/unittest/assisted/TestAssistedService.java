package hemera.core.execution.unittest.assisted;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import hemera.core.execution.interfaces.task.handle.IResultTaskHandle;
import hemera.core.execution.unittest.task.IOResultTask;

/**
 * Compare uniform-distribution with non-uniform distribution
 * to show that work-stealing improves the performance
 * even if the task execution time is unevenly assigned.
 * 
 * Run-1 Without Work-stealing:
 * Uniform TaskCount-100 TaskDuration-1000 Cost: 10033ms
 * NonUniform TaskCount-100 TaskDuration-1000 Cost: 14725ms
 * 
 * Run-2 With Work-stealing:
 * Uniform TaskCount-100 TaskDuration-1000 Cost: 10020ms
 * NonUniform TaskCount-100 TaskDuration-1000 Cost: 11129ms
 * 
 * 32.3% Performance increase with work-stealing enabled.
 */
public class TestAssistedService extends AbstractAssistedTest {
	
	private final int taskCount = 100;
	private final long taskDuration = 1000;

	/**
	 * Use a single test to make sure the same
	 * service is shared for all tests.
	 */
	public void test() {
		System.out.println("Uniform distribution:\nEach executor gets the same amount of tasks and each task has the same execution length.\n");
		this.runUniform();
		
		System.out.println("Non-uniform distribution:\nEach executor gets the same amount of tasks, but the tasks vary in execution length.\n");
		this.runNonuniform();
	}

	@SuppressWarnings("unchecked")
	private void runUniform() {
		final IResultTaskHandle<Integer>[] handles = new IResultTaskHandle[this.taskCount];
		try {
			final long start = System.nanoTime();
			for(int i = 0; i < this.taskCount; i++) {
				final IOResultTask task = new IOResultTask(i, this.taskDuration);
				handles[i] = this.service.submit(task);
			}
			for(int i = 0; i < this.taskCount; i++) {
				final int result = handles[i].getAndWait();
				assertEquals(i, result);
			}
			final long end = System.nanoTime();
			final long duration = TimeUnit.NANOSECONDS.toMillis((end-start));
			System.err.println("Uniform TaskCount-" + this.taskCount + " TaskDuration-" + this.taskDuration + " Cost: " + duration + "ms");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	private void runNonuniform() {
		final IResultTaskHandle<Integer>[] handles = new IResultTaskHandle[this.taskCount];
		try {
			final Random random = new Random();
			final long start = System.nanoTime();
			long lastDuration = 0;
			for(int i = 0; i < this.taskCount; i++) {
				if (i % 2 == 0) {
					final double percentage = random.nextDouble();
					lastDuration = (long)(this.taskDuration*2 * percentage);
					handles[i] = this.service.submit(new IOResultTask(i, lastDuration));
				} else {
					final long duration = this.taskDuration*2 - lastDuration;
					handles[i] = this.service.submit(new IOResultTask(i, duration));
				}
			}
			for(int i = 0; i < this.taskCount; i++) {
				final int result = handles[i].getAndWait();
				assertEquals(i, result);
			}
			final long end = System.nanoTime();
			final long duration = TimeUnit.NANOSECONDS.toMillis((end-start));
			System.err.println("NonUniform TaskCount-" + this.taskCount + " TaskDuration-" + this.taskDuration + " Cost: " + duration + "ms");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
