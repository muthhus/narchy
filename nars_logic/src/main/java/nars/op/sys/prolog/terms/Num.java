package nars.op.software.prolog.terms;

/**
 * Abstract numeric class, part of the Term hierarchy
 * 
 * @see Int
 * @see Real
 * @see PTerm
 */
public abstract class Num extends Nonvar {


	protected Num(String id) {
		super(id);
	}

	public String toString() {
		return name;
	}

	abstract public double getValue();
}