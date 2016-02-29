package nars.op.sys.prolog.terms;

/**
 * Template for builtins of arity >0
 */

abstract public class FunBuiltin extends Fun {

	public FunBuiltin(String f, int i) {
		super(f, i);
	}

	@Override
	abstract public int exec(Prog p);

	@Override
	public final boolean isBuiltin() {
		return true;
	}
}
