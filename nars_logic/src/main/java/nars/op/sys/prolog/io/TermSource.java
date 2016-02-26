package nars.op.sys.prolog.io;

import nars.op.sys.prolog.terms.*;
import org.jetbrains.annotations.Nullable;

/**
 * Maps a Term to an Source for iterating over its arguments
 */
public class TermSource extends Source {
	public TermSource(Nonvar val, Prog p) {
		super(p);
		this.val = val;
		pos = 0;
	}

	@Nullable
	private Nonvar val;

	private int pos;

	@Nullable
	public PTerm getElement() {
		PTerm X;
		if (null == val)
			X = null;
		else if (!(val instanceof Fun)) {
			X = val;
			val = null;
		} else if (0 == pos)
			X = new Const(val.name);
		else if (pos <= val.arity())
			X = ((Fun) val).arg(pos - 1);
		else {
			X = null;
			val = null;
		}
		pos++;
		return X;
	}

	public void stop() {
		val = null;
	}
}
