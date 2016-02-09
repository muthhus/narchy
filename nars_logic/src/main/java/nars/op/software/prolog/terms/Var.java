package nars.op.software.prolog.terms;

/**
 * Part of the Term hierarchy implmenting logical variables. They are subject to
 * reset by application of and undo action keep on the trail stack.
 */
public final class Var extends Term {
	protected Term val;

	public Var() {
		val = this;
	}

	public int arity() {
		return Term.VAR;
	}

	public final Term ref() {
		Term v = this.val;
		return (v == this) ? this : v.ref();
	}

	public boolean bind_to(Term x, Trail trail) {
		val = x;
		trail.add(this);
		return true;
	}

	protected void undo() {
		val = this;
	}

	boolean unify_to(Term that, Trail trail) {
		// expects: this, that are dereferenced
		// return (this==that)?true:val.bind_to(that,trail);
		return val.bind_to(that, trail);
	}

	public boolean eq(Term x) { // not a term compare!
		return ref() == x.ref();
	}

	public String getKey() {
		Term t = ref();
		if (t instanceof Var)
			return null;
		else
			return t.getKey();
	}

	Term reaction(Term agent) {

		Term R = agent.action(ref());

		if (!(R instanceof Var)) {
			R = R.reaction(agent);
		}

		return R;
	}

	protected final String name() {
		return '_' + Integer.toHexString(hashCode());
	}

	public String toString() {
		Term t = ref();
		return t == this ? name() : t.toString();
	}
}