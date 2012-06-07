package hemera.core.execution.interfaces.cycle;

/**
 * <code>IAssistBarrier</code> defines the interface
 * of a barrier unit that provides the functionality
 * to allow early-arriving executors to assist other
 * executors with their left over tasks in their task
 * buffers. This functionality is based on the concept
 * of work-stealing, which is aimed to provide better
 * system performance and resource utilization. No
 * thread is blocked as long as there is still task
 * to be executed in the system.
 * <p>
 * <code>IAssistBarrier</code> should only be used
 * with executors that are only responsible for the
 * execution of tasks that do not require dedicated
 * thread execution. Foreground event tasks are the
 * perfect example for this requirement.
 * <p>
 * <code>IAssistBarrier</code> should be constructed
 * with the group of event executors that assist each
 * other, in order to allow work-stealing to take place
 * when <code>assist</code> method is invoked. There
 * is no restrictions on invoking the <code>assist</code>
 * method really. However, external invocations should
 * be avoided as the completion time of invoking the
 * method is nondeterministic.
 * <p>
 * <code>IAssistBarrier</code> does not provide an
 * explicit break method. It relies on the fact that
 * if the system shuts down, all executors will be
 * shut down, thus the final assist will quickly
 * complete due to executor's internal termination
 * check.
 * <p>
 * Typically executors use assist barrier in following
 * pattern:
 * 1. Execute local task buffer until empty.
 * 2. Reach barrier to assist other executors until
 * all executor buffers are empty.
 * 3. Block until a new task is assigned to the local
 * buffer or eager-idling time expires. Go to step 1.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface IAssistBarrier {

	/**
	 * Reach the barrier and assist other executors
	 * with their left-over tasks.
	 * <p>
	 * There is no restrictions on invoking this
	 * method really. However, external invocations
	 * should be avoided as the completion time of
	 * invoking the method is nondeterministic. In
	 * other words, this method may block for a very
	 * long time.
	 */
	public void assist();
}
