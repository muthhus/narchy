package nars.op.sys.prolog.fluents;

import nars.op.sys.prolog.terms.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Merges a List of Sources into a new Source which (fairly) iterates over them
 * breadth first.
 */
public class SourceMerger extends JavaSource {
	public SourceMerger(Const Xs, Prog p) {
		super(p);
		this.Q = new Queue(Copier.ConsToVector(Xs));
	}

	@NotNull
	private final Queue Q;

	@Nullable
	public PTerm getElement() {
		if (null == Q)
			return null;
		while (!Q.isEmpty()) {
			Source current = (Source) Q.deq();
			if (null == current)
				continue;
			PTerm T = current.getElement();
			if (null == T)
				continue;
			Q.enq(current);
			return T;
		}
		return null;
	}
}
