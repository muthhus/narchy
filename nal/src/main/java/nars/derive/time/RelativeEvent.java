package nars.derive.time;

import nars.$;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

public class RelativeEvent extends Event {
    public final Term rel;
    public final int start;
    public final int end;

    public RelativeEvent(ITemporalize t, Term term, Term relativeTo, int start) {
        this(t, term, relativeTo, start, start + term.dtRange());
    }

    public RelativeEvent(ITemporalize t, Term term, Term relativeTo, int start, int end) {
        super(t, term);
        assert(start!=DTERNAL && start!=XTERNAL && end!=DTERNAL && end != XTERNAL);
        //assert (!term.equals(relativeTo));
        this.rel = relativeTo;
        this.start = start;
        this.end = end;
    }


//        @Override
//        public void apply(Map<Term, Time> trail) {
//            Time t = resolve(this.start, trail);
//            if (t != null)
//                trail.putIfAbsent(term, t); //direct set
//        }

    @Override
    public Event neg() {
        return new RelativeEvent(t, $.neg(term), rel, start, end);
    }

    @Override
    @Nullable
    public Time start(Map<Term, Time> trail) {
        return resolve(this.start, trail);
    }

    @Override
    public Time end(Map<Term, Time> trail) {
        return resolve(this.end, trail);
    }

    @Nullable
    private Time resolve(int offset, Map<Term, Time> trail) {

        assert(offset!=DTERNAL);

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
        if (start != end) {
            return term + "@[" + ITemporalize.timeStr(start) + ".." + ITemporalize.timeStr(end) + "]->" + rel;
        } else {
            return term + "@" + ITemporalize.timeStr(start) + "->" + rel;
        }
    }

}
