package nars.op.software.prolog.terms;

/**
 * List Constructor. Cooperates with terminator Nil.
 * 
 * @see Nil
 */
public class Cons extends Fun {
	public Cons(String cons, PTerm x0, PTerm x1) {
		super(cons, x0, x1);
	}

	public Cons(PTerm x0, PTerm x1) {
		this(".", x0, x1);
	}

	public PTerm getHead() {
		return arg(0);
	}

	public PTerm getTail() {
		return arg(1);
	}

	/**
	 * List printer.
	 */
	public String toString() {
		PTerm h = arg(0);
		PTerm t = arg(1);
		StringBuilder s = new StringBuilder('[' + watchNull(h));
		for (;;) {
			if (t instanceof Nil) {
				s.append(']');
				break;
			} else if (t instanceof Cons) {
				h = ((Cons) t).arg(0);
				t = ((Cons) t).arg(1);
				s.append(',').append(watchNull(h));
			} else {
				s.append('|').append(watchNull(t)).append(']');
				break;
			}
		}
		return s.toString();
	}
}
