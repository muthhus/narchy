package nars.op;

import com.google.common.primitives.Booleans;
import it.unimi.dsi.fastutil.booleans.BooleanArrays;
import nars.$;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.map.primitive.ObjectByteMap;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;

import java.util.List;

import static nars.Op.CONJ;

/**
 * 1-iteration DepVar and IndepVar introduction that emulates and expands the original NAL6 Variable Introduction Rules
 */
public class DepIndepVarIntroduction extends VarIntroduction {

    public DepIndepVarIntroduction() {
        super(1);
    }

    //allow promoting query variables to dep/indep vars
    final static int DepOrIndepBits = Op.VAR_INDEP.bit | Op.VAR_DEP.bit | Op.VAR_PATTERN.bit;
    private boolean canIntroduce(Term subterm) {
        return !subterm.isAny(DepOrIndepBits);
    }

    @Override
    protected Term next(Compound input, Term selected, int iteration) {
        //1. determine the scope (set of superterms) containing the occurrences of the selected term
        //2. if the scope is entirely within statements, use indepvar
        //3. else if the scope is entirely within conjunctions, use depvar
        //4. else, nothing

        List<byte[]> p = input.pathsTo(selected);
        if (p.isEmpty())
            return null;

        boolean[] withinConj = new boolean[p.size()];

        ObjectByteHashMap<Term> statementCoverage = new ObjectByteHashMap(p.size() /* estimate */);
        boolean[] withinStatement = new boolean[p.size()];

        for (int occurrence = 0, pSize = p.size(); occurrence < pSize; occurrence++) {
            byte[] b = p.get(occurrence);
            Term t = input; //root
            for (int i = 0; i < b.length; i++) {
                t = ((Compound)t).term(b[i]);
                if (t.op().statement) {
                    if (i < b.length - 1) {
                        withinStatement[occurrence] = true;
                        byte inside = (byte) (1 << b[i + 1]);
                        statementCoverage.updateValue(t, inside, (previous) -> (byte) ((previous) | inside));
                    }
                }
                else if (t.op() == CONJ)
                    withinConj[occurrence] = true;

                if (withinStatement[occurrence] && withinConj[occurrence])
                    break; //no further iteration is necessary
            }
        }

        boolean statementScope = true;
        for (boolean x : withinStatement)
            if (!x) { statementScope = false; break; }
        if (statementScope) {
            //all statements must be covered: b11
            if (statementCoverage.allSatisfy(b->b==0b11))
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
