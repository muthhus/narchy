package nars.op.software.prolog.terms;

import nars.Op;
import nars.term.Compound;
import nars.term.SubtermVisitor;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Symbolic constant, of arity 0.
 */

public class Const extends Nonvar {

	public final static Nil NIL = new Nil();

	public final static Const TRUE = new true_();

	public final static Const FAIL = new fail_();

	public final static Const YES = new Const("yes");

	public final static Const NO = new Const("no");

	public final static Const anEof = new Const("end_of_file");

	public final static Nonvar the(PTerm X) {
		return (null == X) ? Const.NO : new Fun("the", X);
	}


	/** null will cause this function to take its class simplename as the identifier */
	public Const(String s) {
		super( s ); //s.intern();
	}

	String qname() {
		return qname(name);
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
		return qname();
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

	public final String key() {
		return name + '/' + arity();
	}

}
