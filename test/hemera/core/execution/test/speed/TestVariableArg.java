package hemera.core.execution.test.speed;

import java.util.concurrent.TimeUnit;

public class TestVariableArg {
	
	private final long time;
	
	private long staticCount;
	private long variableCount;
	
	public TestVariableArg() {
		this.time = TimeUnit.NANOSECONDS.convert(5, TimeUnit.SECONDS);
	}
	
	public void start() {
		this.testStatic();
		this.testVariable();
		final double diff = this.staticCount - this.variableCount;
		String pre = null;
		if(diff > 0) pre = "Static argument is ";
		else pre = "Variable argument is ";
		System.out.println(pre + (diff/(double)this.staticCount)*100 + "% faster");
	}
	
	private void testStatic() {
		final StaticArg task = new StaticArg();
		long elapsed = 0;
		while(elapsed <  this.time) {
			final long start = System.nanoTime();
			task.execute();
			final long end = System.nanoTime();
			elapsed += (end - start);
		}
		System.out.println("Static execution count:   " + this.staticCount);
	}
	
	private void testVariable() {
		final VariableArg task = new VariableArg();
		long elapsed = 0;
		while(elapsed <  this.time) {
			final long start = System.nanoTime();
			task.execute();
			final long end = System.nanoTime();
			elapsed += (end - start);
		}
		System.out.println("Variable execution count: " + this.variableCount);
	}
	
	public static void main(String[] args) {
		new TestVariableArg().start();
	}
	
	private final class VariableArg {
		
		public void execute(Object... args) {
			variableCount++;
		}
	}
	
	private final class StaticArg {
		
		public void execute() {
			staticCount++;
		}
	}
}
