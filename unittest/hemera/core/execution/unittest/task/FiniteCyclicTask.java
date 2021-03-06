package hemera.core.execution.unittest.task;

import java.util.concurrent.TimeUnit;

import hemera.core.execution.interfaces.task.ICyclicTask;

public class FiniteCyclicTask implements ICyclicTask {
	
	private long last;
	public volatile int count;

	@Override
	public boolean execute() throws Exception {
		final long elapsed = (this.last==0) ? 0 : System.currentTimeMillis() - this.last;
		System.out.println("Executed: " + count + ". Elapsed: " + elapsed);
		this.last = System.currentTimeMillis();
		this.count++;
		return true;
	}

	@Override
	public int getCycleCount() {
		return 5;
	}

	@Override
	public long getCycleLimit(final TimeUnit unit) {
		return unit.convert(1, TimeUnit.SECONDS);
	}

	@Override
	public void cleanup() throws Exception {
		System.out.println("Cleanup task");
	}

	@Override
	public void signalTerminate() throws Exception {
		System.out.println("Signal terminate");
	}
}
