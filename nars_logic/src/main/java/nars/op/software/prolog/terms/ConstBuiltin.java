package nars.op.software.prolog.terms;

/**
 * Template for builtins of arity 0
 */

abstract public class ConstBuiltin extends Const {

	public ConstBuiltin(String s) {
		super(s);
	}

	abstract public int exec(Prog p);

	public final boolean isBuiltin() {
		return true;
	}
}
