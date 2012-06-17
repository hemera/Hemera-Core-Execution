package hemera.core.execution.unittest.task;

import java.util.concurrent.TimeUnit;

import hemera.core.execution.interfaces.task.IResultTask;

public class IOResultTask implements IResultTask<Integer> {

	private final int value;
	private final long runtime;
	private final long iotime;
	
	public IOResultTask(final int value) {
		this(value, 500, 1000);
	}

	public IOResultTask(final int value, final long runtime, final long iotime) {
		this.value = value;
		this.runtime = runtime;
		this.iotime = iotime;
	}

	@Override
	public Integer execute() throws Exception {
		// Simulate processing.
		final long start = System.currentTimeMillis();
		while (true) {
			final long current = System.currentTimeMillis();
			final long elapsed = current - start;
			if (elapsed >= this.runtime) break;
		}
		// Simulate IO.
		TimeUnit.MILLISECONDS.sleep(this.iotime);
		return this.value;
	}
}
