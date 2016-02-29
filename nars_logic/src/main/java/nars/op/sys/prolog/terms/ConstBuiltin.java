package nars.op.sys.prolog.terms;

/**
 * Template for builtins of arity 0
 */

abstract public class ConstBuiltin extends Const {

	public ConstBuiltin(String s) {
		super(s);
	}

	@Override
	abstract public int exec(Prog p);

	@Override
	public final boolean isBuiltin() {
		return true;
	}
}
