package nars.op.sys.prolog.terms;

import org.jetbrains.annotations.NotNull;

public class Conj extends Cons {
	public Conj(PTerm x0, PTerm x1) {
		super(",", x0, x1);
	}
	public Conj(PTerm[] x0x1) {
		this(x0x1[0], x0x1[1]);
	}

	@NotNull
	public String conjToString() {
		PTerm h = args[0].ref();
		PTerm t = args[1].ref();
		StringBuilder s = new StringBuilder(watchNull(h));
		for (;;) {
			if (!(t instanceof Conj)) {
				s.append(",").append(t);
				break;
			} else {
				h = ((Conj) t).args[0].ref();
				t = ((Conj) t).args[1].ref();
				s.append(',').append(watchNull(h));
			}
		}
		return s.toString();
	}

	@NotNull
	public String toString() {
		return funToString();
	}

	static public final PTerm getHead(PTerm T) {
		T = T.ref();
		return (T instanceof Conj) ? ((Conj) T).arg(0) : T;
	}

	static public final PTerm getTail(PTerm T) {
		T = T.ref();
		return (T instanceof Conj) ? ((Conj) T).arg(1) : PTerm.TRUE;
	}
}
