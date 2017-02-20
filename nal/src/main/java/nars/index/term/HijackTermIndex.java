package nars.index.term;

import jcog.Util;
import jcog.bag.PLink;
import jcog.bag.RawPLink;
import jcog.bag.impl.HijackBag;
import jcog.data.random.XorShift128PlusRandom;
import nars.NAR;
import nars.bag.impl.PLinkHijackBag;
import nars.budget.BudgetMerge;
import nars.conceptualize.ConceptBuilder;
import nars.index.term.map.MaplikeTermIndex;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;

/**
 * Created by me on 2/20/17.
 */
public class HijackTermIndex extends MaplikeTermIndex implements Runnable {

    private final PLinkHijackBag<Termed> table;
    private final Map<Term,Termed> permanent = new ConcurrentHashMap<>();
    private Thread updateThread;
    private boolean running;

    private long updatePeriodMS = 250;

    /** current update index */
    private int visit;

    /** how many items to visit during update */
    private int updateBatchSize;

    public HijackTermIndex(ConceptBuilder cb, int capacity, int reprobes) {
        super(cb);

        updateBatchSize = 1 + (capacity / reprobes);

        this.table = new PLinkHijackBag<Termed>(capacity, reprobes, new XorShift128PlusRandom(1));
    }

    @Override
    public void start(NAR nar) {
        super.start(nar);
        running = true;
        updateThread = new Thread(this);
        updateThread.start();
    }

    @Override
    public @Nullable Termed get(@NotNull Term key, boolean createIfMissing) {
        @Nullable PLink<Termed> x = table.get(key);
        if (x != null) {
            return x.get(); //cache hit
        } else {

            Termed v = permanent.get(key);
            if (v!=null)
                return v;

            if (createIfMissing) {
                Termed kc = conceptBuilder.apply(key);
                if (kc!=null) {
                    PLink<Termed> inserted = insert(kc);
                    if (inserted != null) {
                        Termed ig = inserted.get();
                        if (ig.term().equals(kc))
                            return ig;
                    } else {
                        return null;
                    }
                }
            }
        }

        return null;
    }

    PLink insert(@NotNull Termed value) {
        return table.put(new RawPLink<>(value, activation(value)));
    }

    private float activation(@NotNull Termed value) {
        return 0.5f; //TODO adjust based on complexity etc
    }

    @Override
    public void set(@NotNull Term src, Termed target) {
        table.remove(target); //in-case it already exists, remove it
        permanent.put(src, target);
        insert(target);
    }

    @Override
    public void clear() {
        table.clear();
    }

    @Override
    public void forEach(@NotNull Consumer<? super Termed> c) {
        //TODO make sure this doesnt visit a term twice appearing in both tables but its ok for now
        table.forEachKey(c);
        permanent.values().forEach(c);
    }

    @Override
    public int size() {
        return table.size(); /** approx since permanent not considered */
    }

    @Override
    public @NotNull String summary() {
        return table.size() + " concepts (" + permanent.size() + " permanent)";
    }

    @Override
    public void remove(@NotNull Term entry) {
        table.remove(entry);
        permanent.remove(entry);
    }

    @Override
    public void run() {
        while (running) {

            AtomicReferenceArray<PLink<Termed>> tt = table.map.get();

            int c = tt.length();

            for (int i = 0; i < updateBatchSize; i++ , visit++) {

                if (visit >= c) visit = 0;

                PLink<Termed> x = tt.get(visit);
                if (x!=null)
                    update(x);
            }

            Util.pause(updatePeriodMS);
        }
    }

    protected void update(PLink<Termed> x) {
        x.priMult(0.99f); //TODO better update function based on Concept features
    }

}
