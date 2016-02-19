package nars.op.software.prolog.fluents;

import nars.op.software.prolog.terms.Const;
import nars.op.software.prolog.terms.Prog;
import nars.op.software.prolog.terms.Sink;
import nars.op.software.prolog.terms.PTerm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds Fluents from Java Streams
 */
public class StringSink extends Sink {
	@Nullable
	protected StringBuilder buffer;

	public StringSink(Prog p) {
		super(p);
		this.buffer = new StringBuilder();
	}

	public int putElement(@NotNull PTerm t) {
		buffer.append(t.toUnquoted());
		return 1;
	}

	public void stop() {
		buffer = null;
	}

	@NotNull
	public PTerm collect() {
		return new Const(buffer.toString());
	}
}
