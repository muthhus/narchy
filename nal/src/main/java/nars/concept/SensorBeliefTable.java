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

    static final int durationsTolerance = 3;

    private Task current;

    public SensorBeliefTable(TemporalBeliefTable t) {
        super(t);
    }

    public void commit(Task next) {
        this.current = next;
    }

    @Override
    public Truth truth(long when, NAR nar) {
        Truth tabled = super.truth(when, nar);

        Task current = this.current;
        if (latches(when, nar, current)) {
            return Truth.maxConf(tabled, current.truth());
        } else {
            return tabled;
        }
    }

    @Override
    public Task match(long when, @Nullable Task against, Compound template, boolean noOverlap, NAR nar) {
        Task tabled = super.match(when, against, template, noOverlap, nar);

        Task current = this.current;
        if (latches(when, nar, current)) {
            if (tabled!=null && tabled.conf() >= current.conf())
                    return tabled;
            return current;
        } else {
            return tabled;
        }

    }

    static boolean latches(long when, NAR nar, Task current) {
        return current != null &&
                current.start() <= when
                && when <= current.end() + durationsTolerance * nar.dur();
    }
}
