package nars.op.software.prolog.terms;

public class Int extends Num {
	public Int(long i) {
		val = i;
	}

	final long val;

	public String name() {
		return String.valueOf(val);
	}

	boolean bind_to(Term that, Trail trail) {
		return super.bind_to(that, trail)
				&& ((double) val == (double) ((Int) that).val);
		// unbelievable but true: converting
		// to double is the only way to convince
		// Microsoft's jview that 1==1
		// $$ casting to double to be removed
		// once they get it right
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
