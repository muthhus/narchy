package nars.op.software.prolog.terms;

abstract public class Sink extends Fluent {

	public Sink(Prog p) {
		super(p);
	}

	abstract public int putElement(PTerm T);

	public PTerm collect() {
		return null;
	}
}
