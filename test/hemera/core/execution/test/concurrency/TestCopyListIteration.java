package hemera.core.execution.test.concurrency;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class TestCopyListIteration {
	
	private final List<String> list;
	private final AtomicReference<Iterator<String>> iteratorRef;
	
	public TestCopyListIteration() {
		this.list = new CopyOnWriteArrayList<String>();
		this.iteratorRef = new AtomicReference<Iterator<String>>();
		for(int i = 0; i < 20; i++) {
			this.list.add(String.valueOf(i));
		}
	}
	
	private void start() {
		final CountDownLatch latch = new CountDownLatch(1);
		final Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {latch.await();} catch (InterruptedException e) {}
				iterate();
			}
		});
		final Thread t2 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {latch.await();} catch (InterruptedException e) {}
				iterate();
			}
		});
		final Thread t3 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {latch.await();} catch (InterruptedException e) {}
				iterate();
			}
		});
		t1.start();
		t2.start();
		t3.start();
		latch.countDown();
	}

	private void iterate() {
		// Memory locality.
		final List<String> observers = this.list;
		final AtomicReference<Iterator<String>> iteratorRef = this.iteratorRef;
		// Set a single iterator instance to be shared
		// among all executor threads.
		Iterator<String> iterator = iteratorRef.get();
		if(iterator == null) {
			iteratorRef.compareAndSet(null, observers.iterator());
			iterator = iteratorRef.get();
		}
		// Iteration.
		// This will not iterate duplicates as the
		// list supports thread-safe iteration.
		while(iterator.hasNext()) {
			final String value = iterator.next();
			System.out.print(Thread.currentThread());
			System.out.println(value);
		}
	}

	public static void main(String[] args) {
		new TestCopyListIteration().start();
	}
}
