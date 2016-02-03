package nars.op.software.prolog.terms;

import java.util.ArrayList;

public class ObjectStack extends ArrayList {
	public final void push(Object x) {
		add(x);
	}

	public final Object pop() {
		return remove(size() - 1);
	}
}
