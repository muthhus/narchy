package nars.term.mutate;

import jcog.Util;
import nars.$;
import nars.Op;
import nars.term.Compound;
import nars.term.ProxyTerm;
import nars.term.Term;
import nars.term.compound.GenericCompound;
import nars.term.compound.ProxyCompound;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.term.subst.Unify;

import java.util.List;

/**
 * AIKR choicepoint used in deciding possible mutations to apply in deriving new compounds
 */
public interface Termutator {

    /**
     * match all termutations recursing to the next after each successful one
     */
    boolean mutate(Unify f, List<Termutator> chain, int current);

    default int getEstimatedPermutations() {
        return -1; /* unknown */
    }

    abstract class AbstractTermutator extends ProxyTerm implements Termutator {

        protected static Term wrap(TermContainer y) {
            return y instanceof Term ? (Term)y : $.p(y.toArray());
        }

//        AbstractTermutator(TermContainer... keyComponents) {
//            this(Util.map(AbstractTermutator::wrap, new Term[keyComponents.length], keyComponents));
//        }


        AbstractTermutator(Term... keyComponents) {
            super(keyComponents.length == 1 ? keyComponents[0] : $.pStack(keyComponents));
        }

    }
}
