package astar.model;

import astar.Find;
import astar.Problem;
import jcog.TODO;
import jcog.data.graph.hgraph.Edge;
import jcog.data.graph.hgraph.HashGraph;
import jcog.data.graph.hgraph.Node;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.api.tuple.primitive.ObjectLongPair;

import java.io.PrintStream;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

public class TimeProblem<T,E> extends HashGraph<TimeProblem.Event<T>,E> implements Problem<TimeProblem.Event<T>> {

    public static final long ETERNAL = Long.MIN_VALUE;
    public static final long TIMELESS = Long.MAX_VALUE;

    @Override
    public double cost(Event<T> a, Event<T> b) {
        long at, bt;
        if ((at = a.start()) == Long.MIN_VALUE || (bt = b.start()) == ETERNAL)
            return Double.POSITIVE_INFINITY;
        else
            return Math.abs(at - bt);
    }

    /** TODO stream */
    @Override public Iterable<Event<T>> next(Event<T> current) {
        return ()->node(current).successors().iterator();
    }

    public boolean link(Event<T> before, E e, Event<T> after) {
        return edgeAdd(add(before), e, add(after));
    }

    /** absolutely specified event */
    public static class Event<T> extends Find<ObjectLongPair<T>> {

        public Event(T id, long when) {
            super(pair(id,when));
        }

        public T id() {
            return id.getOne();
        }

        public float pri() {
            return 1f;
        }

        public long start() {
            return id.getTwo();
        }
        public long end() {
            return start();
        }

        @Override
        public String toString() {
            long s = start();
            if (s == ETERNAL)
                return id() + "@ETE";
            else {
                long e = end();
                return id() + "@" + (s==e ? s : "[" + s + ".." + e + "]");
            }
        }

        public boolean absolute() {
            return true;
        }
    }

    /** floating, but potentially related to one or more absolute event */
    public static class Relative<T> extends Event<T> {

        public Relative(T id) {
            super(id, TIMELESS);
        }

        public boolean absolute() {
            return false;
        }

        @Override
        public String toString() {
            return id().toString() + "@?";
        }
    }

    protected static class Unsolved<X> extends Relative<X> {
        public final Map<X, LongSet> absolute;

        public Unsolved(X x, Map<X, LongSet> absolute) {
            super(x);
            this.absolute = absolute;
        }

//        @Override
//        public boolean equals(Object other) {
//            throw new RuntimeException();
//        }
//
//        @Override
//        public int hashCode() {
//            throw new RuntimeException();
//        }

        @Override
        public long start() {
            return TIMELESS;
        }

        /* hack */
        static final String ETERNAL_STRING = Long.toString(ETERNAL);

        @Override
        public String toString() {
            return id() + "?" +
                    (absolute.toString().replace(ETERNAL_STRING, "ETE")); //HACK
        }

    }


}
