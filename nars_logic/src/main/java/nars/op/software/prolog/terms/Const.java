package nars.op.software.prolog.terms;

import java.util.Objects;

/**
 * Symbolic constant, of arity 0.
 */

public class Const extends Nonvar {

	public final static Nonvar the(PTerm X) {
		return (null == X) ? PTerm.NO : new Fun("the", X);
	}


	/** null will cause this function to take its class simplename as the identifier */
	public Const(String s) {
		super( s  ); //s.intern();
	}

	public static String qname(String name) {
		if (0 == name.length())
			return "''";
		for (int i = 0; i < name.length(); i++) {
			if (!Character.isLowerCase(name.charAt(i)))
				return '\'' + name + '\'';
		}
		return name;
	}

	public String toString() {
		return qname(name);
	}

	boolean bind_to(PTerm that, Trail trail) {
		return super.bind_to(that, trail)
				&& Objects.equals(((Const) that).name, name);
	}


	/**
	 * returns an arity normally defined as 0
	 * 
	 * @see PTerm#CONST
	 */
	public int arity() {
		return PTerm.CONST;
	}

	/**
	 * creates a ConstBuiltin from a Const known to be registered as being a
	 * builtin while returning its argument unchanged if it is just a plain
	 * Prolog constant with no builtin code attached to it
	 */

	public final String toUnquoted() {
		return name;
	}

	public String getKey() {
		return name + '/' + arity();
	}

}
