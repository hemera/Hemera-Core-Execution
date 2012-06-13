package hemera.core.execution;

/**
 * <code>Executable</code> defines the abstraction of
 * an executable unit that provides the commonly shared
 * functionality by all types of executables represent
 * all types of tasks.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
abstract class Executable implements IExecutable {
	/**
	 * The <code>Boolean</code> started flag.
	 * <p>
	 * Since the execution thread, which sets this flag
	 * may be a different thread from the concurrent
	 * cancellation thread, the memory visibility needs
	 * to be guaranteed.
	 */
	volatile boolean started;
	/**
	 * The <code>Boolean</code> finished flag.
	 * <p>
	 * Since the execution thread, which sets this flag
	 * may be a different thread from the concurrent
	 * reading thread, the memory visibility needs to
	 * be guaranteed.
	 */
	volatile boolean finished;
	/**
	 * The <code>Boolean</code> canceled flag.
	 * <p>
	 * Since the cancellation thread, which sets this flag
	 * may be a different thread from the concurrent result
	 * retrieval thread or the logic execution thread, the
	 * memory visibility needs to be guaranteed, in order
	 * for retrieval operations to early exit.
	 */
	volatile boolean canceled;

	/**
	 * Constructor of <code>Executable</code>.
	 */
	Executable() {
		this.started = false;
		this.finished = false;
		this.canceled = false;
	}

	@Override
	public final void execute() throws Exception {
		// If canceled or finished, return.
		if (this.isCanceled() || this.hasFinished()) return;
		// Perform logic execution.
		else {
			this.started = true;
			this.doExecute();
			this.finished = true;
		}
	}
	
	/**
	 * Execute the task logic.
	 * @throws Exception if any processing failed.
	 */
	abstract void doExecute() throws Exception;
	
	@Override
	public boolean cancel() {
		if(this.hasStarted()) return false;
		else {
			this.canceled = true;
			return true;
		}
	}
	
	@Override
	public boolean hasStarted() {
		return this.started;
	}
	
	@Override
	public boolean hasFinished() {
		return this.finished;
	}

	@Override
	public boolean isCanceled() {
		return this.canceled;
	}
}
