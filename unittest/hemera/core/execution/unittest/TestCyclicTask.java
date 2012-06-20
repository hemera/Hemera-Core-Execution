package hemera.core.execution.unittest;

import hemera.core.execution.assisted.AssistedService;
import hemera.core.execution.exception.LogExceptionHandler;
import hemera.core.execution.interfaces.IExceptionHandler;
import hemera.core.execution.interfaces.IExecutionService;
import hemera.core.execution.interfaces.IServiceListener;
import hemera.core.execution.interfaces.task.handle.ICyclicTaskHandle;
import hemera.core.execution.scalable.ScalableService;
import hemera.core.execution.unittest.task.FiniteCyclicTask;
import hemera.core.execution.unittest.task.InfiniteCyclicTask;
import hemera.core.execution.unittest.task.SelfTerminatingCyclicTask;

import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

public class TestCyclicTask extends TestCase implements IServiceListener {

	public void testAssistedService() throws Exception {
		final IExceptionHandler handler = new LogExceptionHandler();
		final AssistedService service = new AssistedService(handler, this, 10, 100, 100, TimeUnit.MILLISECONDS);
		service.activate();
		System.out.println("Assisted Activated.");
		
		this.runFinite(service);
		System.out.println("=======================Assisted Finite task completed=========================");
		
		this.runInfinite(service);
		System.out.println("======================Assisted Infinite task completed========================");
		
		this.runSelfTerminatingInfinite(service);
		System.out.println("======================Assisted Self-terminating Infinite task completed========================");
		
		service.shutdownAndWait();
		System.out.println("Assisted Shutdown.");
	}
	
	public void testScalableService() throws Exception {
		final IExceptionHandler handler = new LogExceptionHandler();
		final ScalableService service = new ScalableService(handler, this, 20, 100, 200, TimeUnit.MILLISECONDS);
		service.activate();
		System.out.println("Scalable Activated.");
		
		this.runFinite(service);
		System.out.println("=======================Scalable Finite task completed=========================");
		
		this.runInfinite(service);
		System.out.println("======================Scalable Infinite task completed========================");
		
		this.runSelfTerminatingInfinite(service);
		System.out.println("======================Scalable Self-terminating Infinite task completed========================");
		
		service.shutdownAndWait();
		System.out.println("Scalable Shutdown.");
	}
	
	private void runFinite(final IExecutionService service) throws Exception {
		final FiniteCyclicTask finitetask = new FiniteCyclicTask();
		final ICyclicTaskHandle handle = service.submit(finitetask);
		handle.await();
		assertEquals(finitetask.getCycleCount(), finitetask.count);
	}
	
	private void runInfinite(final IExecutionService service) throws Exception {
		final InfiniteCyclicTask infinitetask = new InfiniteCyclicTask();
		final ICyclicTaskHandle handle = service.submit(infinitetask);
		final int waitseconds = 10;
		TimeUnit.SECONDS.sleep(waitseconds);
		handle.terminate();
		handle.await();
		final long multiplier = 1000 / infinitetask.getCycleLimit(TimeUnit.MILLISECONDS);
		final int expected = (int)(waitseconds*multiplier);
		final int diff = Math.abs(expected-infinitetask.count);
		assertTrue(diff<2);
	}
	
	private void runSelfTerminatingInfinite(final IExecutionService service) throws Exception {
		final int terminatingPoint = 17;
		final SelfTerminatingCyclicTask task = new SelfTerminatingCyclicTask(terminatingPoint);
		final ICyclicTaskHandle handle = service.submit(task);
		task.handle = handle;
		handle.await();
		assertEquals(terminatingPoint, task.count);
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
