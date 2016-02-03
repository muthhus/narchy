package nars.op.software.prolog.terms;

import java.util.Objects;

/**
 * Symbolic constant, of arity 0.
 */

public class Const extends Nonvar {

	public final static Nil aNil = new Nil();

	public final static Const aTrue = new true_();

	public final static Const aFail = new fail_();

	public final static Const aYes = new Const("yes");

	public final static Const aNo = new Const("no");

	public final static Const anEof = new Const("end_of_file");

	public final static Const the(Term X) {
		return (null == X) ? Const.aNo : new Fun("the", X);
	}

	private final String sym;

	public Const(String s) {
		sym = s.intern();
	}

	public final String name() {
		return sym;
	}

	public String qname() {
		if (0 == sym.length())
			return "''";
		for (int i = 0; i < sym.length(); i++) {
			if (!Character.isLowerCase(sym.charAt(i)))
				return '\'' + sym + '\'';
		}
		return sym;
	}

	public String toString() {
		return qname();
	}

	boolean bind_to(Term that, Trail trail) {
		return super.bind_to(that, trail)
				&& Objects.equals(((Const) that).sym, sym);
	}

	public String getKey() {
		return sym;
	}

	/**
	 * returns an arity normally defined as 0
	 * 
	 * @see Term#CONST
	 */
	public int arity() {
		return Term.CONST;
	}

	/**
	 * creates a ConstBuiltin from a Const known to be registered as being a
	 * builtin while returning its argument unchanged if it is just a plain
	 * Prolog constant with no builtin code attached to it
	 */

	public final String toUnquoted() {
		return name();
	}

	public final String key() {
		return name() + '/' + arity();
	}
}
