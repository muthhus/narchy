package nars.op.sys.prolog.io;

import nars.op.sys.prolog.terms.Const;
import nars.op.sys.prolog.terms.Fun;
import nars.op.sys.prolog.terms.PTerm;
import nars.op.sys.prolog.terms.Prog;
import org.jetbrains.annotations.NotNull;

/**
 * Writer
 */
public class ClauseWriter extends CharWriter {
	public ClauseWriter(@NotNull String f, Prog p) {
		super(f, p);
	}

	public ClauseWriter(Prog p) {
		super(p);
	}

	public int putElement(PTerm t) {
		if (null == writer)
			return 0;
		String s = null;
		if ((t instanceof Fun) && "$string".equals(((Fun) t).name)) {
			Const Xs = (Const) ((Fun) t).arg(0);
			s = PTerm.charsToString(Xs);
		} else
			s = t.pprint();
		IO.print(writer, s);
		return 1;
	}
}
