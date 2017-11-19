package astar.model;

import astar.Problem;
import jcog.Util;
import jcog.data.graph.hgraph.HashGraph;
import org.eclipse.collections.api.set.primitive.LongSet;

import java.util.Map;

public class TimeProblem<T,E> extends HashGraph<TimeProblem.Event<T>,E> /*implements Problem<TimeProblem.Event<T>>*/ {

    public static final long ETERNAL = Long.MIN_VALUE;
    public static final long TIMELESS = Long.MAX_VALUE;

//    @Override
//    public double cost(Event<T> a, Event<T> b) {
//        long at, bt;
//        if ((at = a.start()) == Long.MIN_VALUE || (bt = b.start()) == ETERNAL)
//            return Double.POSITIVE_INFINITY;
//        else
//            return Math.abs(at - bt);
//    }
//
//    /** TODO stream */
//    @Override public Iterable<Event<T>> next(Event<T> current) {
//        return ()->node(current).successors().iterator();
//    }

    public boolean link(Event<T> before, E e, Event<T> after) {
        return edgeAdd(add(before), e, add(after));
    }

    /** absolutely specified event */
    public abstract static class Event<T> /*extends Find<ObjectLongPair<T>>*/ {

        public final T id;
        private final int hash;

        protected Event(T id, long start, long end) {
            this.id = id;
            this.hash =  Util.hashCombine(id.hashCode(), Long.hashCode(start), Long.hashCode(end));
        }

        abstract public long start();
        abstract public long end();

        @Override public final int hashCode() {
            return hash;
        }

        @Override
        public final boolean equals(Object obj) {
            if (this == obj) return true;
            Event e = (Event)obj;
            return start()==e.start() && id.equals(e.id) && end() == e.end();
        }

        //        public float pri() {
//            return 1f;
//        }


        @Override
        public final String toString() {
            long s = start();
            if (s == ETERNAL) {
                return id + "@ETE";
            } else if (s == TIMELESS) {
                return id + "@?";
            } else {
                long e = end();
                return id + "@" + (s == e ? s : "[" + s + ".." + e + "]");
            }
        }

        public final boolean absolute() {
            return start()!=TIMELESS;
        }
    }

    public static class Absolute<T> extends Event<T> {
        private final long start;

        public Absolute(T t, long startAndEnd) {
            super(t, startAndEnd, startAndEnd);
            this.start = startAndEnd;
        }

        @Override
        public final long start() {
            return start;
        }

        @Override
        public long end() {
            return start;
        }
    }


    public static class AbsoluteInterval<T> extends Event<T> {
        private final long start, end;

        public AbsoluteInterval(T t, long start, long end) {
            super(t, start, end);
            assert(start!=end);
            this.start = start;
            this.end = end;
        }

        @Override
        public final long start() {
            return start;
        }

        @Override
        public final long end() {
            return end;
        }
    }

    /** floating, but potentially related to one or more absolute event */
    public static class Relative<T> extends Event<T> {

        public Relative(T id) {
            super(id, TIMELESS, TIMELESS);
        }

        @Override
        public final long start() {
            return TIMELESS;
        }

        @Override
        public final long end() {
            return TIMELESS;
        }
    }

    /** holds an attached solution-specific table of event times;
     * instances of Unsolved shouldnt be inserted */
    protected static class Unsolved<X> extends Relative<X> {
        public final Map<X, LongSet> absolute;

        public Unsolved(X x, Map<X, LongSet> absolute) {
            super(x);
            this.absolute = absolute;
        }


//        /* hack */
//        static final String ETERNAL_STRING = Long.toString(ETERNAL);
//
//        @Override
//        public String toString() {
//            return id + "?" +
//                    (absolute.toString().replace(ETERNAL_STRING, "ETE")); //HACK
//        }

    }


}
