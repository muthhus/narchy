package jcog.data.graph.hgraph;

import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

/** search log history for detecting cycles, reachability, etc */
public interface TraveLog {
    void clear();

    /** returns false if it was already added */
    boolean visit(Node n);

    //TODO: reachable, etc

    class IntHashTraveLog implements TraveLog {

        final IntHashSet visit = new IntHashSet();

        @Override
        public void clear() {
            visit.clear();
        }

        @Override
        public boolean visit(Node n) {
            return visit.add(n.serial);
        }
    }

    /** TODO a bitvector based travelog, with a failsafe max limit  */

}
