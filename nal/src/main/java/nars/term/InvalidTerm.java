package nars.term;

import nars.Op;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import static nars.nal.Tense.DTERNAL;

/**
 * Created by me on 2/26/16.
 */
public final class InvalidTerm extends RuntimeException {

    private final Op op;
    private final int rel;
    private final int dt;
    private final Term[] args;


    public InvalidTerm(@NotNull Termed x) {
        this(x.op(),
                x instanceof Compound ? ((Compound)x).relation() :  -1,
                x instanceof Compound ? ((Compound)x).dt() :  DTERNAL,
                x instanceof Compound ? ((Compound)x).terms() :  null);
    }

    public InvalidTerm(@NotNull Compound x /* incomplete or invalid */) {
        this(x.op(), x.relation(), x.dt(), x.terms());
    }

    public InvalidTerm(Term[] args) {
        this(null, -1, DTERNAL, args);
    }

    public InvalidTerm(Op op) {
        this(op, -1, DTERNAL, null);
    }

    public InvalidTerm(Op op, Term[] args) {
        this(op, -1, -1, args);
    }

    public InvalidTerm(Op op, int rel, int dt, Term[] args) {

        this.op = op;
        this.rel = rel;
        this.dt = dt;
        this.args = args;
    }

    @NotNull
    @Override
    public String getMessage() {
        return toString();
    }

    @NotNull
    @Override
    public String toString() {
        if (op!=null)
            return getClass().getSimpleName() + '{' +
                    op +
                    ", rel=" + rel +
                    ", dt=" + dt +
                    ", args=" + Arrays.toString(args) +
                    '}';
        else
            return super.toString();
    }
}
