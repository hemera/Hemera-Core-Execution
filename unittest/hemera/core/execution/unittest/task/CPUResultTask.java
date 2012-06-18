package hemera.core.execution.unittest.task;

import hemera.core.execution.interfaces.task.IResultTask;

public class CPUResultTask implements IResultTask<Integer> {
	
	private final int value;
	private final long duration;
	
	public CPUResultTask(final int value, final long duration) {
		this.value = value;
		this.duration = duration;
	}

	@Override
	public Integer execute() throws Exception {
		final long start = System.currentTimeMillis();
		while (true) {
			final long current = System.currentTimeMillis();
			final long elapsed = current - start;
			if (elapsed >= this.duration) break;
		}
		return this.value;
	}
}
