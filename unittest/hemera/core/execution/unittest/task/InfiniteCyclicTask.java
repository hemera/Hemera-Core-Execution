package hemera.core.execution.unittest.task;

import java.util.concurrent.TimeUnit;

public class InfiniteCyclicTask extends FiniteCyclicTask {

	@Override
	public int getCycleCount() {
		return 0;
	}

	@Override
	public long getCycleLimit(final TimeUnit unit) {
		return unit.convert(500, TimeUnit.MILLISECONDS);
	}
}
