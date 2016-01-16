package nars.java;

import nars.$;
import nars.term.Term;
import nars.term.atom.Atom;
import org.jetbrains.annotations.Nullable;

/**
 * Converts POJOs to NAL Term's, and vice-versa
 */
public interface Termizer {

	Atom TRUE = $.the("true");
	@Nullable
	Term FALSE = $.neg(TRUE);
	Atom VOID = $.the("void");
	Atom EMPTY = $.the("empty");
	Atom NULL = $.the("null");

	@Nullable
	Term term(Object o);
	@Nullable
	Object object(Term t);

}
