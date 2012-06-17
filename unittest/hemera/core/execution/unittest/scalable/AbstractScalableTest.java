package hemera.core.execution.unittest.scalable;

import hemera.core.execution.exception.LogExceptionHandler;
import hemera.core.execution.interfaces.IExceptionHandler;
import hemera.core.execution.interfaces.IServiceListener;
import hemera.core.execution.interfaces.scalable.IScalableService;
import hemera.core.execution.scalable.ScalableService;
import hemera.core.utility.logging.CLogging;

import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

public class AbstractScalableTest extends TestCase implements IServiceListener {

	protected IScalableService service;
	protected final int min;
	protected final int max;
	protected final long timeoutValue;
	protected final TimeUnit timeoutUnit;
	
	public AbstractScalableTest() {
		CLogging.Directory.setValue("/Workspace/Hemera/LocalLog/");
		this.min = 20;
		this.max = 100;
		this.timeoutValue = 200;
		this.timeoutUnit = TimeUnit.MILLISECONDS;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		final IExceptionHandler handler = new LogExceptionHandler();
		this.service = new ScalableService(handler, this, this.min, this.max, this.timeoutValue, this.timeoutUnit);
		this.service.activate();
		System.out.println("Activated.");
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		this.service.shutdownAndWait();
		System.out.println("Completed.");
	}

	@Override
	public void capacityReached() {
		System.err.println("Service capcity reached!!!");
	}

	@Override
	public long getFrequency(final TimeUnit unit) {
		return unit.convert(5, TimeUnit.SECONDS);
	}
}
