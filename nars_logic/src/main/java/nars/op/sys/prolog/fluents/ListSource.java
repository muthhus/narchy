package nars.op.sys.prolog.fluents;

import nars.op.sys.prolog.terms.Copier;
import nars.op.sys.prolog.terms.Nonvar;
import nars.op.sys.prolog.terms.Prog;

/**
 * Builds an iterator from a list
 */
public class ListSource extends JavaSource {
	public ListSource(Nonvar Xs, Prog p) {
		super(Copier.ConsToVector(Xs), p);
	}
}
