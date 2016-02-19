package nars.op.software.prolog.fluents;

import nars.op.software.prolog.terms.PTerm;
import nars.op.software.prolog.terms.Prog;
import nars.op.software.prolog.terms.Source;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * An Infinite Source. If based on a finite Source, it moves to its the first
 * element after reaching its last element. A SourceLoop returns 'no' if the
 * original Source is empty. In case the original Source is infinite, a
 * SourceLoop will return the same elements as the original Source. (In
 * particular, this happens if the original Source is also a Source loop).
 */
public class SourceLoop extends Source {
	@Nullable
	private ArrayList v;

	@Nullable Source s;

	private int i;

	public SourceLoop(Source s, Prog p) {
		super(p);
		this.s = s;
		this.v = new ArrayList();
		this.i = 0;
	}

	private final PTerm getMemoized() {
		if (null == v || v.size() <= 0)
			return null;
		PTerm T = (PTerm) v.get(i);
		i = (i + 1) % v.size();
		return T;
	}

	@Nullable
	public PTerm getElement() {
		PTerm T = null;
		if (null != s) { // s is alive
			T = s.getElement();
			if (null != T)
				v.add(T);
			else {
				s = null;
			}
		}
		if (null == s)
			T = getMemoized();
		return T;
	}

	public void stop() {
		v = null;
		s = null;
	}
}
