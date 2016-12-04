package nars.concept.dynamic;

import nars.NAR;
import nars.concept.Concept;
import nars.term.Term;
import nars.truth.DynTruth;
import nars.truth.Truth;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static nars.Op.NEG;

/**
 * Created by me on 12/4/16.
 */
abstract public class DynamicTruthModel {
    Term[] inputs;
    boolean beliefOrGoal;

    @Nullable
    public DynTruth eval(long when, boolean stamp, NAR n) {

        Map<Term, Truth> e = new UnifiedMap<>();
        for (int i = 0; i < inputs.length; i++) {
            Term t = inputs[i];

            if (t.op() == NEG)
                throw new UnsupportedOperationException();

            Concept c = n.concept(t);
            if (c != null) {
                Truth u = (beliefOrGoal ? c.beliefs() : c.goals()).truth(when);
                /*if (u!=null)
                    u = u.negated( t.op() == NEG );*/
                e.put(t, u);
            } else {
                return null;
            }
        }

        return eval(e, when, stamp, n);
    }

    protected abstract DynTruth eval(Map<Term, Truth> e, long when, boolean stamp, NAR n);


}
