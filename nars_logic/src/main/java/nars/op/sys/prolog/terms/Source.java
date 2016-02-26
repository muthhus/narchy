package nars.op.sys.prolog.terms;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

abstract public class Source extends Fluent {

	public Source(Prog p) {
		super(p);
	}

	@Nullable
	abstract public PTerm getElement();

	@Nullable
	public Nonvar toList() {
		PTerm head = getElement();
		if (null == head)
			return PTerm.NIL;
		Cons l = new Cons(head, PTerm.NIL);
		Cons curr = l;
		for (;;) {
			head = getElement();
			if (null == head)
				break;
			Cons tail = new Cons(head, PTerm.NIL);
			curr.args[1] = tail;
			curr = tail;
		}
		return l;
	}

	@NotNull
	public PTerm toFun() {
		ArrayList V = new ArrayList();
		PTerm X;
		while (null != (X = getElement())) {
			V.add(X);
		}
		return Copier.VectorToFun(V);
	}
}
