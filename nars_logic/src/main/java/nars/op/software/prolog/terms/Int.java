package nars.op.software.prolog.terms;

public class Int extends Num {

	public final long val;

	public Int(long i) {
		super(String.valueOf(i));
		val = i;
	}

	boolean bind_to(Term that, Trail trail) {
		return super.bind_to(that, trail)
				&& (val == ((Int) that).val);
		// unbelievable but true: converting
		// to double is the only way to convince
		// Microsoft's jview that 1==1
		// $$ casting to double to be removed
		// once they get it right
		// wow fuck M$

	}

	public final int arity() {
		return Term.INT;
	}

	public final long longValue() {
		return val;
	}

	public final int intValue() {
		return (int) val;
	}

	public final double getValue() {
		return val;
	}
}
