package nars.nar;

import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.bag.Bag;
import nars.bag.impl.experimental.HijackBag;
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
import nars.term.Term;
import nars.term.Termed;
import nars.time.Clock;
import nars.time.FrameClock;
import nars.util.data.random.XorShift128PlusRandom;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

import static java.lang.System.out;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

/**
 * ALANN Hybrid - experimental
 */
public class Default2 extends NAR {

    private static final Logger logger = LoggerFactory.getLogger(Default2.class);

    public final HijackBag<Concept> active;
    public final List<GraphPremiseBuilder> cores;

    final static Deriver deriver = Deriver.getDefaultDeriver();
    private float _activation;


    public final class GraphPremiseBuilder implements Runnable {

        Concept at;
        //Bag<Concept> local = new HijackBag<>(32, 4, BudgetMerge.avgBlend, random);
        public Bag<Term> terms = new HijackBag<>(24, 4, BudgetMerge.plusBlend, random);
        Bag<Task>    tasklinks = new HijackBag<>(16, 4, BudgetMerge.plusBlend, random);

        int iterations = 1;
        int tasklinksFiring = 2;
        int termlinksFiring = 4;


        public GraphPremiseBuilder() {
        }

        protected void seed(int num) {
            active.sample(num, c -> {
                terms.put(c.get().term(), c);
                return true;
            });
        }

        @Override
        public void run() {
            for (int i = 0; i < iterations; i++) {
                iterate();
            }
        }

        /** a value which should be less than 1.0,
         * indicating the preference for the current value vs. a tendency to move */
        float momentum = 0.1f;

        void iterate() {

            //decide whether to remain here
            boolean move;
            if (at!=null) {
                move = (random.nextFloat() > (1f-momentum)*active.pri(at, 0));
            } else {
                move = true;
            }

            if (move) {

                BLink<Term> next = go();

                if (next == null) {
                    seed(1);
                    //seed(terms.capacity()/2); //the more seeded from 'active', the less localized this worker's interactions
                    go();
                } else {
                    //seed(1);
                }
            }

            if (at != null) {

                PremiseMatrix.run(at, Default2.this,
                        tasklinksFiring, termlinksFiring,
                        Default2.this::input, //input them within the current thread here
                        deriver,
                        this.tasklinks, terms
                );
            }

        }


        private BLink<Term> go() {
            BLink<Term> next = terms.commit().sample();
            if (next != null) {

                Concept d = Default2.this.concept(next.get());
                if (d != null) {

                    List<Task> trash = $.newArrayList(0);
                    d.policy(concepts.conceptBuilder().awake(), time(), trash);
                    tasks.remove(trash);


                    d.termlinks().commit().transfer(2, terms);
                    d.tasklinks().commit().transfer(2, tasklinks);

                    this.at = d;
                }
            }
            return next;
        }

        void print() {
            logger.info("at: {}", at);
            //out.println("\nlocal:"); local.print();
            out.println("\ntermlinks:"); terms.print();
            out.println("\ntasklinks:"); tasklinks.print();
        }

        public int duty() {
            return tasklinksFiring * termlinksFiring * iterations; /* * expected hit rate */
        }

    }

    public Default2() {
        this(new FrameClock(),
                //new SingleThreadExecutioner()
                new MultiThreadExecutioner(4)
        );
    }

    public Default2(@NotNull Clock clock, Executioner exe) {
        super(clock, new TreeTermIndex.L1TreeIndex(new DefaultConceptBuilder(), 1024*1024, 8192, 4),
                new XorShift128PlusRandom(1), Param.defaultSelf(), exe);

        active = new HijackBag<>(1024, 6, random);

        durMin.setValue(BUDGET_EPSILON * 2f);


        int level = level();

        if (level >= 7) {

            initNAL7();

            if (level >= 8) {

                initNAL8();

            }

        }

        int numCores = 32;
        this.cores = range(0, numCores ).mapToObj(i -> new GraphPremiseBuilder()).collect(toList());

        onFrame(()-> {
            this._activation = getNextActivationRate();
            active.commit();
            runLater(cores, GraphPremiseBuilder::run, 1);
        });
    }

    private float getNextActivationRate() {
        return 1f / (cores.size() * cores.stream().mapToInt(GraphPremiseBuilder::duty).sum());

    }

    private STMTemporalLinkage stmLinkage;

    /** NAL7 plugins */
    protected void initNAL7() {

        stmLinkage = new STMTemporalLinkage(this, 2);

    }

    /* NAL8 plugins */
    protected void initNAL8() {

    }


    @Override
    public final Concept concept(Term term, float boost) {
        Concept c = concept(term);
        if (c!=null) {
            return active.boost(c, boost);
        }
        return null;
    }

    @Override
    public final void activationAdd(ObjectFloatHashMap<Concept> concepts, Budgeted in, float activation, MutableFloat overflow) {
        active.put(concepts, in, activation * _activation, overflow);
    }

    @Override
    public final float activation(@NotNull Termed concept) {
        BLink<Concept> c = active.get(concept);
        return c != null ? c.priIfFiniteElseZero() : 0;
    }


    @NotNull
    @Override
    public NAR forEachActiveConcept(@NotNull Consumer<Concept> recip) {
        active.forEachKey(recip);
        return this;
    }

    @Override
    public void clear() {
        //TODO use a 'clear' event handler that these can attach to

        active.clear();

        if (stmLinkage!=null)
            stmLinkage.clear();

    }


    public static void main(String[] args) {
        new Default2().log()
                .believe("(a-->b)")
                .believe("(b-->c)")
                .believe("(c-->d)")
                .run(64);
    }


}
