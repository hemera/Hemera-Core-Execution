package hemera.core.execution.test.os;

import hemera.core.execution.assisted.AssistedService;
import hemera.core.execution.exception.LogExceptionHandler;
import hemera.core.execution.interfaces.IExceptionHandler;
import hemera.core.execution.interfaces.IServiceListener;
import hemera.core.execution.listener.LogServiceListener;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class TestThrashing {

	public static void main(String[] args) throws Exception {
		final IExceptionHandler handler = new LogExceptionHandler();
		final IServiceListener listener = new LogServiceListener();
		final int count = 1000;
		final int buffersize = 100;
		AssistedService service = new AssistedService(handler, listener, count, buffersize, 200, TimeUnit.MILLISECONDS);
		service.activate();
		System.out.println("Activated.");
		
		final Scanner scanner = new Scanner(System.in);
		scanner.nextLine();
		System.out.println("Shuting down...");
		service.shutdownAndWait();
		System.out.println("Completed.");
	}
}
