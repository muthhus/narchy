package nars.term;

import nars.Op;
import nars.term.atom.Atomic;
import nars.term.atom.AtomicStringConstant;
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
public final class Operator<T extends Term> extends AtomicStringConstant {

    @NotNull
    private final String str;

    private final int hash;

    public Operator(@NotNull T the) {
        this(the.toString());
    }

    public Operator(@NotNull String the) {
        this.str = (the.charAt(0)!=Op.OPERATOR.ch ? Op.OPERATOR.ch + the : the); //prepends ^ if necessary
        this.hash = str.hashCode();
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    /** returns the Product arguments compound of an operation. does not check if the input is actually an operation */
    @Nullable
    public static Compound opArgs(@NotNull Compound operation) {
        return (Compound) operation.term(0);
    }

//    @NotNull public static Compound opArgs(@NotNull Termed<Compound> t) {
//        return opArgs((Compound)t.term());
//    }

    /** returns the terms array of the arguments of an operation. does not check if the input is actually an operation */
    @NotNull public static Term[] argArray(@NotNull Compound term) {
        return opArgs(term).terms();
    }

    /** returns the Operator predicate of an operation. */
    @NotNull public static Atomic operator(@NotNull Compound operation) {
        Term o = operation.term(1);
        return (o!=null && o.op() == Op.OPERATOR) ? ((Atomic) o) : null;
    }

    @NotNull
    @Override
    public Op op() {
        return Op.OPERATOR;
    }


    @NotNull
    @Override
    public String toString() {
        return str;
    }


}
