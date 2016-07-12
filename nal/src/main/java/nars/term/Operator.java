package nars.term;

import nars.Op;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * the 1-arity '^' compound which wraps a term to
 * indicate an operator that can be used as the predicate
 * of an Operation, ex:
 *
 *      <(arg0, arg1) --> ^operator>
 *
 * This class also includes static utility methods for
 * working with Operation terms (which include an "Operator",
 * as shown above, but is not an "Operator").
 *
 */
public class Operator<T extends Term> extends Atom {


    public Operator(@NotNull T the) {
        this(the.toString());
    }

    public Operator(@NotNull String id) {
        super((id.charAt(0)!=Op.OPER.ch ? Op.OPER.ch + id : id));
    }


    /** returns the Product arguments compound of an operation. does not check if the input is actually an operation */
    @Nullable
    public static Compound opArgs(@NotNull Compound operation) {
        return (Compound) operation.term(0);
    }
    @Nullable
    public static Compound opArgs(@NotNull Term x) {
        if (x instanceof Compound)
            return opArgs((Compound)x);
        return null;
    }


    /** returns the terms array of the arguments of an operation. does not check if the input is actually an operation */
    @NotNull public static Term[] argArray(@NotNull Compound term) {
        return opArgs(term).terms();
    }

    /** returns the Operator predicate of an operation. */
    @Nullable public static Atomic operator(@NotNull Compound operation) {
        Term o = operation.term(1);
        return (o.op() == Op.OPER) ? ((Atomic) o) : null;
    }
    @Nullable public static Atomic operator(@NotNull Term maybeOperation) {
        if (maybeOperation instanceof Compound) {
            Compound c = (Compound)maybeOperation;
            if (c.size() == 2) {
                if (c.isTerm(1,Op.OPER) && c.isTerm(0, Op.PROD)) {
                    return (Atomic) c.term(1);
                }
            }
        }
        return null;
    }

    @NotNull
    @Override
    public Op op() {
        return Op.OPER;
    }

}
