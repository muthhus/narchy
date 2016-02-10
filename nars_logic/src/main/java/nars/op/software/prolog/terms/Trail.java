package nars.op.software.prolog.terms;

import nars.util.data.list.FasterList;

/**
 * Implements a stack of undo actions for backtracking, and in particular,
 * resetting a Var's val fiels to unbound (i.e. this).
 * 
 * @see PTerm
 * @see Var
 */
public class Trail extends FasterList<PTerm> {

	public Trail() {
		super();
	}

//	public String name() {
//		return "trail" + hashCode() % 64;
//	}
//
//	public String pprint() {
//		return name() + '\n' + super.toString() + '\n';
//	}

	/**
	 * Used to undo bindings after unification, if we intend to leave no side
	 * effects.
	 */

	// synchronized
	final public void unwind(int to) {
		// IO.mes("unwind TRAIL: "+name()+": "+size()+"=>"+to);
		// if(to>size())
		// IO.assertion("unwind attempted from smaller to larger top");
		for (int i = size() - to; i > 0; i--) {
			removeLast().undo();
		}
	}

	// public String stat() {
	// return "Trail="+size();
	// }
}
