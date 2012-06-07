package hemera.core.execution.unittest;

import hemera.core.execution.ExecutionService;
import hemera.core.execution.exception.FileExceptionHandler;
import hemera.core.execution.interfaces.IExecutionService;
import hemera.core.execution.interfaces.exception.IExceptionHandler;

import junit.framework.TestCase;

public class AbstractTest extends TestCase {

	protected IExecutionService service;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		final int count = Runtime.getRuntime().availableProcessors();
		final IExceptionHandler handler = new FileExceptionHandler();
		this.service = new ExecutionService(count, handler);
		this.service.activate();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		this.service.shutdown();
	}
}
