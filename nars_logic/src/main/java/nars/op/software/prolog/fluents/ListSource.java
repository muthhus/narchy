package nars.op.software.prolog.fluents;

import nars.op.software.prolog.terms.Const;
import nars.op.software.prolog.terms.Copier;
import nars.op.software.prolog.terms.Prog;

/**
 * Builds an iterator from a list
 */
public class ListSource extends JavaSource {
	public ListSource(Const Xs, Prog p) {
		super(Copier.ConsToVector(Xs), p);
	}
}
