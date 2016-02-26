package nars.op.sys.prolog.terms;

import org.jetbrains.annotations.NotNull;

/**
 * Part of the Term hierarchy, implementing double float point numbers.
 * 
 * @see PTerm
 * @see Nonvar
 */
public class Real extends Num {

	public final double val;

	public Real(double i) {
		super(String.valueOf(i));
		val = i;
	}

	boolean bind_to(@NotNull PTerm that, Trail trail) {
		return super.bind_to(that, trail) && val == ((Real) that).val;
	}

	public final int arity() {
		return PTerm.REAL;
	}

	public final double getValue() {
		return val;
	}
}
