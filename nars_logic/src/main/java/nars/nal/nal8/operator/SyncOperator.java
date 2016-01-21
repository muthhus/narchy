package nars.nal.nal8.operator;

import nars.nal.nal8.AbstractOperator;

/**
 * Operator which executes synchronously (in current reasoner thread). Should be
 * used only if the operation procedure will not take long and block the
 * reasoner thread.
 */
public abstract class SyncOperator extends AbstractOperator {


	protected SyncOperator(String name) {
		super(name);
	}

	/** uses the implementation class's simpleName as the term */
	protected SyncOperator() {
		this(null);
	}

}
