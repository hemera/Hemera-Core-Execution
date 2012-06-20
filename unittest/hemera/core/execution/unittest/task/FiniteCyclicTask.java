package hemera.core.execution.unittest.task;

import java.util.concurrent.TimeUnit;

import hemera.core.execution.interfaces.task.ICyclicTask;

public class FiniteCyclicTask implements ICyclicTask {
	
	public int count;
	private long last;

	@Override
	public void execute() throws Exception {
		final long elapsed = (this.last==0) ? 0 : System.currentTimeMillis() - this.last;
		System.out.println("Executed: " + count + ". Elapsed: " + elapsed);
		this.last = System.currentTimeMillis();
		this.count++;
	}

	@Override
	public int getCycleCount() {
		return 5;
	}

	@Override
	public long getCycleLimit(final TimeUnit unit) {
		return unit.convert(1, TimeUnit.SECONDS);
	}
}
