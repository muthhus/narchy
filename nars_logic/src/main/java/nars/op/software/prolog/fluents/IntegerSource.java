package nars.op.software.prolog.fluents;

import nars.op.software.prolog.terms.Int;
import nars.op.software.prolog.terms.Prog;
import nars.op.software.prolog.terms.Source;
import nars.op.software.prolog.terms.Term;

/**
 * creates a source of integers based on x=a*x+b formula
 */
public class IntegerSource extends Source {

	public IntegerSource(long fuel, long a, long x, long b, Prog p) {
		super(p);
		this.fuel = fuel;
		this.a = a;
		this.b = b;
		this.x = x;
	}

	private long fuel;

	private final long a;

	private final long b;

	private long x;

	public Term getElement() {
		if (fuel <= 0)
			return null;
		Int R = new Int(x);
		x = a * x + b;
		--fuel;
		return R;
	}

	public void stop() {
		fuel = 0;
	}

	public String toString() {
		return "{(x->" + a + "*x+" + b + ")[" + fuel + "]=" + x + '}';
	}

}