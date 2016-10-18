package nars.nar;

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
import nars.nar.exe.SingleThreadExecutioner;
import nars.nar.util.DefaultConceptBuilder;
import nars.nar.util.PremiseMatrixBuilder;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static java.lang.System.out;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

/**
 * ALANN Hybrid - experimental
 */
public class Default2 extends NAR {

    private static final Logger logger = LoggerFactory.getLogger(Default2.class);

    public final HijackBag<Concept> active;
    private final List<PremiseGraphBuilder> cores;

    final static Deriver deriver = Deriver.getDefaultDeriver();

    final class PremiseGraphBuilder implements Runnable {

        Concept at = null;
        //Bag<Concept> local = new HijackBag<>(32, 4, BudgetMerge.avgBlend, random);
        Bag<Term>    termlinks = new HijackBag<>(24, 2, BudgetMerge.plusBlend, random);
        Bag<Task>    tasklinks = new HijackBag<>(16, 2, BudgetMerge.plusBlend, random);
        float atPri = 0;
        int iterations = 1;

        public PremiseGraphBuilder() {
        }

        protected void seed(int num) {
            active.sample(num, c -> {
                termlinks.put(c.get().term(), c);
                return true;
            });
        }

        @Override
        public void run() {
            for (int i = 0; i < iterations; i++) {
                iterate();
            }
        }

        void iterate() {
            if (at!=null) {
                //decide whether to remain here
                if (random.nextFloat() > atPri) {
                    at = null;
                }
            }

            if (at == null) {

                seed(1); //the more seeded from 'active', the less localized this worker's interactions

                BLink<Term> next = termlinks.commit().sample();
                if (next != null) {


                    Concept d = Default2.this.concept(next.get());
                    if (d != null) {


                        d.termlinks().commit().sample(4, t -> {
                            termlinks.putLink(t);
                            return true;
                        });
                        ;
                        d.tasklinks().commit().sample(4, t -> {
                            tasklinks.putLink(t);
                            return true;
                        });

                        this.at = d;
                        this.atPri = next.pri(); //what about including the 'active.pri' measure
                    }
                }
            }

            if (at != null) {

                PremiseMatrixBuilder.run(at, Default2.this,
                        4, 4,
                        Default2.this::input, //input them within the current thread here
                        deriver,
                        tasklinks, termlinks
                );
            }

        }

        void print() {
            logger.info("at: {}", at);
            //out.println("\nlocal:"); local.print();
            out.println("\ntermlinks:"); termlinks.print();
            out.println("\ntasklinks:"); tasklinks.print();
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

        active = new HijackBag<>(2048, 4, random);

        durMin.setValue(BUDGET_EPSILON * 2f);


        int level = level();

        if (level >= 7) {

            initNAL7();

            if (level >= 8) {

                initNAL8();

            }

        }

        int numCores = 16;
        this.cores = range(0, numCores ).mapToObj(i -> new PremiseGraphBuilder()).collect(toList());

        onFrame(()-> {
            active.commit();
            runLater(cores, PremiseGraphBuilder::run, 1);
        });
    }

    private STMTemporalLinkage stmLinkage = null;

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
        active.put(concepts, in, activation, overflow);
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
