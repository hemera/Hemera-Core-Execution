package hemera.core.execution.unittest.task;

import java.util.concurrent.TimeUnit;

import hemera.core.execution.interfaces.task.IResultTask;

public class ResultTask implements IResultTask<Integer> {

	private final int value;

	public ResultTask(final int value) {
		this.value = value;
	}

	@Override
	public Integer execute() throws Exception {
		// Simulate processing.
		final long start = System.currentTimeMillis();
		while (true) {
			final long current = System.currentTimeMillis();
			final long elapsed = current - start;
			if (elapsed >= 1000) break;
		}
		// Simulate IO.
		TimeUnit.SECONDS.sleep(1);
		return this.value;
	}
}
