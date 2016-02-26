package nars.op.sys.prolog.terms;

import org.jetbrains.annotations.Nullable;

abstract public class Sink extends Fluent {

	public Sink(Prog p) {
		super(p);
	}

	abstract public int putElement(PTerm T);

	@Nullable
	public PTerm collect() {
		return null;
	}
}
