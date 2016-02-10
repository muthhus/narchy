package nars.op.software.prolog.terms;

import nars.op.software.prolog.Prolog;
import nars.op.software.prolog.io.IO;

import java.util.Iterator;

//!depends

/**
 * For a given clause g= A0:-<Guard>,A1,A2...,An, used as resolvent iterates
 * over its possible unfoldings (LD-resolution steps) with clauses of the form
 * B0:-B1,...,Bm in the default database. For each such step, a new clause
 * (A0:-B1,...,Bm,A2...,An)mgu(A1,B0) is built and returned by the Unfolder's
 * getElement method. Before the actual unfolding operations, builtins in Guard
 * are executed, possibly providing bindings for some variables or failing. In
 * case of failure of Guard or of unification, getElement() returns null.
 */
public class Unfolder extends Source {
	private int oldtop;

	private Iterator e;

	private Clause goal;

	private final Prog prog;

	/**
	 * Creates an Unfolder based on goal clause g for resolution step in program
	 * p. Iterator e is set to range over matching clauses in the database of
	 * program p.
	 */
	public Unfolder(Prolog prolog, Clause g, Prog p) {
		super(p);
		this.goal = g;
		this.prog = p;
		this.e = null;
		trace_goal(g);
		reduceBuiltins();
		if (null != goal) {
			PTerm first = goal.getFirst();
			if (null != first) {
				oldtop = prog.getTrail().size();
				this.e = prolog.db.toEnumerationFor(first.getKey());
				if (!e.hasNext())
					trace_nomatch(first);
			}
		} else
			trace_failing(g);
	}

	/**
	 * Overrides default trailing by empty action
	 */
	protected void trailMe(Prog p) {
		// IO.mes("not trailing"+this);
	}

	/**
	 * Extracts an answer at the end of an AND-derivation
	 */
	Clause getAnswer() {
		if (null != goal && goal.body() instanceof true_)
			return goal.ccopy();
		else
			return null;
	}

	/**
	 * Checks if this clause is the last one, allowing LCO
	 */
	final boolean notLastClause() {
		return (null != e) && e.hasNext();
	}

	/**
	 * Reduces builtin functions after the neck of a clause, before a "real"
	 * atom is unfolded
	 */
	final void reduceBuiltins() {
		for (;;) {
			PTerm first = goal.getFirst();
			if (null == first)
				break; // cannot reduce further
			if (first instanceof Conj) { // advances to next (possibly) inline
											// builtin
				goal = new Clause(goal.head(), Clause.appendConj(first,
						goal.getRest()));
				first = goal.getFirst();
			}

			int ok = first.exec(prog); // (possibly) executes builtin

			switch (ok) {

				case -1 : // nothing to do, this is not a builtin
					break;

				case 1 : // builtin suceeds
					// IO.mes("advancing: "+goal);
					goal = new Clause(goal.head(), goal.getRest());
					continue; // advance

				case 0 : // builtin fails
					goal = null;
					break; // get out

				default : // unexpected code: programming error
					IO.error("Bad return code:" + ok + ") in builtin: " + first);
					goal = null;
					break;
			}
			// IO.mes("leaving reduceBuiltins: "+goal);
			break; // leaves loop
		}
	}

	/**
	 * Returns a new clause by unfolding the goal with a matching clause in the
	 * database, or null if no such clause exists.
	 */
	public Clause getElement() {
		if (null == e)
			return null;
		Clause unfolded_goal = null;
		while (e.hasNext()) {
			PTerm T = (PTerm) e.next();
			if (!(T instanceof Clause))
				continue;
			// resolution step, over goal/resolvent of the form:
			// Answer:-G1,G2,...,Gn.
			prog.getTrail().unwind(oldtop);
			// unify() happens here !!!
			unfolded_goal = T.toClause()
					.unfold_with_goal(goal, prog.getTrail());
			if (null != unfolded_goal)
				break;
		}
		return unfolded_goal;
	}

	/**
	 * Stops production of more alternatives by setting the clause enumerator to
	 * null
	 */
	public void stop() {
		e = null;
	}

	/**
	 * Tracer on entering g
	 */
	static void trace_goal(Clause g) {
		switch (Prog.tracing) {
			case 2 :
				IO.println(">>>: " + g.getFirst());
				break;
			case 3 :
				IO.println(">>>: " + g.pprint());
				break;
		}
	}

	/**
	 * Tracer on exiting g
	 */
	static void trace_failing(Clause g) {
		switch (Prog.tracing) {
			case 2 :
				IO.println("FAILING CALL IN<<<: " + g.getFirst());
				break;
			case 3 :
				IO.println("FAILING CALL IN<<<: " + g.pprint());
				break;
		}
	}

	/**
	 * Tracer for undefined predicates
	 */
	static void trace_nomatch(PTerm first) {
		if (Prog.tracing > 0) {
			IO.println("*** UNDEFINED CALL: " + first.pprint());
		}
	}

	/**
	 * Returns a string representation of this unfolder, based on the original
	 * clause it is based on.
	 */
	public String toString() {
		return (null == goal) ? "{Unfolder}" : "{Unfolder=> " + goal.pprint()
				+ '}';
	}
}
