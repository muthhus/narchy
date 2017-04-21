package nars.term.mutate;

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

    abstract class AbstractTermutator implements Termutator {

        /**
         * cached key
         */
        private Object key = null;

        @Override
        public String toString() {
            return key().toString();
        }

        @Override
        public final boolean equals(@NotNull Object obj) {
            return (this == obj) ||
                    obj instanceof AbstractTermutator
                        &&
                    key().equals(((AbstractTermutator) obj).key());
        }

        @Override
        public final int hashCode() {
            return key().hashCode();
        }

        public Object key() {
            Object k = this.key;
            if (k == null) {
                k = newKey();
            }
            return k;
        }

        /**
         * lazily generate a key for identity comparison
         */
        abstract protected Object newKey();
    }
}
