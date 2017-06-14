package nars.term.mutate;

import jcog.Util;
import nars.$;
import nars.term.Compound;
import nars.term.ProxyCompound;
import nars.term.Term;
import nars.term.container.TermContainer;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;

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

    abstract class AbstractTermutator extends ProxyCompound implements Termutator {

        protected static Term wrap(TermContainer y) {
            return $.p(y instanceof Term ? (Term)y : $.p(y.toArray()));
        }

        public AbstractTermutator(TermContainer... keyComponents) {
            this(Util.map(AbstractTermutator::wrap, new Term[keyComponents.length], keyComponents));
        }

        public AbstractTermutator(Term... keyComponents) {
            this($.p(keyComponents));
        }

        public AbstractTermutator(Compound key) {
            super(key);
        }
    }
}
