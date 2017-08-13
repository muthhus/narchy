package nars.derive.time;

import nars.$;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static nars.Op.CONJ;

public class TimeEvent extends Event {

    private final Time time;
    private final int dur;

    TimeEvent(ITemporalize t, Term term, Time time) {
        super(t, term);
        this.time = time;
        this.dur = term.op() == CONJ ? term.dtRange() : 0;
    }

    @Override
    public @Nullable Time start(Map<Term, Time> ignored) {
        return time;
    }

    @Override
    public @Nullable Time end(Map<Term, Time> trail) {
        return time.add(dur);
    }

    @Override
    public Event neg() {
        return new TimeEvent(t, $.neg(term), time);
    }

    @Override
    public String toString() {
        return Event.str(term, start(null).abs(), end(null).abs());
    }
}
