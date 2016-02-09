package nars.op.software.prolog.terms;

/**
 * Part of the Prolog Term hierarchy
 * 
 * @see Term
 */
public abstract class Nonvar extends Term {

	public abstract String name();

	boolean bind_to(Term that, Trail trail) {
		return getClass() == that.getClass();
	}

	boolean unify_to(Term that, Trail trail) {
		return bind_to(that, trail) || that.bind_to(this, trail);
	}

	@Override
	public final Term ref() {
		return this;
	}

	// /**
	// returns a list representation of the object
	// */
	// Const listify() {
	// return new Cons(this,Const.aNil);
	// }
}
