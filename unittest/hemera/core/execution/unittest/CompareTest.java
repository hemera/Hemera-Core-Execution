package hemera.core.execution.unittest;

import hemera.core.execution.assisted.AssistedService;
import hemera.core.execution.exception.LogExceptionHandler;
import hemera.core.execution.interfaces.IExceptionHandler;
import hemera.core.execution.interfaces.IExecutionService;
import hemera.core.execution.interfaces.IServiceListener;
import hemera.core.execution.interfaces.assisted.IAssistedService;
import hemera.core.execution.interfaces.scalable.IScalableService;
import hemera.core.execution.interfaces.task.IResultTask;
import hemera.core.execution.interfaces.task.handle.IResultTaskHandle;
import hemera.core.execution.scalable.ScalableService;
import hemera.core.execution.unittest.task.IOResultTask;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

/**
 * Compare the performance between scalable service and
 * assisted service.
 * 
 * Run-1:
 * AssistedService ExecutorCount-1000 BufferSize-1000 Cost: 3522ms
 * ScalableService MinCount-1000 MaxCount-1000 Cost: 4030ms
 * Difference: 14.4%
 * 
 * Run-2:
 * AssistedService ExecutorCount-1000 BufferSize-1000 Cost: 3332ms
 * ScalableService MinCount-1000 MaxCount-1000 Cost: 3951ms
 * Difference: 18.6%
 * 
 * Run-3:
 * AssistedService ExecutorCount-1000 BufferSize-1000 Cost: 3607ms
 * ScalableService MinCount-1000 MaxCount-1000 Cost: 4029ms
 * Difference: 11.7%
 */
public class CompareTest extends TestCase implements IServiceListener {
	
	private final int executorCount = 1000;
	@SuppressWarnings("unchecked")
	private final IResultTask<Integer>[] tasks = new IResultTask[1000];
	private final long taskDuration = 1000;
	
	public CompareTest() {
		final Random random = new Random();
		long lastDuration = 0;
		for(int i = 0; i < this.tasks.length; i++) {
			if (i % 2 == 0) {
				final double percentage = random.nextDouble();
				lastDuration = (long)(this.taskDuration*2 * percentage);
				this.tasks[i] = new IOResultTask(i, lastDuration);
			} else {
				final long duration = this.taskDuration*2 - lastDuration;
				this.tasks[i] = new IOResultTask(i, duration);
			}
		}
		System.out.println("TaskCount-" + this.tasks.length + " TaskDuration-" + this.taskDuration);
		System.out.println("===========================================================");
	}
	
	public void testAssistedService() throws Exception {
		final IExceptionHandler handler = new LogExceptionHandler();
		final int buffersize = this.executorCount;
		final IAssistedService service = new AssistedService(handler, this, this.executorCount, buffersize, this.taskDuration/10, TimeUnit.MILLISECONDS);
		service.activate();
		System.out.println("AssistedService activated.");

		final long cost = this.runTest(service);
		System.err.println("AssistedService ExecutorCount-" + this.executorCount + " BufferSize-" + buffersize + " Cost: " + cost + "ms");
		
		service.shutdownAndWait();
		System.out.println("AssistedService shutdown.");
		System.out.println("===========================================================");
	}
	
	public void testScalableService() throws Exception {
		final IExceptionHandler handler = new LogExceptionHandler();
		final int min = this.executorCount;
		final int max = this.executorCount;
		final IScalableService service = new ScalableService(handler, this, min, max, this.taskDuration/10, TimeUnit.MILLISECONDS);
		service.activate();
		System.out.println("ScalableService activated.");

		final long cost = this.runTest(service);
		System.err.println("ScalableService MinCount-" + min + " MaxCount-" + max + " Cost: " + cost + "ms");
		
		service.shutdownAndWait();
		System.out.println("ScalableService shutdown.");
	}
	
	@SuppressWarnings("unchecked")
	private long runTest(final IExecutionService service) throws Exception {
		final long start = System.nanoTime();
		final IResultTaskHandle<Integer>[] handles = new IResultTaskHandle[this.tasks.length];
		final CountDownLatch latch = new CountDownLatch(1);
		final Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < tasks.length; i++) {
					handles[i] = service.submit(tasks[i]);
				}
				latch.countDown();
			}
		});
		thread.start();
		latch.await();
		for(int i = 0; i < this.tasks.length; i++) {
			final int result = handles[i].getAndWait();
			assertEquals(i, result);
		}
		final long end = System.nanoTime();
		final long duration = TimeUnit.NANOSECONDS.toMillis((end-start));
		return duration;
	}
	
	@Override
	public void capacityReached() {
		System.err.println("Service capcity reached!!!");
	}

	@Override
	public long getFrequency(final TimeUnit unit) {
		return unit.convert(5, TimeUnit.SECONDS);
	}
}
