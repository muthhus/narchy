package nars.op.sys.prolog.terms;

import org.jetbrains.annotations.NotNull;

public class Int extends Num {

	public final long val;

	public Int(long i) {
		super(String.valueOf(i));
		val = i;
	}

	@Override
	boolean bind_to(@NotNull PTerm that, Trail trail) {
		return super.bind_to(that, trail)
				&& (val == ((Int) that).val);
		// unbelievable but true: converting
		// to double is the only way to convince
		// Microsoft's jview that 1==1
		// $$ casting to double to be removed
		// once they get it right
		// wow fuck M$

	}

	@Override
	public final int arity() {
		return PTerm.INT;
	}

	public final long longValue() {
		return val;
	}

	public final int intValue() {
		return (int) val;
	}

	@Override
	public final double getValue() {
		return val;
	}
}
