package nars.op.software.prolog.fluents;

import nars.op.software.prolog.terms.Const;
import nars.op.software.prolog.terms.Prog;
import nars.op.software.prolog.terms.Sink;
import nars.op.software.prolog.terms.PTerm;

/**
 * Builds Fluents from Java Streams
 */
public class StringSink extends Sink {
	protected StringBuilder buffer;

	public StringSink(Prog p) {
		super(p);
		this.buffer = new StringBuilder();
	}

	public int putElement(PTerm t) {
		buffer.append(t.toUnquoted());
		return 1;
	}

	public void stop() {
		buffer = null;
	}

	public PTerm collect() {
		return new Const(buffer.toString());
	}
}
