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
    private final int dt;
    private final Term[] args;


    public InvalidTerm(@NotNull Termed x) {
        this(x.op(),
                x instanceof Compound ? ((Compound)x).dt() :  DTERNAL,
                x instanceof Compound ? ((Compound)x).terms() :  null);
    }

    public InvalidTerm(@NotNull Compound x /* incomplete or invalid */) {
        this(x.op(), x.dt(), x.terms());
    }

    public InvalidTerm(Term[] args) {
        this(null, DTERNAL, args);
    }

    public InvalidTerm(Op op) {
        this(op, DTERNAL, null);
    }

    public InvalidTerm(Op op, Term[] args) {
        this(op, DTERNAL, args);
    }

    public InvalidTerm(Op op, int dt, Term[] args) {

        this.op = op;
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
                    ", dt=" + dt +
                    ", args=" + Arrays.toString(args) +
                    '}';
        else
            return super.toString();
    }
}
