package hemera.core.execution.unittest;

import java.util.concurrent.TimeUnit;

import hemera.core.execution.interfaces.task.ICyclicTask;
import hemera.core.execution.interfaces.time.ITimeHandle;

public class TestCyclicException extends AbstractTest {

	public void test() {
		this.service.submit(new ExceptionTask());
		// Ensure JUnit does not shut down service
		// before the task gets executed.
		try {
			TimeUnit.SECONDS.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private class ExceptionTask implements ICyclicTask {
		private int i;
		
		@Override
		public void execute() {
			throw new RuntimeException("Exception thrown: " + (i++));
		}

		@Override
		public long getCycleLimit(TimeUnit unit) {
			return 0;
		}

		@Override
		public void setTimeHandle(ITimeHandle handle) {
		}

		@Override
		public void shutdown() {
		}
	}
}
