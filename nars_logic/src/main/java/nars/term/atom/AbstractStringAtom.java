package nars.term.atom;

import nars.Op;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 12/4/15.
 */
public abstract class AbstractStringAtom extends StringAtom {

	protected AbstractStringAtom(@NotNull byte[] id) {
		this(id, null);
	}
	protected AbstractStringAtom(String id) {
		this(id, null);
	}

	protected AbstractStringAtom(@NotNull byte[] id, Op specificOp) {
		this(new String(id), specificOp);
	}

	protected AbstractStringAtom(String id, Op specificOp) {
		super(id);
		// hash = Atom.hash(
		// id.hashCode(),
		// specificOp!=null ? specificOp : op()
		// );
	}

}
