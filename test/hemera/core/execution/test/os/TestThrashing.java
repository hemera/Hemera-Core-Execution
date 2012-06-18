package hemera.core.execution.test.os;

import hemera.core.execution.assisted.AssistedService;
import hemera.core.execution.exception.LogExceptionHandler;
import hemera.core.execution.interfaces.IExceptionHandler;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class TestThrashing {

	public static void main(String[] args) throws Exception {
		final IExceptionHandler handler = new LogExceptionHandler();
		final int count = 2000;
		final int buffersize = 100;
		AssistedService service = new AssistedService(handler, count, buffersize, 500, TimeUnit.MILLISECONDS);
		service.activate();
		System.out.println("Activated.");
		
		final Scanner scanner = new Scanner(System.in);
		scanner.nextLine();
		System.out.println("Shuting down...");
		service.shutdownAndWait();
		System.out.println("Completed.");
	}
}
