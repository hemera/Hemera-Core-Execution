package hemera.core.execution.unittest.assisted;

import java.util.concurrent.TimeUnit;

import hemera.core.execution.assisted.AssistedService;
import hemera.core.execution.exception.LogExceptionHandler;
import hemera.core.execution.interfaces.IExceptionHandler;
import hemera.core.execution.interfaces.IServiceListener;
import hemera.core.execution.interfaces.assisted.IAssistedService;

import junit.framework.TestCase;

public class AbstractAssistedTest extends TestCase implements IServiceListener {

	protected IAssistedService service;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		final IExceptionHandler handler = new LogExceptionHandler();
		final int count = 10;
		final int buffersize = 100;
		this.service = new AssistedService(handler, this, count, buffersize, 100, TimeUnit.MILLISECONDS);
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
