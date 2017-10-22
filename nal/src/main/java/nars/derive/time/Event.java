package nars.derive.time;

import nars.term.Term;
import nars.term.atom.Bool;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static nars.time.Tense.ETERNAL;

public abstract class Event implements Comparable<Event> {

    public final Term term;

    Event(Term term) {
        assert (!(term instanceof Bool));
        this.term = term;
    }

    @Nullable
    abstract public Time start(Map<Term, Time> trail);

    @Nullable
    abstract public Time end(Map<Term, Time> trail);

    @Override
    public boolean equals(Object o) {
        return o instanceof Event && compareTo((Event) o) == 0;
//        if (this == o) return true;
//        if (!(o instanceof Event)) return false;
//
//        Event event = (Event) o;
//
//        return term.equals(event.term);
    }

    @Override
    public int hashCode() {
        return term.hashCode();
    }

    /**
     * return a new instance with the term negated
     */
    abstract public Event neg();


    @Override
    public int compareTo(Event that) {
        if (this == that) return 0;

        if (getClass() == that.getClass()) {
            //same class

            if (this instanceof RelativeEvent) {
                RelativeEvent THIS = (RelativeEvent) this;
                RelativeEvent THAT = (RelativeEvent) that;

                if (THIS.inverts() && THAT.inverts()) {
                    //prefer the one with the non-zero delta. the zero delta is likely the least useful case
                    //the non-zero delta is indicating a temporal relationship between an event and its inverse
                    if (THIS.zero() && !THAT.zero())
                        return +1;
                    else if (THAT.zero() && !THIS.zero())
                        return -1;
                }


                //always prefer non-inverting, ie. negation as a last resort
                if (THIS.inverts() && !THAT.inverts())
                    return +1;
                if (THAT.inverts() && !THIS.inverts())
                    return -1;


                return compareTermStartEnd(
                        THIS.rel.term(), THIS.start, THIS.end,
                        THAT.rel.term(), THAT.start, THAT.end);


            } else if (this instanceof AbsoluteEvent) {
                AbsoluteEvent THIS = (AbsoluteEvent) this;
                AbsoluteEvent THAT = (AbsoluteEvent) that;
                long sThis = THIS.start;
                long sThat = THAT.start;

                //eternal should be ranked lower
                if (sThis == ETERNAL && sThat != ETERNAL) return +1;
                if (sThat == ETERNAL && sThis != ETERNAL) return -1;

                return compareTermStartEnd(
                        THIS.term, THIS.start, THIS.end,
                        THAT.term, THAT.start, THAT.end);

            }

        } else {
            //different types: absolute vs. relative or relative vs. absolute

            //absolute eternal is dead last, even compared to relative
            if (this instanceof AbsoluteEvent) {
                if (((AbsoluteEvent) this).start == ETERNAL)
                    return +1;
            }
            if (that instanceof AbsoluteEvent) {
                if (((AbsoluteEvent) that).start == ETERNAL)
                    return -1;
            }

            if (this instanceof AbsoluteEvent)
                return -1;

        }

        return +1;
    }

    static int compareTermStartEnd(Term x, long xstart, long xend, Term y, long ystart, long yend) {

        if (x.equals(y)) {
            int c1 = Long.compare(xstart, ystart);
            if (c1 != 0)
                return c1;
            return Long.compare(xend, yend);
        } else {

//                    float xs = t.score(x);
//                    float ys = t.score(y);
//                    if (xs != ys) {
//                        return Float.compare(ys, xs);
//                    } else {
            //prefer larger complexity (variables arent helpful)
            int xv = x.complexity();
            int yv = y.complexity();
            if (xv == yv)
                return x.compareTo(y);
            else
                return Integer.compare(yv, xv);
            //         }
        }
    }

    public static String str(Term term, long start, long end) {
        if (start != ETERNAL) {
            if (start != end)
                return term + ("@[" + ITemporalize.timeStr(start) + ".." + ITemporalize.timeStr(end)) + ']';
            else
                return term + "@" + ITemporalize.timeStr(start);
        } else
            return term + "@ETE";
    }
}
