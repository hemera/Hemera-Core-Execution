package hemera.core.execution.enumn;

/**
 * <code>ETaskOrder</code> defines the enumeration that
 * presents the task buffer ordering in cyclic tasks.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public enum ETaskOrder {
	/**
	 * The front event task buffer, executed before the
	 * cyclic task logic.
	 */
	Front,
	/**
	 * The back event task buffer, executed after the
	 * cyclic task logic.
	 */
	Back;
}
