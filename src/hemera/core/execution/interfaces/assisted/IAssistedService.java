package hemera.core.execution.interfaces.assisted;

import hemera.core.execution.interfaces.IExecutionService;

/**
 * <code>IAssistedService</code> is an extension of
 * <code>IExecutionService</code> that manages a fixed
 * pool of <code>IAssistExecutor</code> to provide
 * work-stealing enabled task execution service. This
 * pool does not grow or shrink. This design is based
 * on the concept of work-stealing, which is aimed to
 * provide better system performance and resource
 * utilization. No thread is idling as long as there
 * are tasks to be executed in the system.
 * <p>
 * This type of service is best suited for computation
 * tasks that are CPU-bound and do not perform many
 * blocking operations such as I/O. In such cases,
 * growing the pool of executors will not increase the
 * system performance, but the fixed pool with work-
 * stealing can improve load-balancing thus improving
 * system performance.
 * <p>
 * Tasks submitted to the assisted service are assigned
 * to a set of assist executors based on load-balancing
 * information. Initial task assignment is based on a
 * round-robin strategy, however assist executors may
 * perform work-stealing within the executor group to
 * provide automatic load balancing at execution time.
 * There is no upper bound for the executors task
 * buffers, therefore, memory leaks may occur if the
 * system is overloaded.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface IAssistedService extends IExecutionService {

	/**
	 * Try to assist other executors with their left
	 * over tasks. This method returns only when all
	 * executors task buffers are empty.
	 * @return <code>true</code> if one or more tasks
	 * have been executed. <code>false</code> if all
	 * other executors local buffers are empty at the
	 * time of invocation, i.e. no tasks were stolen.
	 */
	public boolean assist();
	
	/**
	 * Retrieve the average waiting task queue length
	 * of assist executors. This value is calculated
	 * using the total number of waiting tasks on all
	 * assist executors divide by the number of assist
	 * executors.
	 * @return The <code>double</code> average waiting
	 * task queue length.
	 */
	public double getAverageQueueLength();
}
