package hemera.core.execution.cycle;

import hemera.core.execution.interfaces.IAssistExecutor;
import hemera.core.execution.interfaces.cycle.IAssistBarrier;

/**
 * <code>AssistBarrier</code> defines the implementation
 * of a work-stealing based unit that enables the early
 * finishing executors to assist the more heavily loaded
 * ones with their left-over event tasks.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public class AssistBarrier implements IAssistBarrier {
	/**
	 * The array of <code>IAssistExecutor</code> units
	 * that perform work-stealing on each other.
	 * <p>
	 * The content of this array should never be modified.
	 * The contents are directly assigned by the executor
	 * context at construction time.
	 */
	private final IAssistExecutor[] executors;

	/**
	 * Constructor of <code>AssistBarrier</code>.
	 * @param executors The <code>IAssistExecutor</code>
	 * array that perform work-stealing on each other.
	 * The contents of the array do not have to be
	 * finalized when passed in, since a direct copy
	 * of reference is performed instead of copying
	 * of the contents.
	 */
	public AssistBarrier(final IAssistExecutor[] executors) {
		if(executors == null) throw new IllegalArgumentException("Executor array cannot be null.");
		this.executors = executors;
	}

	@Override
	public void assist() {
		// Cycle through all executors to assist.
		final int length = this.executors.length;
		for(int i = 0; i < length; i++) {
			final IAssistExecutor executor = this.executors[i];
			while(executor.executeTail());
			Thread.yield();
		}
	}
}
