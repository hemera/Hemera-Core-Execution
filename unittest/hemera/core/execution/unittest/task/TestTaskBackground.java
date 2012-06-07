package hemera.core.execution.unittest.task;

import hemera.core.execution.unittest.AbstractTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import hemera.core.execution.interfaces.task.IEventTask;
import hemera.core.execution.interfaces.task.IResultTask;
import hemera.core.execution.interfaces.task.handle.IEventTaskHandle;
import hemera.core.execution.interfaces.task.handle.IResultTaskHandle;

/**
 * Use profiler to monitor recyclable executor re-usage.
 */
public class TestTaskBackground extends AbstractTest {
	
	private final AtomicInteger result = new AtomicInteger(0);
	private final Queue<Integer> values = new ConcurrentLinkedQueue<Integer>();

	public void test() throws InterruptedException {
		final List<IResultTaskHandle<Integer>> rhandles = new ArrayList<IResultTaskHandle<Integer>>();
		final List<IEventTaskHandle> ehandles = new ArrayList<IEventTaskHandle>();
		final Scanner scanner = new Scanner(System.in);
		System.out.println("Please enter \"event\" or \"result\" to indicate test task type.");
		final String str = scanner.nextLine();
		boolean event = false;
		if(str.equalsIgnoreCase("event")) event = true;
		else if(str.equalsIgnoreCase("result")) event = false;
		else throw new IllegalArgumentException("Invalid task type: ".concat(str));
		while(true) {
			final String input = scanner.nextLine();
			if(input.equals("finish")) {
				if(event) this.checkEvent(ehandles);
				else this.checkResult(rhandles);
				break;
			} else {
				final int n = Integer.valueOf(input);
				if(event) {
					for(int i = 0; i < n; i++) {
						final IEventTaskHandle handle = this.service.submitBackground(new EventTask());
						ehandles.add(handle);
					}
				} else {
					for(int i = 0; i < n; i++) {
						final IResultTaskHandle<Integer> handle = this.service.submitBackground(new ResultTask());
						rhandles.add(handle);
					}
				}
				System.out.println("Submitted: " + n);
			}
		}
	}
	
	private void checkEvent(final List<IEventTaskHandle> handles) throws InterruptedException {
		for(final IEventTaskHandle h : handles) h.await();
		int result = 0;
		for(final Integer i : this.values) result += i;
		if(result != this.result.get()) fail();
		System.out.println("Value: " + result);
	}
	
	private void checkResult(final List<IResultTaskHandle<Integer>> handles) throws InterruptedException {
		int result = 0;
		for(final IResultTaskHandle<Integer> h : handles) result += h.getAndWait();
		if(result != this.result.get()) fail();
		System.out.println("Value: " + result);
	}
	
	private class EventTask implements IEventTask {

		@Override
		public void execute() {
			try {
				TimeUnit.SECONDS.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			final Random random = new Random();
			final int i = random.nextInt();
			result.addAndGet(i);
			System.out.println(i);
			values.add(i);
		}
	}
	
	private class ResultTask implements IResultTask<Integer> {

		@Override
		public Integer execute() {
			try {
				TimeUnit.SECONDS.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			final Random random = new Random();
			final int i = random.nextInt();
			result.addAndGet(i);
			System.out.println(i);
			return i;
		}
	}
}
