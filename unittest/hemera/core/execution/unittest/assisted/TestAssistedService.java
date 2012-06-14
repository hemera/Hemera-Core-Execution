package hemera.core.execution.unittest.assisted;

import java.util.concurrent.TimeUnit;

import hemera.core.execution.interfaces.task.IResultTask;
import hemera.core.execution.interfaces.task.handle.IResultTaskHandle;

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

	private int multiplier;
	private int[] array;
	private int result;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		// Controls overall running time.
		this.multiplier = 50000;
		// Initialize array.
		final int n = 2000000;
		this.array = new int[n];
		for(int i = 0; i < n; i++) this.array[i] = i+1;
		// Compute expected result.
		this.result = (1+n)*(n/2)*this.multiplier;
		System.out.println("Setup completed!");
	}

	/**
	 * Use a single test to make sure the same
	 * service is shared for all tests.
	 */
	public void test() {
		// Task amount, must be even to allow each executor
		// get the same amount of tasks.
		final int n = 20;
		// Even.
		System.out.println("Even distribution:\nEach executor gets the same amount of tasks\nand each task contains the same amount of work.\n");
		this.runEven(n);
		// Uneven.
		final int multiple = 4;
		System.out.println("Uneven distribution:\nEach executor gets the same amount of tasks,\nbut the tasks assigned to one executor runs\n" + multiple + " times longer.\n");
		this.runUneven(n, multiple);
	}

	@SuppressWarnings("unchecked")
	private void runEven(final int count) {
		final AddTask[] tasks = this.newEvenTasks(count);
		final IResultTaskHandle<Integer>[] handles = new IResultTaskHandle[count];
		try {
			final long start = System.nanoTime();
			for(int i = 0; i < count; i++) {
				handles[i] = this.service.submit(tasks[i]);
			}
			int result = 0;
			for(int i = 0; i < count; i++) result += handles[i].getAndWait();
			final long end = System.nanoTime();
			if(result != this.result) fail();
			final long duration = TimeUnit.NANOSECONDS.toMillis((end-start));
			System.err.println("Even-" + count + " duration:   " + duration + "ms\n");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	private void runUneven(final int count, final int multiple) {
		final AddTask[] tasks = this.newUnevenTasks(count, multiple);
		final IResultTaskHandle<Integer>[] handles = new IResultTaskHandle[count];
		try {
			final long start = System.nanoTime();
			for(int i = 0; i < count; i++) {
				handles[i] = this.service.submit(tasks[i]);
			}
			int result = 0;
			for(int i = 0; i < count; i++) result += handles[i].getAndWait();
			final long end = System.nanoTime();
			if(result != this.result) fail();
			final long duration = TimeUnit.NANOSECONDS.toMillis((end-start));
			System.err.println("Uneven-" + count + " duration: " + duration + "ms");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private AddTask[] newEvenTasks(final int count) {
		final AddTask[] tasks = new AddTask[count];
		final int increment = this.array.length/count;
		for(int i = 0; i < count; i++) {
			final int start = i*increment;
			final int end = (i+1)*increment;
			final AddTask task = new AddTask(start, end);
			tasks[i] = task;
		}
		return tasks;
	}

	/**
	 * Small tasks are on odd indices. Large tasks
	 * are on even indices.
	 */
	private AddTask[] newUnevenTasks(final int count, final int multiple) {
		final AddTask[] tasks = new AddTask[count];
		// Total number of small tasks.
		final int numSmall = (count/2)*multiple + (count/2);
		// Length of a small task.
		final int sLength = this.array.length/numSmall;
		// Create small tasks.
		int laststart = 0;
		for(int i = 1; i < count; i += 2) {
			final int end = laststart + sLength;
			tasks[i] = new AddTask(laststart, end);
			laststart = end;
		}
		// Length of a large task.
		final int lLength = sLength * multiple;
		// Create large tasks.
		for(int i = 0; i < count; i += 2) {
			int end = -1;
			// Make sure the last task covers all.
			if(i+2 >= count) {
				end = this.array.length;
			} else {
				end = laststart + lLength;
			}
			tasks[i] = new AddTask(laststart, end);
			laststart = end;
		}
		return tasks;
	}

	private class AddTask implements IResultTask<Integer> {

		private final int start;
		private final int end;
		private final String str;

		public AddTask(final int start, final int end) {
			this.start = start;
			this.end = end;
			this.str = "["+start+","+end+"]";
		}

		@Override
		public Integer execute() {
			int temp = 0;
			for(int i = 0; i < multiplier; i++) {
				for(int j = this.start; j < this.end; j++) {
					temp += array[j];
				}
			}
			return temp;
		}
		
		public String toString() {
			return this.str;
		}
	}
}
