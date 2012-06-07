package hemera.core.execution.test.speed;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

public class TestIteration {

	private final int taskCount;
	private final int frameCount;
	private final float[] array;
	private final List<Float> arrayList;
	private final List<Float> copylist;
	private final Set<Float> hashSet;
	private final Queue<Float> concurrentQueue;
	private final Queue<Float> blockingQueue;
	private final Deque<Float> blockingDeque;
	private final Map<Float, Float> concurrentmap;
//	private final jsr166x.Deque<Float> concurrentDeque;
	
	private long startDirect;
	private long endDirect;
	
	private long startArray;
	private long endArray;
	
	private long startArrayList;
	private long endArrayList;
	
	private long startHashSet;
	private long endHashSet;
	
	private long startIndexing;
	private long endIndexing;
	
	private long startIterator;
	private long endIterator;
	
	private long startConcurrentQ;
	private long endConcurrentQ;
	
	private long startBlockingQ;
	private long endBlockingQ;
	
	private long startBlockingDeque;
	private long endBlockingDeque;
	
	private long startConcurrentMap;
	private long endConcurrentMap;
	
//	private long startConcurrentD;
//	private long endConcurrentD;
	
	public TestIteration() {
		this.taskCount = 3;
		this.frameCount = 60 * 3600 * 8; // 60 frames per second for 8 hours.
		this.array = new float[this.taskCount];
		this.arrayList = new ArrayList<Float>(this.taskCount);
		this.copylist = new CopyOnWriteArrayList<Float>();
		this.hashSet = new HashSet<Float>(this.taskCount);
		this.concurrentQueue = new ConcurrentLinkedQueue<Float>();
		this.blockingQueue = new LinkedBlockingQueue<Float>(this.taskCount);
		this.blockingDeque = new LinkedBlockingDeque<Float>(this.taskCount);
		this.concurrentmap = new ConcurrentHashMap<Float, Float>(this.taskCount);
//		this.concurrentDeque = new ConcurrentLinkedDeque<Float>();
	}

	public void start() {
		this.initialize();
		this.testDirect();
		System.out.println(this.testArrayIndexing());
		System.out.println(this.testArrayListIndexing());
		System.out.println(this.testHashSetIterator());
		System.out.println(this.testCopyIndexing());
		System.out.println(this.testCopyIterator());
		System.out.println(this.testConcurrentQueue());
		System.out.println(this.testBlockingDeque());
		System.out.println(this.testBlockingQueue());
		System.out.println(this.testConcurrentMap());
//		System.out.println(this.testConcurrentDeque());
		this.report();
	}
	
	private void initialize() {
		final Random random = new Random();
		for(int i = 0; i < this.taskCount; i++) {
			final float value = random.nextFloat();
			this.array[i] = value;
			this.arrayList.add(value);
			this.copylist.add(value);
			this.hashSet.add(value);
			this.concurrentQueue.add(value);
			this.blockingQueue.add(value);
			this.blockingDeque.add(value);
			this.concurrentmap.put(value, value);
//			this.concurrentDeque.add(value);
		}
	}
	
	private float testDirect() {
		this.startDirect = System.nanoTime();
		float temp = 0;
		for(int i = 0; i < this.frameCount; i++) {
			for(int j = 0; j < this.taskCount; j++) temp += this.taskCount;
		}
		this.endDirect = System.nanoTime();
		return temp;
	}
	
	private float testArrayIndexing() {
		this.startArray = System.nanoTime();
		float temp = 0;
		for(int i = 0; i < this.frameCount; i++) {
			for(int j = 0; j < this.array.length; j++) temp += this.array[j];
		}
		this.endArray = System.nanoTime();
		return temp;
	}
	
	private float testArrayListIndexing() {
		this.startArrayList = System.nanoTime();
		float temp = 0;
		for(int i = 0; i < this.frameCount; i++) {
			for(int j = 0; j < this.taskCount; j++) temp += this.arrayList.get(j);
		}
		this.endArrayList = System.nanoTime();
		return temp;
	}
	
	private float testHashSetIterator() {
		this.startHashSet = System.nanoTime();
		float temp = 0;
		for(int i = 0; i < this.frameCount; i++) {
			for(Float num : this.hashSet) temp += num;
		}
		this.endHashSet = System.nanoTime();
		return temp;
	}

	private float testCopyIndexing() {
		this.startIndexing = System.nanoTime();
		final int size = this.copylist.size();
		float temp = 0;
		for(int i = 0; i < this.frameCount; i++) {
			for(int j = 0; j < size; j++) temp += this.copylist.get(j);
		}
		this.endIndexing = System.nanoTime();
		return temp;
	}

	private float testCopyIterator() {
		this.startIterator = System.nanoTime();
		float temp = 0;
		for(int i = 0; i < this.frameCount; i++) {
			for(Float num : this.copylist) temp += num;
		}
		this.endIterator = System.nanoTime();
		return temp;
	}

	private float testConcurrentQueue() {
		this.startConcurrentQ = System.nanoTime();
		float temp = 0;
		for(int i = 0; i < this.frameCount; i++) {
			for(Float num : this.concurrentQueue) temp += num;
		}
		this.endConcurrentQ = System.nanoTime();
		return temp;
	}

	private float testBlockingDeque() {
		this.startBlockingDeque = System.nanoTime();
		float temp = 0;
		for(int i = 0; i < this.frameCount; i++) {
			for(Float num : this.blockingDeque) temp += num;
		}
		this.endBlockingDeque = System.nanoTime();
		return temp;
	}

	private float testBlockingQueue() {
		this.startBlockingQ = System.nanoTime();
		float temp = 0;
		for(int i = 0; i < this.frameCount; i++) {
			for(Float num : this.blockingQueue) temp += num;
		}
		this.endBlockingQ = System.nanoTime();
		return temp;
	}
	
	private float testConcurrentMap() {
		this.startConcurrentMap = System.nanoTime();
		float temp = 0;
		for(int i = 0; i < this.frameCount; i++) {
			final Collection<Float> values = this.concurrentmap.values();
			for(final Float v : values) temp += v;
		}
		this.endConcurrentMap = System.nanoTime();
		return temp;
	}
	
//	private float testConcurrentDeque() {
//		this.startConcurrentD = System.nanoTime();
//		float temp = 0;
//		for(int i = 0; i < this.frameCount; i++) {
//			for(Float num : this.concurrentDeque) temp += num;
//		}
//		this.endConcurrentD = System.nanoTime();
//		return temp;
//	}
	
	private void report() {
		System.out.println("Iteration times:");
		
		final long directDiff = this.endDirect - this.startDirect;
		final double directTime = (double)directDiff / 1000000.0;
		System.out.println("Direct access iteration time:        " + directTime);
		
		final long arrayDiff = this.endArray - this.startArray;
		final double arrayTime = (double)arrayDiff / 1000000.0;
		System.out.println("Array indexing time:                 " + arrayTime);
		
		final long arrayListDiff = this.endArrayList - this.startArrayList;
		final double arrayListTime = (double)arrayListDiff / 1000000.0;
		System.out.println("ArrayList indexing time:             " + arrayListTime);
		
		final long hashSetDiff = this.endHashSet - this.startHashSet;
		final double hashSetTime = (double)hashSetDiff / 1000000.0;
		System.out.println("HashSet iterator time:               " + hashSetTime);
		
		final long indexingDiff = this.endIndexing - this.startIndexing;
		final double indexingTime = (double)indexingDiff / 1000000.0;
		System.out.println("CopyOnWriteArrayList indexing time:  " + indexingTime);
		
		final long iteratorDiff = this.endIterator - this.startIterator;
		final double iteratorTime = (double)iteratorDiff / 1000000.0;
		System.out.println("CopyOnWriteArrayList iterator time:  " + iteratorTime);
		
		final long concurrentQDiff = this.endConcurrentQ - this.startConcurrentQ;
		final double concurrentQTime = (double)concurrentQDiff / 1000000.0;
		System.out.println("ConcurrentLinkedQueue iterator time: " + concurrentQTime);
		
		final long blockingDequeDiff = this.endBlockingDeque - this.startBlockingDeque;
		final double blockingDequeTime = (double)blockingDequeDiff / 1000000.0;
		System.out.println("LinkedBlockingDeque iterator time:   " + blockingDequeTime);
		
		final long blockingQDiff = this.endBlockingQ - this.startBlockingQ;
		final double blockingQTime = (double)blockingQDiff / 1000000.0;
		System.out.println("LinkedBlockingQueue iterator time:   " + blockingQTime);
		
		final long concurrentMapDiff = this.endConcurrentMap - this.startConcurrentMap;
		final double concurrentMapTime = (double)concurrentMapDiff / 1000000.0;
		System.out.println("ConcurrentHashMap values iterator time:   " + concurrentMapTime);
		
//		final long concurrentDDiff = this.endConcurrentD - this.startConcurrentD;
//		final double concurrentDTime = (double)concurrentDDiff / 1000000.0;
//		System.out.println("ConcurrentLinkedDeque iterator time: " + concurrentDTime);
	}

	public static void main(String[] args) {
		new TestIteration().start();
	}
}
