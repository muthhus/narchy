package nars.index.term;

import jcog.Util;
import jcog.bag.impl.hijack.PLinkHijackBag;
import jcog.pri.PLink;
import jcog.pri.RawPLink;
import jcog.random.XorShift128PlusRandom;
import nars.NAR;
import nars.Param;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.conceptualize.ConceptBuilder;
import nars.index.term.map.MaplikeTermIndex;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;

/**
 * Created by me on 2/20/17.
 */
public class HijackTermIndex extends MaplikeTermIndex implements Runnable {

    private final PLinkHijackBag<Termed> table;
    //private final Map<Term,Termed> permanent = new ConcurrentHashMap<>(1024);
    private Thread updateThread;
    private boolean running;

    private final long updatePeriodMS;

    /** current update index */
    private int visit;

    /** how many items to visit during update */
    private final int updateBatchSize;
    private float initial = 0.5f;
    private float getBoost = 0.02f;
    private float forget = 0.05f;

    public HijackTermIndex(ConceptBuilder cb, int capacity, int reprobes) {
        super(cb);

        updateBatchSize = 4096; //1 + (capacity / (reprobes * 2));
        updatePeriodMS = 100;

        this.table = new PLinkHijackBag<Termed>(capacity, reprobes) {
            @Override
            protected boolean replace(PLink<Termed> incoming, PLink<Termed> existing) {
                if (incoming.get() instanceof PermanentConcept)
                    return true;
                if (existing.get() instanceof PermanentConcept)
                    return false;
                return super.replace(incoming, existing);
            }
        };
    }

    @Override
    public void start(NAR nar) {
        super.start(nar);
        running = true;
        updateThread = new Thread(this);
        updateThread.start();
    }

    @Override
    public void commit(Concept c) {
        get(c.term(), false); //get boost
    }

    @Override
    public @Nullable Termed get(@NotNull Term key, boolean createIfMissing) {
        @Nullable PLink<Termed> x = table.get(key);
        if (x != null) {
            x.priAdd(getBoost);
            return x.get(); //cache hit
        } else {

            if (createIfMissing) {
                Termed kc = conceptBuilder.apply(key);
                if (kc!=null) {
                    PLink<Termed> inserted = table.put(new RawPLink<>(kc, initial));
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


    @Override
    public void set(@NotNull Term src, Termed target) {
        remove(src);
        //permanent.put(src, target);
        table.put(new RawPLink<>(target, 1f));
    }

    @Override
    public void clear() {
        table.clear();
    }

    @Override
    public void forEach(@NotNull Consumer<? super Termed> c) {
        //TODO make sure this doesnt visit a term twice appearing in both tables but its ok for now
        table.forEachKey(c);
        //permanent.values().forEach(c);
    }

    @Override
    public int size() {
        return table.size(); /** approx since permanent not considered */
    }

    @Override
    public @NotNull String summary() {

        return table.size() + " concepts"; // (" + permanent.size() + " permanent)";
    }

    @Override
    public void remove(@NotNull Term entry) {
        table.remove(entry);
        //permanent.remove(entry);
    }

    @Override
    public void run() {
        while (running) {

            AtomicReferenceArray<PLink<Termed>> tt = table.map.get();

            int c = tt.length();

            int visit = this.visit;
            int n = updateBatchSize;

            for (int i = 0; i < n; i++ , visit++) {

                if (visit >= c) visit = 0;

                PLink<Termed> x = tt.get(visit);
                if (x!=null)
                    update(x);
            }

            this.visit = visit;

            //Util.pause(updatePeriodMS);
            Util.sleep(updatePeriodMS);
        }
    }

    protected void update(PLink<Termed> x) {

        //TODO better update function based on Concept features
        Termed c = x.get();
        if (!(c instanceof PermanentConcept)) {
            float decayRate = c.complexity() / ((float)Param.COMPOUND_VOLUME_MAX);
                    // / (1f + c.beliefs().priSum() + c.goals().priSum());
            x.priMult(1f - forget * Util.unitize(decayRate));
        }
    }

}
