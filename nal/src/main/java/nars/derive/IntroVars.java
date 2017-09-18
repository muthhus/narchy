package nars.derive;

import nars.control.Derivation;
import nars.op.DepIndepVarIntroduction;
import nars.term.Term;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class IntroVars extends AbstractPred<Derivation> {

    protected IntroVars() {
        super("IntroVars");
    }

    @Override
    public boolean test(Derivation p) {
        Term x = p.derivedTerm.get();
        if (x == null)
            return false; //spent all TTL or something

        //var intro before temporalizing.  otherwise any calculated temporal data may not applied to the changed term (ex: occ shift)
        @Nullable Pair<Term, Map<Term, Term>> vc = DepIndepVarIntroduction.varIntroX(x, p.random);
        if (vc == null) return false;

        Term v = vc.getOne();
        if (!(v.op().conceptualizable) || (v.equals(x) /* keep only if it differs */))
            return false;

//            if (d.temporal) {
//                Map<Term, Term> m = vc.getTwo();
//                m.forEach(d.xy::tryPut); //store the mapping so that temporalization can resolve with it
//            }

        p.derivedTerm.set(v);
        return true;
    }
}
