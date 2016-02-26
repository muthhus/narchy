package nars.op.sys.prolog.terms;

import nars.Op;
import nars.term.Compound;
import nars.term.SubtermVisitor;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.Predicate;

/**
 * Part of the Prolog Term hierarchy
 * 
 * @see PTerm
 */
public abstract class Nonvar extends PTerm {

	protected Nonvar(String id) {
		super(id);
	}

	boolean bind_to(@NotNull PTerm that, Trail trail) {
		return getClass() == that.getClass();
	}

	boolean unify_to(@NotNull PTerm that, Trail trail) {
		return bind_to(that, trail) || that.bind_to(this, trail);
	}

	@NotNull
	@Override
	public final PTerm ref() {
		return this;
	}

	// /**
	// returns a list representation of the object
	// */
	// Const listify() {
	// return new Cons(this,Const.aNil);
	// }

	@Nullable
	@Override
	public Op op() {
		return null;
	}

	@Override
	public int volume() {
		return 0;
	}

	@Override
	public int complexity() {
		return 0;
	}

	@Override
	public int structure() {
		return 0;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public boolean containsTerm(Term t) {
		return false;
	}

	@Override
	public boolean and(Predicate<? super Term> v) {
		return false;
	}

	@Override
	public boolean or(Predicate<? super Term> v) {
		return false;
	}

	@Override
	public void recurseTerms(@NotNull SubtermVisitor v, Compound parent) {

	}

	@Override
	public boolean isCommutative() {
		return false;
	}

	@Override
	public int varIndep() {
		return 0;
	}

	@Override
	public int varDep() {
		return 0;
	}

	@Override
	public int varQuery() {
		return 0;
	}
	@Override
	public int varPattern() {
		return 0;
	}
	@Override
	public int vars() {
		return 0;
	}

	@Override
	public void append(@NotNull Appendable w, boolean pretty) throws IOException {

	}

	@Override
	public
	@NotNull
	StringBuilder toStringBuilder(boolean pretty) {
		return null;
	}

	@Nullable
	@Override
	public String toString(boolean pretty) {
		return null;
	}

	@Override
	public boolean isCompound() {
		return false;
	}

	@Override
	public int compareTo(Object o) {
		return 0;
	}

}
