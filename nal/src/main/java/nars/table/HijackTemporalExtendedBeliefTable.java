package nars.table;

import nars.$;
import nars.Task;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * extends ListTemporalBeliefTable with additional read-only history which tasks, once removed from the
 * main (parent) belief table, may be allowed to populate.
 */
abstract public class HijackTemporalExtendedBeliefTable extends HijackTemporalBeliefTable {


    final TreeMap<Double, Task> history;
    private final int historicCapacity;

    public HijackTemporalExtendedBeliefTable(int initialCapacity, int historicCapacity, Random r) {
        super(initialCapacity, r);
        this.historicCapacity = historicCapacity;
        this.history = new TreeMap<>();
    }




    @Override
    public @Nullable Task match(long when, long now, int dur, @Nullable Task against) {
        Task t = super.match(when, now, dur, against);

            Task h = matchHistory(when);
            if (h != null) {
                float conf = h.conf(when, dur);
                if (t == null || conf > t.conf()) {
                    t = ressurect(h);
                }
            }

        return t;
    }

    protected Task ressurect(Task t) {
        if (t.isDeleted())
            t.priority().setPriority(0);

        return t;
    }

    @Override
    public Truth truth(long when, int dur, @Nullable EternalTable eternal) {
        Truth t = super.truth(when, dur, eternal);
        Task h = matchHistory(when);
        if (h != null) {
            float hConf = h.conf(when, dur);
            if (t == null || hConf > t.conf())
                return $.t(h.freq(), hConf);
        }
        return t;
    }

    //TODO use a better method:
    Task matchHistory(long when) {
        Double dwhen = Double.valueOf(when);

        Task c, f;
        synchronized (history) {
            Map.Entry<Double, Task> ceil = history.ceilingEntry(dwhen);
            Map.Entry<Double, Task> floor = history.floorEntry(dwhen);
            c = ceil != null ? ceil.getValue() : null;
            f = floor != null ? floor.getValue() : null;
        }

        if (c == f)
            return c;
        else {
            //TODO find closest if both are non-null

            if (c == null)
                return f;
            else
                return c;
        }
    }

    @Override
    public void clear() {
        super.clear();
        synchronized (history) {
            history.clear();
        }
    }

    @Override
    public void onRemoved(Task x) {
        if (save(x)) {

            /** time + insignificant hash difference HACK */
            double k = (x.mid() - 0.25) + (0.5 * (Math.abs(x.hashCode() / ((double) (Integer.MAX_VALUE)))));

            synchronized (history) {
                int toRemove = history.size() + 1 - historicCapacity;
                for (int i = 0; i < toRemove; i++) {
                    Task t = history.pollFirstEntry().getValue();
                    super.onRemoved(t);
                }


                history.put( k , x);
            }

        } else {
            super.onRemoved(x);
        }
    }

    abstract protected boolean save(Task t);

}
