package nars.table;

import nars.$;
import nars.Task;
import nars.truth.Truth;
import nars.util.signal.SignalTask;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * extends ListTemporalBeliefTable with additional read-only history which tasks, once removed from the
 * main (parent) belief table, may be allowed to populate.
 */
abstract public class HijackTemporalExtendedBeliefTable extends HijackTemporalBeliefTable {


    final TreeMap<Long, Task> history;
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
                return ressurect(h);
            }
        }
        return t;
    }

    protected Task ressurect(Task t) {
        if (t.isDeleted())
            t.budget().setPriority(0);

        return t;
    }

    @Override
    public @Nullable Truth truth(long when, long now, int dur, @Nullable EternalTable eternal) {
        Truth t = super.truth(when, now, dur, eternal);
        Task h = matchHistory(when);
        if (h != null) {
            float conf = h.conf(when, dur);
            if (t == null || conf > t.conf())
                return $.t(h.freq(), conf);
        }
        return t;
    }

    public Task matchHistory(long when) {
        synchronized (history) {
            if (history.isEmpty())
                return null;

            Map.Entry<Long, Task> ceil = history.ceilingEntry(when);
            Task c = ceil != null ? ceil.getValue() : null;
            Map.Entry<Long, Task> floor = history.floorEntry(when);
            Task f = floor != null ? floor.getValue() : null;
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
        if (!save(x))
            return;

        synchronized (history) {
            int toRemove = history.size() + 1 - historicCapacity;
            if (toRemove > 0) {
                for (int i = 0; i < toRemove; i++)
                    history.pollFirstEntry();
            }

            history.put(x.mid(), x);
        }
    }

    abstract protected boolean save(Task t);

}
