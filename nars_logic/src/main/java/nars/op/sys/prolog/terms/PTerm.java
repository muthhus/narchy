package nars.op.sys.prolog.terms;

import nars.op.sys.prolog.Prolog;
import nars.op.sys.prolog.io.Parser;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Top element of the Prolog term hierarchy. Describes a simple or compound ter
 * like: X,a,13,f(X,s(X)),[a,s(X),b,c], a:-b,c(X,X),d, etc.
 */
public abstract class PTerm implements Cloneable, Term {

	public final static Nil NIL = new Nil();
	public final static Const TRUE = new true_();
	public final static Const FAIL = new fail_();
	public final static Const YES = new Const("yes");
	public final static Const NO = new Const("no");
	public final static Const EOF = new Const("end_of_file");
	public final String name;


	public final static int JAVA = -4;

	public final static int REAL = -3;

	public final static int INT = -2;

	public final static int VAR = -1;

	public final static int CONST = 0;

	protected PTerm(String id) {
		this.name = id;
	}

	@Override
	public final int hashCode() {
		return name.hashCode();
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof PTerm)) return false;
		return name.equals(((PTerm)obj).name);
	}

	/**
	 * returns or fakes an arity for all subtypes
	 */
	abstract public int arity();

	/**
	 * Dereferences if necessary. If multi-threaded, this should be synchronized
	 * otherwise vicious non-reentrancy problems may occur in the presence of GC
	 * and heavy multi-threading!!!
	 */
	@NotNull
	abstract public PTerm ref();

	abstract boolean bind_to(PTerm that, Trail trail);

	/** Unify dereferenced */
	abstract boolean unify_to(PTerm that, Trail trail);

	/** Dereference and unify_to */
	protected final boolean unify(@NotNull PTerm that, Trail trail) {
		return ref().unify_to(that.ref(), trail);
	}

	protected void undo() { // does nothing
	}

	// public abstract boolean eq(Term that);

	public PTerm token() {
		return this;
	}

	// Term toTerm() {
	// return this;
	// }

	@NotNull
	public Clause toClause() {
		return new Clause(this, TRUE);
	}

	boolean isClause() {
		return false;
	}

	public static PTerm fromString(Prolog p, String s) {
		return Parser.stringToClause(p, s).toTerm();
	}

	/**
	 * Tests if this term unifies with that. Bindings are trailed and undone
	 * after the test. This should be used with the shared term as this and the
	 * new term as that. Synchronization makes sure that side effects on the
	 * shared term are not interfering, i.e as in:
	 * SHARED.matches(NONSHARED,trail).
	 */
	// synchronized
	public boolean matches(@NotNull PTerm that) {
		return matches(that, new Trail());
	}

	public boolean matches(@NotNull PTerm that, @NotNull Trail trail) {
		int oldtop = trail.size();
		boolean ok = unify(that, trail);
		trail.unwind(oldtop);
		return ok;
	}

	/**
	 * Returns a copy of the result if the unification of this and that. Side
	 * effects on this and that are undone using trailing of bindings..
	 * Synchronization happens over this, not over that. Make sure it is used as
	 * SHARED.matching_copy(NONSHARED,trail).
	 */
	// synchronized

	@Nullable
	public PTerm matching_copy(@NotNull PTerm that) {
		Trail trail = new Trail();
		boolean ok = unify(that, trail);
		// if(ok) that=that.copy();
		if (ok)
			that = copy();
		trail.unwind(0);
		return (ok) ? that : null;
	}

	/**
	 * Defines the reaction to an agent recursing over the structure of a term.
	 * <b>This</b> is passed to the agent and the result of the action is
	 * returned. Through overriding, for instance, a Fun term will provide the
	 * recursion over its arguments, by applying the action to each of them.
	 * 
	 * @see Fun
	 */
	@Nullable PTerm reaction(@NotNull PTerm agent) {
		return agent.action(this);
	}

	/**
	 * Identity action.
	 */
	PTerm action(PTerm that) {
		return that;
	}

	/**
	 * Returns a copy of a term with variables standardized apart (`fresh
	 * variables').
	 */
	// synchronized
	@Nullable
	public PTerm copy() {
		return reaction(new Copier());
	}

	/**
	 * Returns '[]'(V1,V2,..Vn) where Vi is a variable occuring in this Term
	 */
	@NotNull
	public PTerm varsOf() {
		return (new Copier()).getMyVars(this);
	}

	/**
	 * Replaces variables with uppercase constants named `V1', 'V2', etc. to be
	 * read back as variables.
	 */
	@Nullable
	public PTerm numbervars() {
		return copy().reaction(new VarNumberer());
	}

	/**
	 * Prints out a term to a String with variables named in order V1, V2,....
	 */
	public String pprint() {
		return numbervars().toString();
	}

	/*
	 * Returns an unquoted version of toString()
	 */
	public String toUnquoted() {
		return pprint();
	}

	/**
	 * Returns a string key used based on the string name of the term. Note that
	 * the key for a clause AL-B,C. is the key insted of ':-'.
	 */
	@Nullable
	public String getKey() {
		return toString();
	}

	/**
	 * Java Object wrapper. In particular, it is used to wrap a Thread to hide
	 * it inside a Prolog data object.
	 */
	public Object toObject() {
		return ref();
	}

	/*
	 * Just to catch the frequent error when the arg is forgotten while definig
	 * a builtin. Being final, it will generate a compile time error if this
	 * happens
	 */
	final int exec() {

		return -1;
	}

	/**
	 * Executed when a builtin is called. Needs to be overriden. Returns a
	 * run-time warning if this is forgotten.
	 */

	protected int exec(Prog p) {
		// IO.println("this should be overriden, prog="+p);
		return -1;
	}

	@NotNull
	static final Nonvar stringToChars(@NotNull String s) {
		if (0 == s.length())
			return NIL;
		Cons l = new Cons(new Int((s.charAt(0))), NIL);
		Cons curr = l;
		for (int i = 1; i < s.length(); i++) {
			Cons tail = new Cons(new Int((s.charAt(i))), NIL);
			curr.args[1] = tail;
			curr = tail;
		}
		return l;
	}

	@NotNull
	public Nonvar toChars() {
		return stringToChars(toUnquoted());
	}

	/**
	 * Converts a list of character codes to a String.
	 */
	public static String charsToString(Nonvar Cs) {
		StringBuilder s = new StringBuilder("");

		while (!(Cs instanceof Nil)) {
			if (!(Cs instanceof Cons))
				return null;
			Nonvar head = (Nonvar) ((Cons) Cs).arg(0);
			if (!(head instanceof Int))
				return null;
			char c = (char) ((Int) head).val;
			s.append(c);
			Cs = (Nonvar) ((Cons) Cs).arg(1);
		}

		return s.toString();
	}

	public boolean isBuiltin() {
		return false;
	}



}
