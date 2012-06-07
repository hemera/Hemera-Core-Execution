package hemera.core.execution.test.speed;

import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TestQueueRemoval {

	private final static int clientCount = 5000;
	
	public static void main(String[] args) {
		final class Entry {
			private final String value;
			public Entry(final float value) {
				this.value = String.valueOf(value);
			}
			
			@Override
			public boolean equals(Object o) {
				if(o instanceof Entry) {
					final Entry given = (Entry)o;
					return this.value.equals(given.value);
				}
				return false;
			}
		}
		final Queue<Entry> queue = new ConcurrentLinkedQueue<Entry>();
		final Random random = new Random();
		Entry target = null;
		for(int i = 0; i < clientCount; i++) {
			final float value = random.nextFloat();
			final Entry entry = new Entry(value);
			queue.add(entry);
			if(i == clientCount-1) target = entry;
		}
		final long start = System.nanoTime();
		final boolean result = queue.remove(target);
		final long end = System.nanoTime();
		System.out.println("Removed: " + result);
		final double milli = ((double)(end-start)) / 1000000.0;
		System.out.println("Worst case time handling " + clientCount + " clients: " + milli);
	}
}
