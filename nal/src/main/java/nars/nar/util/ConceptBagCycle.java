package nars.nar.util;

import nars.*;
import nars.bag.Bag;
import nars.bag.impl.CurveBag;
import nars.budget.merge.BudgetMerge;
import nars.concept.Concept;
import nars.link.BLink;
import nars.nal.Conclusion;
import nars.nal.Deriver;
import nars.nal.PremiseBuilder;
import nars.nal.derive.TrieDeriver;
import nars.nal.meta.PremiseEval;
import nars.term.Term;
import nars.util.data.MutableInteger;
import nars.util.data.Range;
import nars.util.data.list.FasterList;
import nars.util.data.map.nbhm.NonBlockingHashMap;
import nars.util.data.random.XorShift128PlusRandom;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;

/**
 * The original deterministic memory cycle implementation that is currently used as a standard
 * for development and testing.
 *
 */
public class ConceptBagCycle implements Consumer<NAR> {
    /**
     * How many concepts to fire each cycle; measures degree of parallelism in each cycle
     */
    @Range(min = 0, max = 64, unit = "Concept")
    public final @NotNull MutableInteger conceptsFiredPerCycle;


    private static final Logger logger = LoggerFactory.getLogger(ConceptBagCycle.class);



    /**
     * concepts active in this cycle
     */
    @NotNull
    public final Bag<Concept> concepts;

    @Deprecated
    public final transient @NotNull NAR nar;


    @Range(min = 0, max = 16, unit = "TaskLink") //TODO use float percentage
    public final MutableInteger tasklinksFiredPerFiredConcept = new MutableInteger(1);

    @Range(min = 0, max = 16, unit = "TermLink")
    public final MutableInteger termlinksFiredPerFiredConcept = new MutableInteger(1);


    private final MutableInteger cyclesPerFrame;
    private final Concept.ConceptBuilder conceptBuilder;

//
//    private static final Logger logger = LoggerFactory.getLogger(AbstractCore.class);

    //private final CapacityLinkedHashMap<Premise,Premise> recent = new CapacityLinkedHashMap<>(256);
    //long novel=0, total=0;

    public ConceptBagCycle(@NotNull NAR nar, int initialCapacity, MutableInteger cyclesPerFrame) {

        this.nar = nar;

        this.conceptsFiredPerCycle = new MutableInteger(1);
        this.cyclesPerFrame = cyclesPerFrame;
        this.conceptBuilder = nar.index.conceptBuilder();

        this.concepts = new MonitoredCurveBag(nar, initialCapacity, ((DefaultConceptBuilder) conceptBuilder).defaultCurveSampler);

        nar.onFrame(this);
        nar.eventReset.on(this::reset);

    }

    /** called when a concept is displaced from the concept bag */
    protected void sleep(@NotNull Concept c) {
        NAR n = this.nar;

        n.policy(c, conceptBuilder.sleep(), n.time());

        n.emotion.alert(1f / concepts.size());
    }

    /** called when a concept enters the concept bag
     * @return whether to accept the item into the bag
     * */
    protected boolean awake(@NotNull Concept c) {

        NAR n = this.nar;
        n.policy(c, conceptBuilder.awake(), n.time());

        return true;
    }


    public void reset(Memory m) {
        concepts.clear();
    }



    /** called each frame */
    @Override public void accept(NAR nar) {

        int cycles = cyclesPerFrame.intValue();

        int cpf = conceptsFiredPerCycle.intValue();

        short taskLinks = (short) tasklinksFiredPerFiredConcept.intValue();
        short termLinks = (short) termlinksFiredPerFiredConcept.intValue();

        List<BLink<Concept>> toFire = $.newArrayList();
        for (int cycleNum = 0; cycleNum < cycles; cycleNum++) {

            concepts.commit();

            //gather the concepts into a list before firing. if firing while sampling, the bag can block itself
            concepts.sample(cpf, toFire::add);

            for (int i = 0, toFireSize = toFire.size(); i < toFireSize; i++) {
                @Nullable Concept c = toFire.get(i).get();
                if (c != null) {
                    new FireConcept(c, nar,
                            taskLinks, termLinks,
                            new LinkedHashSet<>( 2 * (taskLinks*termLinks) /* estimate */ ))

                        /*@Override
                        public void accept(Premise p) {
                            total++;
                            if (recent.putIfAbsent(p,p)==null) {
                                super.accept(p);
                                novel++;
                            } else {
                                //System.err.println("duplicate (novel=" + Texts.n2(((float)novel/total)*100f) + "%)" );
                            }

                        }*/

                        .run();
                }
            }

        }

        //recent.clear();

    }

    /**
     * shared combined conclusion
     */
    public static class FireConcept extends Conclusion implements Runnable {

        private static final Logger logger = LoggerFactory.getLogger(FireConcept.class);

        private final Concept c;
        private final NAR nar;
        private final short tasklinks;
        private final short termlinks;
        private final TrieDeriver deriver;
        private int count;

        public FireConcept(Concept c, NAR nar, short tasklinks, short termlinks, Collection<Task> batch) {
            super(batch);
            this.c = c;
            this.nar = nar;
            this.deriver = Deriver.getDefaultDeriver();
            this.tasklinks = tasklinks;
            this.termlinks = termlinks;
        }



        @Override
        public void run() {

            try {

                c.commit();


                firePremiseSquared(
                        c,
                        tasklinks,
                        termlinks
                );

                if (!derive.isEmpty())
                    nar.inputLater(derive);

            } catch (Exception e) {

                if (Param.DEBUG)
                    e.printStackTrace();

                logger.error("run {}", e.toString());
            }

        }

        /**
         * iteratively supplies a matrix of premises from the next N tasklinks and M termlinks
         * (recycles buffers, non-thread safe, one thread use this at a time)
         */
        public final int firePremiseSquared(@NotNull Concept c, int tasklinks, int termlinks) {

            FasterList<BLink<Term>> termsBuffer = $.newArrayList(termlinks);
            c.termlinks().sample(termlinks, termsBuffer::addIfNotNull);

            count = 0;

            @NotNull PremiseEval mm = new PremiseEval(nar, deriver);

            if (!termsBuffer.isEmpty()) {

                FasterList<BLink<Task>> tasksBuffer = $.newArrayList(tasklinks);
                c.tasklinks().sample(tasklinks, tasksBuffer::addIfNotNull);

                if (!tasksBuffer.isEmpty()) {

                    for (int i = 0, tasksBufferSize = tasksBuffer.size(); i < tasksBufferSize; i++) {

                        PremiseBuilder.run(
                                c,
                                nar,
                                termsBuffer,
                                tasksBuffer.get(i),
                                mm,
                                (p) -> {
                                    mm.run(p, this);
                                    count++;
                                }
                        );

                    }

                    //tasksBuffer.clear();
                }

                //termsBuffer.clear();
            }


            return count;
        }

//
//        @Override
//        public void accept(Premise p) {
//            mm.run(p, this);
//            count++;
//        }
    }

    /** extends CurveBag to invoke entrance/exit event handler lambda */
    public final class MonitoredCurveBag extends CurveBag<Concept> {

        final NAR nar;

        public MonitoredCurveBag(NAR nar, int capacity, @NotNull CurveSampler sampler) {
            super(capacity, sampler, BudgetMerge.plusBlend,
                    //new ConcurrentHashMap<>(capacity)
                    new NonBlockingHashMap<>(capacity*2)
            );
            this.nar = nar;
        }

        @Override
        public void clear() {
            forEach((BLink<Concept> v) -> { if (v!=null) sleep(v.get()); }); //HACK allow opportunity to process removals
            super.clear();
        }

        @Override
        protected void onActive(@NotNull Concept c) {
            awake(c);
        }

        @Override
        protected void onRemoved(@NotNull Concept c, BLink<Concept> value) {
            if (value!=null)
                sleep(c);
        }

        @Override
        public @Nullable BLink<Concept> remove(@NotNull Concept x) {
            BLink<Concept> r = super.remove(x);
            if (r!=null) {
                sleep(x);
            }
            return r;
        }


    }


//    public void conceptualize(@NotNull Concept c, @NotNull Budgeted b, float conceptActivation, float linkActivation, NAR.Activation activation) {
//
//        concepts.put(c, b, conceptActivation, activation.overflow);
//        //if (b.isDeleted())
//            //return;
//            //throw new RuntimeException("Concept rejected: " + b);
//        if (linkActivation > 0)
//            c.link(b, linkActivation, nar, activation);
//    }


    //try to implement some other way, this is here because of serializability

}
