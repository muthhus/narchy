package nars.nar;

import nars.NAR;
import nars.Param;
import nars.Task;
import nars.bag.Bag;
import nars.bag.impl.CurveBag;
import nars.budget.Budgeted;
import nars.budget.merge.BudgetMerge;
import nars.concept.Concept;
import nars.index.term.tree.TreeTermIndex;
import nars.link.BLink;
import nars.nal.Deriver;
import nars.nar.exe.Executioner;
import nars.nar.exe.MultiThreadExecutioner;
import nars.nar.util.DefaultConceptBuilder;
import nars.nar.util.PremiseMatrix;
import nars.op.time.STMTemporalLinkage;
import nars.term.Termed;
import nars.time.FrameTime;
import nars.time.Time;
import nars.util.data.random.XorShift128PlusRandom;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static java.lang.System.out;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static nars.bag.impl.CurveBag.power2BagCurve;

/**
 * ALANN Hybrid - experimental
 * Adaptive Logic and Neural Network
 * Original design by Tony Lofthouse
 */
public class Alann extends NAR {

    private static final Logger logger = LoggerFactory.getLogger(Alann.class);

    static private final BudgetMerge blend = BudgetMerge.plusBlend;

    public final List<GraphPremiseBuilder> cores;

    final static Deriver deriver = Deriver.get("default.meta.nal");

    public final class GraphPremiseBuilder implements Runnable {

        /** how many tasklinks to absorb on a visit */
        public static final int TASKLINK_COLLECTION_RATE = 1;

        Concept here;

        private BLink<? extends Termed> linkHere;

        public final Bag<Concept> terms =
                //new HijackBag<>(128, 3, blend, random);
                new CurveBag(64, new CurveBag.NormalizedSampler(power2BagCurve, random), blend, new ConcurrentHashMap(64));

        /** the tasklink bag is only modified and accessed by the core, locally, so it does not need to have a concurrent Map */
        public final Bag<Task> tasklinks =
                //new HijackBag<>(128, 3, blend, random);
                new CurveBag(64, new CurveBag.NormalizedSampler(power2BagCurve, random), blend, new HashMap(64));

        int iterations = 16;
        int tasklinksFiring = 1;
        int termlinksFiring = 3;

        /* a cost (reduction) applied to the local 'term' bag */
        private float conceptVisitCost = 0.5f;

        /** a value which should be less than 1.0,
         * indicating the preference for the current value vs. a tendency to move */
        float momentum = 0.5f;


        @Override
        public void run() {
            for (int i = 0; i < Math.round(iterations * exe.load()); i++) {
                iterate();
            }
        }

        public  void loop() {
            while (true) {
                try {
                    //run();
                    iterate();
                } catch (Throwable t) {
                    logger.error("run: {}", t);
                }
            }
        }



        void iterate() {

            //decide whether to remain here
            boolean move;
            if (here !=null) {
                move = (random.nextFloat() > (momentum) * linkHere.priIfFiniteElseZero());
            } else {
                move = true;
            }

            if (move) {

                BLink<? extends Termed> next = go();

                if (next == null) {
                    //seed(seedRate);
                    go();
                }
            }

            if (here != null) {

                terms.mul(here, conceptVisitCost);

                PremiseMatrix.run(here, Alann.this,
                        tasklinksFiring, termlinksFiring,
                        Alann.this::input, //input them within the current thread here
                        deriver,
                        this.tasklinks, terms
                );
            }

        }


        @Nullable
        private BLink<Concept> go() {
            BLink<Concept> next = terms.commit().sample();
            if (next != null) {

                Concept d = concept(next.get());
                if (d != null) {

                    d.policy(concepts.conceptBuilder().awake(), Alann.this);

                    //d.termlinks().commit().transfer(2, terms);
                    d.tasklinks().commit().copy(tasklinks, TASKLINK_COLLECTION_RATE);

                    this.here = d;
                    this.linkHere = next;

                }
            }
            return next;
        }

        void print() {
            logger.info("at: {}", here);
            //out.println("\nlocal:"); local.print();
            out.println("\ntermlinks:"); terms.print();
            out.println("\ntasklinks:"); tasklinks.print();
        }

        public int duty() {
            return tasklinksFiring * termlinksFiring * iterations; /* * expected hit rate */
        }

    }

    public Alann() {
        this(new FrameTime());
    }

    public Alann(Time time) {
        this(time,
                //new SingleThreadExecutioner()
                new MultiThreadExecutioner(2, 1024*16).sync(false),
                4
        );
    }

    public Alann(@NotNull Time time, Executioner exe, int cores) {
        super(time, new TreeTermIndex.L1TreeIndex(new DefaultConceptBuilder(), 1024*1024, 16384, 3),
                new XorShift128PlusRandom(1), Param.defaultSelf(), exe);

        quaMin.setValue(BUDGET_EPSILON * 2f);

        int level = level();

        if (level >= 7) {
            initNAL7();

            if (level >= 8) {
                initNAL8();
            }

        }

        this.cores = range(0, cores).mapToObj(i -> new GraphPremiseBuilder()).collect(toList());

        AtomicBoolean running = new AtomicBoolean(false);
        runLater(()-> {
           if (running.compareAndSet(false,true)) {
               for (GraphPremiseBuilder b : this.cores)
                   new Thread(b::loop).start();
           }
        });

    }


    /** NAL7 plugins */
    protected void initNAL7() {

        STMTemporalLinkage stmLinkage = new STMTemporalLinkage(this, 2);

    }

    /* NAL8 plugins */
    protected void initNAL8() {

    }


    @Override
    public final Concept concept(@NotNull Termed term, float boost) {
        Concept c = concept(term);
        if (c!=null) {
            cores.get(Math.abs(term.hashCode()) % cores.size()).terms.add(term, boost);
        }
        return c;
    }

    @Override
    public final void activationAdd(@NotNull ObjectFloatHashMap<Concept> concepts, @NotNull Budgeted in, float activation, MutableFloat overflow) {
        int numCores = cores.size();

        concepts.forEachKeyValue((c,v)->{
            cores.get(Math.abs(c.hashCode()) % numCores).terms.put(c, in, activation * v, overflow);
        });
    }

    @Override
    public final float priority(@NotNull Termed concept) {
        //TODO impl
        return 0;
    }


    @NotNull
    @Override
    public NAR forEachActiveConcept(@NotNull Consumer<Concept> recip) {
        //TODO impl
        return this;
    }

}
