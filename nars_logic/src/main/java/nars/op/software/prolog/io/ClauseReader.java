package nars.op.software.prolog.io;

import nars.op.software.prolog.Prolog;
import nars.op.software.prolog.terms.*;

import java.io.IOException;
import java.io.Reader;

/**
 * Builds Fluents from Java Streams
 */
public class ClauseReader extends CharReader {
	protected Parser parser;

	public ClauseReader(Prolog prolog, Reader reader, Prog p) throws Exception {
		super(reader, p);
		make_parser(prolog, "from shared reader");
	}

	public ClauseReader(Prolog prolog, String f, Prog p) throws Exception {
		super(f, p);
		make_parser(prolog, f);
	}

	public ClauseReader(Prolog prolog, Prog p) throws Exception {
		super(p);
		make_parser(prolog, "standard input");
	}

	/**
	 * parses from a string representation of a term
	 */
	public ClauseReader(Prolog prolog, PTerm t, Prog p) throws Exception {
		super(t, p);
		make_parser(prolog, "string parser");
	}

	void make_parser(Prolog prolog, String f) throws Exception {
		if (null != reader)
			try {
				this.parser = new Parser(prolog, reader);
			} catch (IOException e) {
				IO.error("unable to build parser for: " + f);
			}
		else
			this.parser = null;
	}

	public PTerm getElement() {
		Clause C = null;
		if (// IO.peer!=null &&
		reader.equals(IO.input)) {
			String s = IO.promptln(">:");
			if (null == s || 0 == s.length())
				C = null;
			else
				C = new Clause(parser.prolog, s);
		} else if (null != parser) {
			if (parser.atEOF()) {
				C = null;
				stop();
			} else
				C = parser.readClause();
			if (C != null && C.head().equals(PTerm.EOF)) {
				C = null;
				stop();
			}
		}
		return extract_info(C);
	}

	static Fun extract_info(Clause C) {
		if (null == C)
			return null;
		PTerm Vs = C.varsOf();
		Clause SuperC = new Clause(Vs, C);
		SuperC.dict = C.dict;
		Clause NamedSuperC = SuperC.cnumbervars(false);
		PTerm Ns = NamedSuperC.head();
		PTerm NamedC = NamedSuperC.body();
		return new Fun("clause", C, Vs, NamedC, Ns);
	}

	public void stop() {
		super.stop();
		parser = null;
	}
}
