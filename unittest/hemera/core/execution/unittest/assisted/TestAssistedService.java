package hemera.core.execution.unittest.assisted;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import hemera.core.execution.interfaces.task.handle.IResultTaskHandle;
import hemera.core.execution.unittest.task.IOResultTask;

/**
 * Compare even-distribution with uneven distribution
 * to show that work-stealing improves the performance
 * even if the task execution time is unevenly assigned.
 * <p>
 * Even distribution:
 * Each executor gets the same amount of tasks
 * and each task contains the same amount of work.
 * <p>
 * Uneven distribution:
 * Each executor gets the same amount of tasks,
 * but the tasks assigned to one executor runs
 * a multiple times longer.
 * <p>
 * Without work-stealing:
 * Even-20 duration:   9465ms
 * Uneven-20 duration: 10323ms
 * <p>
 * With work-stealing:
 * Even-20 duration:   9459ms
 * Uneven-20 duration: 9616ms
 * <p>
 * As data above shows, with work-stealing, performance
 * of uneven-distribution, which is closer to real-world
 * conditions is much better.
 */
public class TestAssistedService extends AbstractAssistedTest {

	/**
	 * Use a single test to make sure the same
	 * service is shared for all tests.
	 */
	public void test() {
		// Task amount, must be even to allow each executor
		// get the same amount of tasks.
		final int n = 2;
		// Even.
		System.out.println("Even distribution:\nEach executor gets the same amount of tasks and each task has the same execution length.\n");
		this.runEven(n);
		// Uneven.
		//System.out.println("Uneven distribution:\nEach executor gets the same amount of tasks, but the tasks vary in execution length.\n");
		//this.runUneven(n);
	}

	@SuppressWarnings("unchecked")
	private void runEven(final int count) {
		final IResultTaskHandle<Integer>[] handles = new IResultTaskHandle[count];
		try {
			final long start = System.nanoTime();
			for(int i = 0; i < count; i++) {
				final IOResultTask task = new IOResultTask(i);
				handles[i] = this.service.submit(task);
			}
			System.out.println("submitted");
			for(int i = 0; i < count; i++) {
				final int result = handles[i].getAndWait();
				assertEquals(i, result);
			}
			final long end = System.nanoTime();
			final long duration = TimeUnit.NANOSECONDS.toMillis((end-start));
			System.err.println("Even-" + count + " duration:   " + duration + "ms\n");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	private void runUneven(final int count) {
		final IResultTaskHandle<Integer>[] handles = new IResultTaskHandle[count];
		try {
			final Random random = new Random();
			final long start = System.nanoTime();
			for(int i = 0; i < count; i++) {
				// Random possibility to create long or short tasks.
				final boolean shorttask = random.nextBoolean();
				if (shorttask) {
					handles[i] = this.service.submit(new IOResultTask(i, 500, 1000));
				} else {
					handles[i] = this.service.submit(new IOResultTask(i, 1500, 1500));
				}
			}
			for(int i = 0; i < count; i++) {
				final int result = handles[i].getAndWait();
				assertEquals(i, result);
			}
			final long end = System.nanoTime();
			final long duration = TimeUnit.NANOSECONDS.toMillis((end-start));
			System.err.println("Uneven-" + count + " duration: " + duration + "ms");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
