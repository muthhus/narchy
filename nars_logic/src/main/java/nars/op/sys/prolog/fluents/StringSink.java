package nars.op.sys.prolog.fluents;

import nars.op.sys.prolog.terms.Const;
import nars.op.sys.prolog.terms.Prog;
import nars.op.sys.prolog.terms.Sink;
import nars.op.sys.prolog.terms.PTerm;
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

	@Override
	public int putElement(@NotNull PTerm t) {
		buffer.append(t.toUnquoted());
		return 1;
	}

	@Override
	public void stop() {
		buffer = null;
	}

	@Override
	@NotNull
	public PTerm collect() {
		return new Const(buffer.toString());
	}
}
