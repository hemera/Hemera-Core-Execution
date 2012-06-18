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
 * AssistedService ExecutorCount-1000 BufferSize-1000 Cost: 27575ms
 * ScalableService MinCount-1000 MaxCount-1000 Cost: 28768ms
 * Difference: 4.3%
 * 
 * Run-2:
 * AssistedService ExecutorCount-1000 BufferSize-1000 Cost: 27641ms
 * ScalableService MinCount-1000 MaxCount-1000 Cost: 29670ms
 * Difference: 7.3%
 * 
 * Run-3:
 * AssistedService ExecutorCount-1000 BufferSize-1000 Cost: 27094ms
 * ScalableService MinCount-1000 MaxCount-1000 Cost: 28601ms
 * Difference: 5.6%
 * 
 * Run-4:
 * AssistedService ExecutorCount-1000 BufferSize-1000 Cost: 27045ms
 * ScalableService MinCount-700 MaxCount-1000 Cost: 41002ms
 * Difference: 51.6%
 * 
 * Run-5:
 * AssistedService ExecutorCount-1000 BufferSize-1000 Cost: 27937ms
 * ScalableService MinCount-700 MaxCount-1000 Cost: 42669ms
 * Difference: 52.7%
 * 
 * Run-6:
 * AssistedService ExecutorCount-1000 BufferSize-1000 Cost: 27145ms
 * ScalableService MinCount-700 MaxCount-1000 Cost: 41287ms
 * Difference: 52.0%
 */
public class CompareTest extends TestCase implements IServiceListener {
	
	private final int executorCount = 1000;
	private final int scalableExecutorMin = (int)(this.executorCount*0.7);
	@SuppressWarnings("unchecked")
	private final IResultTask<Integer>[] tasks = new IResultTask[10000];
	private final int[] taskValues = new int[this.tasks.length];
	private final long taskDuration = 2000;
	
	private long assistedCost;
	private long scalableCost;
	
	public CompareTest() {
		final Random random = new Random();
		long lastDuration = 0;
		for(int i = 0; i < this.tasks.length; i++) {
			final int value = random.nextInt();
			if (i % 2 == 0) {
				final double percentage = random.nextDouble();
				lastDuration = (long)(this.taskDuration*2 * percentage);
				this.tasks[i] = new IOResultTask(value, lastDuration);
			} else {
				final long duration = this.taskDuration*2 - lastDuration;
				this.tasks[i] = new IOResultTask(value, duration);
			}
			this.taskValues[i] = value;
		}
		System.out.println("TaskCount-" + this.tasks.length + " TaskDuration-" + this.taskDuration);
		System.out.println("===========================================================");
	}
	
	public void test() throws Exception {
		this.runAssistedService();
		System.out.println("===========================================================");
		this.runScalableService();
		System.out.println("===========================================================");
		final double ratio = (double)this.scalableCost / (double)this.assistedCost - 1;
		final double percentage = ratio*100.0;
		final String percentageStr = String.valueOf(percentage);
		final int index = percentageStr.indexOf(".");
		System.err.println("Difference: " + percentageStr.substring(0, index+2) + "%");
	}
	
	private void runAssistedService() throws Exception {
		final IExceptionHandler handler = new LogExceptionHandler();
		final int buffersize = this.executorCount;
		final IAssistedService service = new AssistedService(handler, this, this.executorCount, buffersize, this.taskDuration/10, TimeUnit.MILLISECONDS);
		service.activate();
		System.out.println("AssistedService activated.");

		this.assistedCost = this.runTest(service);
		System.err.println("AssistedService ExecutorCount-" + this.executorCount + " BufferSize-" + buffersize + " Cost: " + this.assistedCost + "ms");
		
		service.shutdownAndWait();
		System.out.println("AssistedService shutdown.");
	}
	
	private void runScalableService() throws Exception {
		final IExceptionHandler handler = new LogExceptionHandler();
		final IScalableService service = new ScalableService(handler, this, this.scalableExecutorMin, this.executorCount, this.taskDuration/10, TimeUnit.MILLISECONDS);
		service.activate();
		System.out.println("ScalableService activated.");

		this.scalableCost = this.runTest(service);
		System.err.println("ScalableService MinCount-" + this.scalableExecutorMin + " MaxCount-" + this.executorCount + " Cost: " + this.scalableCost + "ms");
		
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
			final int expected = this.taskValues[i];
			final int result = handles[i].getAndWait();
			assertEquals(expected, result);
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
