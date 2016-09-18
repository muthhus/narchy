package nars.op;

import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

import static nars.$.varDep;
import static nars.$.varIndep;
import static nars.Op.CONJ;

/**
 * 1-iteration DepVar and IndepVar introduction that emulates and expands the original NAL6 Variable Introduction Rules
 * <p>
 * allows promoting query variables to dep/indep vars
 */
public class DepIndepVarIntroduction extends VarIntroduction {


    public DepIndepVarIntroduction() {
        super(1);
    }

    @Override
    public void accept(Task task, NAR nar) {

        if (!task.term().hasAny(ConjOrStatementBits))
            return; //early failure test

        if (task.cyclic())
            return; //avoids reprocessing the same

        super.accept(task, nar);
    }

    @Override
    protected Task clone(@NotNull Task original, Compound c) {
        Task t = super.clone(original, c);
        if (t!=null) {
            t.budget().setPriority(t.pri()*t.pri()); //shrink
        }
        return t;
    }

    final static int ConjOrStatementBits = Op.StatementBits | Op.CONJ.bit;
    final static int DepOrIndepBits = Op.VAR_INDEP.bit | Op.VAR_DEP.bit | Op.VAR_PATTERN.bit;

    @Nullable
    @Override
    protected Term[] nextSelection(Compound input) {
        Predicate<Term> condition = subterm -> !subterm.isAny(DepOrIndepBits);
        return Terms.substAllRepeats(input, condition, 2);
        //return Terms.substRoulette(input, condition, 2, rng);
    }

    @Override
    protected Term[] next(Compound input, Term selected) {

        List<byte[]> p = input.pathsTo(selected);
        if (p.isEmpty())
            return null;

        //detect an invalid top-level indep var substitution
        if (input.op().statement) {
            for (byte[] path : p)
                if (path.length < 2)
                    return null; //substitution would replace something at the top level of a statement
        }


        boolean[] withinConj = new boolean[p.size()];
        ObjectByteHashMap<Term> conjCoverage = new ObjectByteHashMap<>(p.size());

        ObjectByteHashMap<Term> statementCoverage = new ObjectByteHashMap<>(p.size() /* estimate */);
        boolean[] withinStatement = new boolean[p.size()];

        for (int occurrence = 0, pSize = p.size(); occurrence < pSize; occurrence++) {
            byte[] path = p.get(occurrence);
            Term t = null; //root
            int pathLength = path.length;
            for (int i = -1; i < pathLength-1 /* dont include the selected term itself */; i++) {
                if (i == -1)
                    t = input;
                else
                    t = ((Compound) t).term(path[i]);
                Op o = t.op();
                if (o.statement) {
                    withinStatement[occurrence] = true;
                    byte inside = (byte) (1 << path[i + 1]);
                    statementCoverage.updateValue(t, inside, (previous) -> (byte) ((previous) | inside));
                } else if (o == CONJ) {
                    withinConj[occurrence] = true;
                    conjCoverage.addToValue(t, (byte) 1);
                }
            }
        }

        int iteration = 0;

        //at least one statement must have both sides covered
        Term I = (statementCoverage.anySatisfy(b -> b == 0b11)) ?
                varIndep("i" + iteration) : null;

        //at least one conjunction must contain >=2 path instances
        Term D = conjCoverage.anySatisfy(b -> b >= 2) ?
                varDep("i" + iteration) : null;

        if (I!=null && D!=null) {
            return new Term[] { I , D };
        } else if (I!=null) {
            return new Term[] { I };
        } else if (D!=null) {
            return new Term[] { D };
        } else {
            return null;
        }

    }
}
