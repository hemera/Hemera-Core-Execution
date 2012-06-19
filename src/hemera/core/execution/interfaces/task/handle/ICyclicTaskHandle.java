package hemera.core.execution.interfaces.task.handle;

/**
 * <code>ICyclicTaskHandle</code> defines an extension
 * to the <code>IEventTaskHandle</code> to provide the
 * additional support related to a cyclic task.
 * <p>
 * <code>ICyclicTaskHandle</code> provides the means to
 * gracefully terminate the corresponding cyclic task.
 * If the task has not been started, then the task will
 * not be executed at all. If the cyclic execution has
 * started, the task will terminate as soon as the
 * current cycle completes.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface ICyclicTaskHandle extends IEventTaskHandle {
	
	/**
	 * Terminate the cyclic task execution and allow the
	 * task to gracefully terminate.
	 */
	public void terminate();
}
