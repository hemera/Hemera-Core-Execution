package hemera.core.execution.unittest.assisted;

import java.util.concurrent.TimeUnit;

import hemera.core.execution.assisted.AssistedService;
import hemera.core.execution.exception.LogExceptionHandler;
import hemera.core.execution.interfaces.IExceptionHandler;
import hemera.core.execution.interfaces.assisted.IAssistedService;

import junit.framework.TestCase;

public class AbstractAssistedTest extends TestCase {

	protected IAssistedService service;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		final IExceptionHandler handler = new LogExceptionHandler();
		final int count = Runtime.getRuntime().availableProcessors()+1;
		this.service = new AssistedService(handler, count, 100, TimeUnit.MILLISECONDS);
		this.service.activate();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		this.service.shutdown();
	}
}
