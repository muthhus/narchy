package nars.derive.time;

import nars.term.Term;
import nars.term.atom.Bool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static nars.time.Tense.ETERNAL;

public abstract class Event implements Comparable<Event> {

    public final Term term;

    Event(Term term) {
        assert(!(term instanceof Bool));
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
    public int compareTo(@NotNull Event that) {
        if (this == that) return 0;

        if (getClass() == that.getClass()) {
            //same class

            if (this instanceof RelativeEvent) {
                RelativeEvent THIS = (RelativeEvent) this;
                Term x = THIS.rel.term();
                RelativeEvent THAT = (RelativeEvent) that;
                Term y = THAT.rel.term();
                if (x.equals(y)) {
                    int c1 = Integer.compare(THIS.start, THAT.start);
                    if (c1 != 0)
                        return c1;
                    return Integer.compare(THIS.end, THAT.end);
                } else {

//                    float xs = t.score(x);
//                    float ys = t.score(y);
//                    if (xs != ys) {
//                        return Float.compare(ys, xs);
//                    } else {
                        //prefer lower volume
                        int xv = x.volume();
                        int yv = y.volume();
                        if (xv == yv)
                            return x.compareTo(y);
                        else
                            return Integer.compare(xv, yv);
           //         }
                }

            } else if (this instanceof AbsoluteEvent) {
                AbsoluteEvent THIS = (AbsoluteEvent) this;
                AbsoluteEvent THAT = (AbsoluteEvent) that;
                long sThis = THIS.start;
                long sThat = THAT.start;

                //eternal should be ranked lower
                if (sThis == ETERNAL) return +1;
                if (sThat == ETERNAL) return -1;

                int cs = Long.compare(sThis, sThat);

                if (cs != 0)
                    return cs;
                return Long.compare(THIS.end, THAT.end);

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
