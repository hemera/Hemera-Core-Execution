package hemera.core.execution.unittest.scalable;

import hemera.core.execution.exception.LogExceptionHandler;
import hemera.core.execution.interfaces.IExceptionHandler;
import hemera.core.execution.interfaces.scalable.IScalableService;
import hemera.core.execution.scalable.ScalableService;
import hemera.core.utility.logging.CLogging;

import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

public class AbstractScalableTest extends TestCase {

	protected IScalableService service;
	protected final int min;
	protected final int max;
	protected final long timeoutValue;
	protected final TimeUnit timeoutUnit;
	
	public AbstractScalableTest() {
		CLogging.Directory.setValue("/Workspace/Hemera/LocalLog/");
		this.min = 20;
		this.max = 500;
		this.timeoutValue = 200;
		this.timeoutUnit = TimeUnit.MILLISECONDS;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		final IExceptionHandler handler = new LogExceptionHandler();
		this.service = new ScalableService(handler, this.min, this.max, this.timeoutValue, this.timeoutUnit);
		this.service.activate();
		System.out.println("Activated.");
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		this.service.shutdown();
		System.out.println("Completed.");
	}
}
