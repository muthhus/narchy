package nars.op.software.prolog.io;

import nars.op.software.prolog.terms.Int;
import nars.op.software.prolog.terms.Prog;
import nars.op.software.prolog.terms.Sink;
import nars.op.software.prolog.terms.Term;

import java.io.IOException;
import java.io.Writer;

/**
 * Writer
 */
public class CharWriter extends Sink {
	protected Writer writer;

	public CharWriter(String f, Prog p) {
		super(p);
		this.writer = IO.toFileWriter(f);
	}

	public CharWriter(Prog p) {
		super(p);
		this.writer = IO.output;
	}

	public int putElement(Term t) {
		if (null == writer)
			return 0;
		try {
			char c = (char) ((Int) t).intValue();
			writer.write(c);
		} catch (IOException e) {
			return 0;
		}
		return 1;
	}

	public void stop() {
		if (null != writer && IO.output != writer) {
			try {
				writer.close();
			} catch (IOException e) {
			}
			writer = null;
		}
	}
}
