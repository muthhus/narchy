package nars.op.software.prolog.fluents;

import nars.op.software.prolog.terms.Prog;
import nars.op.software.prolog.terms.Sink;
import nars.op.software.prolog.terms.PTerm;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * Builds Fluents from Java Streams
 */
public class TermCollector extends Sink {
	@Nullable
	protected ArrayList buffer;

	private final Prog p;

	public TermCollector(Prog p) {
		super(p);
		this.p = p;
		this.buffer = new ArrayList();
	}

	public int putElement(PTerm T) {
		buffer.add(T);
		return 1;
	}

	public void stop() {
		buffer = null;
	}

	@Nullable
	public PTerm collect() {
		return new JavaSource(buffer, p);
	}
}
