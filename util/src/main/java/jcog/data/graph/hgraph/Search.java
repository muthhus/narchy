package jcog.data.graph.hgraph;

import jcog.list.FasterList;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;

import java.util.stream.Stream;

/**
 *  a search process instance
 *
 *  general purpose recursive search for DFS/BFS/A* algorithms
 *  backtrack/cyclic prevention guaranteed to visit each vertex at most once.
 *  - an instance may be recycled multiple times
 *  - multiple instances may concurrently access the same graph
 */
abstract public class Search<N, E> {

    final TraveLog log;

    protected Search() {
        this(new TraveLog.IntHashTraveLog());
    }

    protected Search(TraveLog log) {
        this.log = log;
    }

    public void start() {

    }

    public void stop() {
        log.clear();
    }

    /**
     * path should not be modified by callee. it is left exposed for performance
     * path boolean is true = OUT, false = IN
     */
    abstract protected boolean visit(Node<N, E> n, FasterList<BooleanObjectPair<Edge<N, E>>> path);


    protected final boolean visited(Node<N, E> next) {
        return !log.visit(next);
    }

    /**
     * the (set of) edges provided to be traversed in the next exploration iteration.
     * subclasses may filter, reorder, or grow  */
    protected Stream<Edge<N,E>> edges(Node<N, E> n, boolean in, boolean out) {
        return n.edges(in, out);
    }
}
