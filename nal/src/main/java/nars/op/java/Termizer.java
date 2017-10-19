package nars.op.java;

import nars.Op;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;

/**
 * Converts POJOs to NAL Term's, and vice-versa
 */
public interface Termizer {

	Term VOID = Atomic.the("void");
	Term EMPTY = Atomic.the("empty");
	Term NULL = Atomic.the("null");

	@Nullable
	Term term(Object o);

	@Nullable
	Object object(Term t);

}
