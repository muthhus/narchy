package nars.nal.nal8;

import nars.Op;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

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

    public Operator(@NotNull T the) {
        this(the.toString());
    }

    public Operator(@NotNull String the) {
        this.str = (the.charAt(0)!=Op.OPERATOR.ch ? Op.OPERATOR.ch + the : the); //prepends ^ if necessary
    }

    /** returns the Product arguments compound of an operation. does not check if the input is actually an operation */
    @NotNull public static Compound opArgs(@NotNull Compound operation) {
        return (Compound) operation.term(0);
    }

//    @NotNull public static Compound opArgs(@NotNull Termed<Compound> t) {
//        return opArgs((Compound)t.term());
//    }

    /** returns the terms array of the arguments of an operation. does not check if the input is actually an operation */
    @NotNull public static Term[] argArray(@NotNull Compound term) {
        return opArgs(term).terms();
    }

    /** returns the Operator predicate of an operation. does not check if the input is actually an operation */
    @NotNull public static Operator operator(@NotNull Compound operation) {
        return ((Operator) operation.term(1));
    }

    @NotNull
    @Override
    public Op op() {
        return Op.OPERATOR;
    }

    @Override
    public int complexity() {
        return 1;
    }


    @NotNull
    @Override
    public String toString() {
        return str;
    }


}
