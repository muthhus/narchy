package nars.op.software.prolog.terms;

/**
 * List Constructor. Cooperates with terminator Nil.
 * 
 * @see Nil
 */
public class Cons extends Fun {
	public Cons(String cons, Term x0, Term x1) {
		super(cons, x0, x1);
	}

	public Cons(Term x0, Term x1) {
		this(".", x0, x1);
	}

	public Term getHead() {
		return arg(0);
	}

	public Term getTail() {
		return arg(1);
	}

	/**
	 * List printer.
	 */
	public String toString() {
		Term h = arg(0);
		Term t = arg(1);
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
