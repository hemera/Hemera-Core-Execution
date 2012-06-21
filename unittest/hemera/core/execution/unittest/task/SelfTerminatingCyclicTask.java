package hemera.core.execution.unittest.task;

import java.util.concurrent.TimeUnit;

import hemera.core.execution.interfaces.task.ICyclicTask;

public class SelfTerminatingCyclicTask implements ICyclicTask {

	private final int terminatingPoint;
	private long last;
	
	public volatile int count;
	
	public SelfTerminatingCyclicTask(final int terminatingPoint) {
		this.terminatingPoint = terminatingPoint;
	}

	@Override
	public boolean execute() throws Exception {
		final long elapsed = (this.last==0) ? 0 : System.currentTimeMillis() - this.last;
		System.out.println("Executed: " + count + ". Elapsed: " + elapsed);
		this.last = System.currentTimeMillis();
		this.count++;
		if (this.count == this.terminatingPoint) return false;
		else return true;
	}

	@Override
	public int getCycleCount() {
		return 0;
	}

	@Override
	public long getCycleLimit(final TimeUnit unit) {
		return unit.convert(500, TimeUnit.MILLISECONDS);
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
