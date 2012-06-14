package hemera.core.execution.unittest.assisted;

import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import hemera.core.execution.assisted.AssistedService;
import hemera.core.execution.exception.LogExceptionHandler;
import hemera.core.execution.interfaces.IExceptionHandler;
import hemera.core.execution.interfaces.IExecutionService;
import hemera.core.execution.interfaces.task.IEventTask;

public class TestAssistedShutdown extends TestCase {
	
	private int count;
	private IExceptionHandler handler;
	
	protected void setUp() throws Exception {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println("Completed");
			}
		}));
		this.count = Runtime.getRuntime().availableProcessors()+1;
		this.handler = new LogExceptionHandler();
	}

	public void testGraceful() {
		for(int i = 0; i < 1000; i++) {
			final IExecutionService service = new AssistedService(handler, count, 100, TimeUnit.MILLISECONDS);
			service.activate();
			service.shutdown();
		}
	}
	
	public void testGracefulWait() throws InterruptedException {
		for(int i = 0; i < 10; i++) {
			final IExecutionService service = new AssistedService(handler, count, 100, TimeUnit.MILLISECONDS);
			service.activate();
			service.submit(new BlockTask());
			service.shutdownAndWait();
			System.out.println("Finished one.");
		}
	}
	
	public void testForceful() {
		for(int i = 0; i < 1000; i++) {
			final IExecutionService service = new AssistedService(handler, count, 100, TimeUnit.MILLISECONDS);
			service.activate();
			service.forceShutdown();
		}
	}
	
	public void testTimedForceful() throws InterruptedException {
		for(int i = 0; i < 1000; i++) {
			final IExecutionService service = new AssistedService(handler, count, 100, TimeUnit.MILLISECONDS);
			service.activate();
			service.forceShutdown(10, TimeUnit.MILLISECONDS);
		}
	}
	
	private class BlockTask implements IEventTask {

		@Override
		public void execute() {
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
