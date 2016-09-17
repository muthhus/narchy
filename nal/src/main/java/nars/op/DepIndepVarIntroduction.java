package nars.op;

import nars.$;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

import static nars.Op.CONJ;

/**
 * 1-iteration DepVar and IndepVar introduction that emulates and expands the original NAL6 Variable Introduction Rules
 *
 * allows promoting query variables to dep/indep vars
 */
public class DepIndepVarIntroduction extends VarIntroduction {

    private final Random rng;

    public DepIndepVarIntroduction(Random rng) {
        super(1);
        this.rng = rng;
    }


    final static int DepOrIndepBits = Op.VAR_INDEP.bit | Op.VAR_DEP.bit | Op.VAR_PATTERN.bit;

    @Nullable
    @Override
    protected Term[] nextSelection(Compound input) {
        return Terms.substRoulette(input, subterm -> !subterm.isAny(DepOrIndepBits), 2, rng);
    }

    @Override
    protected Term next(Compound input, Term selected, int iteration) {

        List<byte[]> p = input.pathsTo(selected);
        if (p.isEmpty())
            return null;

        boolean[] withinConj = new boolean[p.size()];

        ObjectByteHashMap<Term> statementCoverage = new ObjectByteHashMap<>(p.size() /* estimate */);
        boolean[] withinStatement = new boolean[p.size()];

        for (int occurrence = 0, pSize = p.size(); occurrence < pSize; occurrence++) {
            byte[] path = p.get(occurrence);
            Term t = null; //root
            int pathLength = path.length;
            for (int i = -1; i < pathLength; i++) {
                if (i == -1)
                    t = input;
                else
                    t = ((Compound)t).term(path[i]);
                Op o = t.op();
                if (o.statement) {
                    if (i < pathLength - 1) {
                        withinStatement[occurrence] = true;
                        byte inside = (byte) (1 << path[i + 1]);
                        statementCoverage.updateValue(t, inside, (previous) -> (byte) ((previous) | inside));
                    }
                } else if (o == CONJ) {
                    withinConj[occurrence] = true;
                }
            }
        }

        boolean statementScope = true;
        for (boolean x : withinStatement)
            if (!x) { statementScope = false; break; }
        if (statementScope) {
            //at least one statement must be covered (both bits)
            if (statementCoverage.anySatisfy(b->b==0b11))
                return $.varIndep("i" + iteration);
        }

        boolean conjScope = true;
        for (boolean x : withinConj)
            if (!x) { conjScope = false; break; }
        if (conjScope)
            return $.varDep("d" + iteration);

        return null;
    }
}
