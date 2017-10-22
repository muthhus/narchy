package nars.derive.time;

import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.SortedSet;

import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.XTERNAL;

public class RelativeEvent extends Event {
    public final Term rel;
    public final int start;
    public final int end;
    protected final ITemporalize t;
    final boolean inverts;
    private final boolean self;


    protected RelativeEvent(ITemporalize t, Term term, Term relativeTo, int start, int end) {
        super(term);
        assert (start != XTERNAL && end != XTERNAL);

        this.t = t;

        //assert (!term.equals(relativeTo));
        this.rel = relativeTo;
        this.start = start;
        this.end = end;
        this.self = term.equals(relativeTo.term());
        this.inverts = term.unneg().equals(relativeTo.unneg());
    }


//        @Override
//        public void apply(Map<Term, Time> trail) {
//            Time t = resolve(this.start, trail);
//            if (t != null)
//                trail.putIfAbsent(term, t); //direct set
//        }

    @Override
    public Event neg() {
        return new RelativeEvent(t, term.neg(), rel, start, end);
    }

    @Override
    @Nullable
    public Time start(Map<Term, Time> trail) {


        if (self && start!=DTERNAL) {
            //terminate here but apply the relative offset indicated to any matching absolute term constraint
            SortedSet<Event> m = ((Temporalize) t).constraints.get(term);
            if (m!=null) {
                for (Event e : m) {
                    if (e instanceof AbsoluteEvent) {
                        long ae = ((AbsoluteEvent) e).start;
                        if (ae == ETERNAL)
                            return null; //cant do anything
                        return Time.the(this.start + ae, 0);
                    }
                }
            }
            return null;
        }

        return resolve(this.start, trail);
    }


    @Override
    public Time end(Map<Term, Time> trail) {
        return resolve(this.end, trail);
    }

    @Nullable
    private Time resolve(int offset, Map<Term, Time> trail) {


        Event e = t.solve(rel, trail);
        if (e != null) {
            @Nullable Time rt = e.start(trail);
            if (rt != null)
                return rt.add(offset);
        }

        return null;
    }

    @Override
    public String toString() {
        if (start == DTERNAL) {
            return term + "@:->" + rel;
        } else if (start != end) {
            return term + "@[" + ITemporalize.timeStr(start) + ".." + ITemporalize.timeStr(end) + "]->" + rel;
        } else {
            return term + "@" + ITemporalize.timeStr(start) + "->" + rel;
        }
    }

    public boolean self() {
        return self;
    }

    public boolean inverts() {
        return inverts;
    }

    public boolean zero() {
        return start == 0;
    }
}
