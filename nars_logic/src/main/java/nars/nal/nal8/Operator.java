package nars.nal.nal8;

import nars.$;
import nars.Op;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Term;
import nars.term.atom.AbstractStringAtom;
import nars.term.compound.Compound;
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
public final class Operator<T extends Term> extends AbstractStringAtom { //implements Term {


    //final static byte[] opPrefix = new byte[] { (byte)'^' };

    @NotNull
    private final T term;

    public Operator(@NotNull T the) {
        super(Op.OPERATOR.ch + the.toString());
        term = the;
    }

    @NotNull
    public static Compound opArgs(@NotNull Compound operation) {
        return (Compound) operation.term(0);
    }

    public static Term operatorName(@NotNull Compound operation) {
        Operator tn = operatorTerm(operation);
        if (tn != null) return tn.identifier();
        return null;
    }

    @NotNull
    public static Operator operatorTerm(@NotNull Compound operation) {
        return ((Operator) operation.term(1));
    }

    /**
     * creates a result term in the conventional format.
     * the final term in the product (x) needs to be a variable,
     * which will be replaced with the result term (y)
     */
    @Nullable
    public static Term result(@NotNull Compound operation, Term y) {
        Compound x = (Compound) operation.term(0);
        /*if (!(t instanceof Variable))
            return null;*/

        return $.inh(
                y, //SetExt.make(y),
                makeImageExt(x, operation.term(1), (short) (x.size() - 1) /* position of the variable */)
        );
    }

    /**
     * Try to make an Image from a Product and a relation. Called by the logic rules.
     *
     * @param product  The product
     * @param relation The relation (the operator)
     * @param index    The index of the place-holder (variable)
     * @return A compound generated or a term it reduced to
     */
    @Nullable
    private static Term makeImageExt(@NotNull Compound product, @NotNull Term relation, short index) {
        int pl = product.size();
        if (relation.op(Op.PRODUCT)) {
            Compound p2 = (Compound) relation;
            if ((pl == 2) && (p2.size() == 2)) {
                if ((index == 0) && product.term(1).equals(p2.term(1))) { // (/,_,(*,a,b),b) is reduced to a
                    return p2.term(0);
                }
                if ((index == 1) && product.term(0).equals(p2.term(0))) { // (/,(*,a,b),a,_) is reduced to b
                    return p2.term(1);
                }
            }
        }
        /*Term[] argument =
            Terms.concat(new Term[] { relation }, product.cloneTerms()
        );*/
        Term[] argument = new Term[pl];
        argument[0] = relation;
        System.arraycopy(product.terms(), 0, argument, 1, pl - 1);

        return $.the(Op.IMAGE_EXT, index + 1, argument);
    }

    /**
     * applies certain data to a feedback task relating to its causing operation's task
     */
    public static Task feedback(@NotNull MutableTask feedback, @NotNull Task goal, float priMult, float durMult) {
        return feedback.budget(goal.getBudget()).
                budgetScaled(priMult, durMult).
                parent(goal);
    }

    public static Term[] opArgsArray(@NotNull Compound term) {
        return opArgs(term).terms();
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

    @Override
    public int varIndep() {
        return 0;
    }

    @Override
    public int varDep() {
        return 0;
    }

    @Override
    public int varQuery() {
        return 0;
    }

    @Override
    public int vars() {
        return 0;
    }


    @NotNull
    public Term identifier() {
        return term;
    }

}
