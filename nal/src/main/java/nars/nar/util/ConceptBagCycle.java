package nars.nar.util;

import nars.*;
import nars.bag.Bag;
import nars.bag.impl.CurveBag;
import nars.budget.merge.BudgetMerge;
import nars.concept.Concept;
import nars.concept.ConceptBuilder;
import nars.link.BLink;
import nars.util.data.MutableInteger;
import nars.util.data.Range;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMapUnsafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
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
    private final ConceptBuilder conceptBuilder;

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

        int taskLinks = tasklinksFiredPerFiredConcept.intValue();
        int termLinks = termlinksFiredPerFiredConcept.intValue();

        for (int cycleNum = 0; cycleNum < cycles; cycleNum++)
            cycle(cpf, taskLinks, termLinks);


    }

    void cycle(int cpf, int taskLinks, int termLinks) {

        concepts.commit();

        List<BLink<Concept>> toFire = $.newArrayList();

        //gather the concepts into a list before firing. if firing while sampling, the bag can block itself
        int cpfSampled = (int)Math.ceil(cpf * Param.BAG_OVERSAMPLING);
        concepts.sample(cpfSampled, toFire::add);

        int conceptsFired = 0;
        int premisesFired = 0;

        for (int i = 0, toFireSize = toFire.size(); i < toFireSize && conceptsFired < cpf; i++) {
            @Nullable Concept c = toFire.get(i).get();
            if (c != null) {
                FireConceptSquared f = new FireConceptSquared(c, nar,
                        taskLinks, termLinks,
                        nar::inputLater);

                int p = f.premisesFired;
                if (p > 0) {
                    premisesFired += p;
                    conceptsFired++;
                }

            }
        }
    }

    /** extends CurveBag to invoke entrance/exit event handler lambda */
    public final class MonitoredCurveBag extends CurveBag<Concept> {

        final NAR nar;

        public MonitoredCurveBag(NAR nar, int capacity, @NotNull CurveSampler sampler) {
            super(capacity, sampler, BudgetMerge.plusBlend,
                    //new ConcurrentHashMap<>(capacity)
                    nar.exe.concurrent() ?  new ConcurrentHashMapUnsafe<>(capacity) : new HashMap(capacity)
                    //new NonBlockingHashMap<>(capacity)
            );
            this.nar = nar;
        }

        @Override
        public void clear() {
            forEach((BLink<Concept> v) -> { if (v!=null) sleep(v.get()); }); //HACK allow opportunity to process removals
            super.clear();
        }

        @Override
        protected final void onActive(@NotNull Concept c) {
            awake(c);
        }

        @Override
        protected final void onRemoved(@NotNull Concept c, @Nullable BLink<Concept> value) {
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
