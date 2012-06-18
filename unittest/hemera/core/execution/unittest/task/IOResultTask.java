package hemera.core.execution.unittest.task;

import java.util.concurrent.TimeUnit;

import hemera.core.execution.interfaces.task.IResultTask;

public class IOResultTask implements IResultTask<Integer> {

	private final int value;
	private final long duration;

	public IOResultTask(final int value, final long duration) {
		this.value = value;
		this.duration = duration;
	}

	@Override
	public Integer execute() throws Exception {
		// Simulate processing.
		final long start = System.currentTimeMillis();
		final long cputime = this.duration/2;
		while (true) {
			final long current = System.currentTimeMillis();
			final long elapsed = current - start;
			if (elapsed >= cputime) break;
		}
		// Simulate IO.
		TimeUnit.MILLISECONDS.sleep(this.duration-cputime);
		return this.value;
	}
}
