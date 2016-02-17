package nars.op.software.prolog.fluents;

import nars.op.software.prolog.terms.PTerm;
import nars.op.software.prolog.terms.Prog;
import nars.op.software.prolog.terms.Source;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Builds Prolog Iterators from Java Sequences and Iterator type classes
 */
public class JavaSource extends Source {
	@Nullable
	private Iterator e;

	public JavaSource(Prog p) {
		super(p);
		e = null;
	}

	public JavaSource(Iterator iterator, Prog p) {
		super(p);
		this.e = iterator;
	}

	public JavaSource(@NotNull ArrayList V, Prog p) {
		super(p);
		this.e = V.iterator();
	}

	@Nullable
	public PTerm getElement() {
		if (null == e || !e.hasNext())
			return null;
		else
			return (PTerm) e.next();
	}

	public void stop() {
		e = null;
	}
}
