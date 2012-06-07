package hemera.core.execution.test.concurrency;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TestCopyListRemoval {

	public static void main(String[] args) {
		final List<String> list = new CopyOnWriteArrayList<String>();
		for(int i = 1; i < 11; i++) list.add(String.valueOf(i));
		// Remove divisible by 3.
		final Iterator<String> iterator = list.iterator();
		while(iterator.hasNext()) {
			final String string = iterator.next();
			final int value = Integer.valueOf(string);
			if(value % 3 == 0) list.remove(string);
		}
		final int size = list.size();
		for(int i = 0; i < size; i++) System.out.println(list.get(i));
	}
}
