package nars.concept;

import nars.NAR;
import nars.Task;
import nars.table.DefaultBeliefTable;
import nars.table.TemporalBeliefTable;
import nars.term.Compound;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

/**
 * latches the last known task
 */
class SensorBeliefTable extends DefaultBeliefTable {

    static final int durationsTolerance = 4;

    private Task current;

    public SensorBeliefTable(TemporalBeliefTable t) {
        super(t);
    }

    public void commit(Task next) {
        this.current = next;
    }

    @Override
    public Truth truth(long when, long now, int dur, NAR nar) {
        Truth x = super.truth(when, now, dur, nar);
        if (x!=null)
            return x;

        Task current = this.current;
        if (current != null &&
                current.start() <= when && when <= current.end() + durationsTolerance * dur) {
            return current.truth();
        } else {
            return null;
        }
    }

    @Override
    public Task match(long when, long now, int dur, @Nullable Task against, Compound template, boolean noOverlap, NAR nar) {
        Task x = super.match(when, now, dur, against, template, noOverlap, nar);
        if (x!=null)
            return x;

        Task current = this.current;
        if (current != null &&
                current.start() <= when && when <= current.end() + durationsTolerance * dur) {
            return current;
        } else {
            return null;
        }

    }
}
