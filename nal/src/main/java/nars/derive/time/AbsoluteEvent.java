package nars.derive.time;

import nars.$;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static nars.time.Tense.ETERNAL;

public class AbsoluteEvent extends Event {

    public final long start, end;

    public AbsoluteEvent(ITemporalize t, Term term, long start, long end) {
        super(t, term);

        if (start == ETERNAL) {
            this.start = this.end = ETERNAL;
        } else {

            //int tdt = term.dtRange();

            if (end == ETERNAL) {
                end = start;
            }

            long te;
            if (start <= end) {
                this.start = start;
                te = end;
            } else {
                this.start = end;
                te = start;
            }

            this.end = te;
        }
    }

//        @Override
//        public void apply(Map<Term, Time> trail) {
//            trail.put(term, Time.the(start, 0)); //direct set
//        }

    @Override
    public Event neg() {
        return new AbsoluteEvent(t, $.neg(term), start, end);
    }

    @Override
    public Time start(@Nullable Map<Term, Time> ignored) {
//            if(ignored!=null) {
//                Time existingTime = ignored.get(term);
//                if (existingTime != null) {
//                    System.out.println("conflict?: " + existingTime + " " + Time.the(start, 0));
//                }
//            }
        return Time.the(start, 0);
    }

    @Override
    public Time end(Map<Term, Time> ignored) {
//            int dt = term.dt();
//            if (dt == DTERNAL)
//                dt = 0;
        return Time.the(end, 0);
    }

    @Override
    public String toString() {
        if (start != ETERNAL) {
            if (start != end)
                return term + ("@[" + ITemporalize.timeStr(start) + ".." + ITemporalize.timeStr(end)) + ']';
            else
                return term + "@" + ITemporalize.timeStr(start);
        } else
            return term + "@ETE";
    }

}
