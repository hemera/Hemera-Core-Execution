package hemera.core.execution.unittest.task;

import hemera.core.execution.unittest.AbstractTest;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import hemera.core.execution.interfaces.task.ICyclicTask;
import hemera.core.execution.interfaces.task.handle.ICyclicTaskHandle;
import hemera.core.execution.interfaces.time.ITimeHandle;

public class TestTaskCyclic extends AbstractTest {
	
	public void test() throws Exception {
		final int n = 4;
		final ICyclicTaskHandle[] handles = new ICyclicTaskHandle[n];
		for(int i = 0; i < n; i++) {
			final ICyclicTaskHandle handle = this.service.submit(new TimestampTask());
			handles[i] = handle;
		}
		final Scanner scanner = new Scanner(System.in);
		while(true) {
			final String input = scanner.nextLine();
			if(input.equals("finish")) break;
			else {
				final int i = Integer.valueOf(input);
				if(i < 0 || i >= n) continue;
				handles[i].cancel();
				System.out.println("Canceled: " + i);
			}
		}
	}

	private class TimestampTask implements ICyclicTask {
		
		private ITimeHandle handle;

		@Override
		public void execute() {
			final long interpolation = this.handle.getUnit().toMillis(this.handle.getInterpolation());
			final StringBuilder builder = new StringBuilder();
			builder.append(Thread.currentThread().getName()).append(":\n");
			builder.append("Execution time: ").append(interpolation).append("ms. Expected: 2000ms\n");
			builder.append("Execution rate: ").append(this.handle.getUpdateRate()).append(". Expected: 0.5\n");
			builder.append("Stamp: ").append(System.currentTimeMillis()).append("\n");
			builder.append("\n");
			System.out.println(builder.toString());
		}

		@Override
		public long getCycleLimit(final TimeUnit unit) {
			return unit.convert(2, TimeUnit.SECONDS);
		}

		@Override
		public void setTimeHandle(final ITimeHandle handle) {
			this.handle = handle;
		}

		@Override
		public void shutdown() {
		}
	}
}
