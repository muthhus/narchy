package nars.op.java;

import nars.$;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;

/**
 * Converts POJOs to NAL Term's, and vice-versa
 */
public interface Termizer {

	Atomic TRUE = $.the("true");
	@Nullable
	Term FALSE = $.neg(TRUE);
	Atomic VOID = $.the("void");
	Atomic EMPTY = $.the("empty");
	Atomic NULL = $.the("null");

	@Nullable
	Term term(Object o);
	@Nullable
	Object object(Term t);

}
