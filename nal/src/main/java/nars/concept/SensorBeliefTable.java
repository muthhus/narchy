package nars.concept;

import nars.NAR;
import nars.Task;
import nars.table.DefaultBeliefTable;
import nars.table.TemporalBeliefTable;
import nars.term.Compound;
import nars.truth.Truth;
import nars.util.signal.ScalarSignal;
import nars.util.signal.Signal;
import org.jetbrains.annotations.Nullable;

/**
 * latches the last known task
 */
@Deprecated class SensorBeliefTable extends DefaultBeliefTable {

    static final int durationsTolerance = 1;


    public Signal sensor;

    public SensorBeliefTable(TemporalBeliefTable t) {
        super(t);
    }


    @Override
    public Truth truth(long when, NAR nar) {
        Truth tabled = super.truth(when, nar);

        Task current = this.sensor.get();
        if (current!=null && latches(when, nar, current)) {
            return Truth.maxConf(tabled, current.truth(when, nar.dur()));
        } else {
            return tabled;
        }
    }

    @Override
    public Task match(long when, @Nullable Task against, Compound template, boolean noOverlap, NAR nar) {
        Task tabled = super.match(when, against, template, noOverlap, nar);

        Task current = this.sensor.get();
        if (current!=null && latches(when, nar, current)) {
            int dur = nar.dur();
            if (tabled!=null && tabled.evi(when, dur) >= current.evi(when, dur))
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
