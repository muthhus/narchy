package nars.op.software.prolog.io;

import nars.op.software.prolog.terms.Const;
import nars.op.software.prolog.terms.Fun;
import nars.op.software.prolog.terms.Prog;
import nars.op.software.prolog.terms.Term;

/**
 * Writer
 */
public class ClauseWriter extends CharWriter {
	public ClauseWriter(String f, Prog p) {
		super(f, p);
	}

	public ClauseWriter(Prog p) {
		super(p);
	}

	public int putElement(Term t) {
		if (null == writer)
			return 0;
		String s = null;
		if ((t instanceof Fun) && "$string".equals(((Fun) t).name)) {
			Const Xs = (Const) ((Fun) t).arg(0);
			s = Term.charsToString(Xs);
		} else
			s = t.pprint();
		IO.print(writer, s);
		return 1;
	}
}
