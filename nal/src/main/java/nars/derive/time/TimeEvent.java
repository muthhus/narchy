package nars.derive.time;

import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class TimeEvent extends Event {

    private final Time time;
    private final int dur;

    TimeEvent(Term term, Time time) {
        this(term, time, 0);
    }

    TimeEvent(Term term, Time time, int dur) {
        super(term);
        this.time = time;
        this.dur = dur;
                //term.op() == CONJ ? term.dtRange() : 0;
    }

    @Override
    public @Nullable Time start(Map<Term, Time> ignored) {
        return time;
    }

    @Override
    public @Nullable Time end(Map<Term, Time> ignored) {
        return time.add(dur);
    }

    @Override
    public Event neg() {
        return new TimeEvent(term.neg(), time);
    }

    @Override
    public String toString() {
       if (dur == 0) {
           if (time.offset==0)
               return term +"@" + ITemporalize.timeStr(time.base);
           else
               return term + "@" + time;
       } else {
           return Event.str(term, start(null).abs(), end(null).abs());
       }
    }
}
