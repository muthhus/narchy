package nars.op.java;

import nars.term.Term;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;

/**
 * Converts POJOs to NAL Term's, and vice-versa
 */
public interface Termizer {

	Atomic TRUE = Atomic.the("true");
	@Nullable
	Term FALSE = TRUE.neg();
    Atomic VOID = Atomic.the("void");
	Atomic EMPTY = Atomic.the("empty");
	Atomic NULL = Atomic.the("null");

	@Nullable
	Term term(Object o);
	@Nullable
	Object object(Term t);

}
