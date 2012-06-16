package hemera.core.execution.unittest.task;

import java.util.concurrent.TimeUnit;

import hemera.core.execution.interfaces.task.IEventTask;

public class IOEventTask implements IEventTask {

	@Override
	public void execute() throws Exception {
		// Simulate processing.
		final long start = System.currentTimeMillis();
		while (true) {
			final long current = System.currentTimeMillis();
			final long elapsed = current - start;
			if (elapsed >= 500) break;
		}
		// Simulate IO.
		TimeUnit.MILLISECONDS.sleep(1000);
	}
}
