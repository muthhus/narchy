package nars.nar;

import jcog.bag.Bag;
import jcog.bag.impl.hijack.PLinkHijackBag;
import jcog.pri.PLink;
import jcog.random.XorShift128PlusRandom;
import nars.NAR;
import nars.concept.Concept;
import nars.conceptualize.DefaultConceptBuilder;
import nars.control.ConceptBagFocus;
import nars.control.FireConcepts;
import nars.index.term.TermIndex;
import nars.index.term.map.MapTermIndex;
import nars.op.stm.STMTemporalLinkage;
import nars.time.CycleTime;
import nars.time.Time;
import nars.util.exe.BufferedSynchronousExecutor;
import nars.util.exe.Executioner;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Random;

/**
 * Various extensions enabled
 */
public class Default extends NAR {

    //private static final Logger logger = LoggerFactory.getLogger(Default.class);


    public final ConceptBagFocus focus;

    public final STMTemporalLinkage stmLinkage = new STMTemporalLinkage(this, 2);
    //public final STMTemporalLinkage2 stmLinkage = new STMTemporalLinkage2(this, 4, 2, 2);

    public final FireConcepts deriver;

    @Deprecated
    public Default() {
        this(1024);
    }

    public Default(int activeConcepts) {
        this(activeConcepts,
            new DefaultTermIndex(activeConcepts * INDEX_TO_CORE_INITIAL_SIZE_RATIO),
            new CycleTime(),
            new BufferedSynchronousExecutor());
    }

    public static final int INDEX_TO_CORE_INITIAL_SIZE_RATIO = 8;


    public Default(int activeConcepts, @NotNull TermIndex concepts, @NotNull Time time, Executioner exe) {
        this(activeConcepts, new XorShift128PlusRandom(1), concepts, time, exe);
    }

    public Default(int activeConcepts, @NotNull Random random, @NotNull TermIndex concepts, @NotNull Time time, Executioner exe) {
        super(time, concepts, random, exe);

        ConceptBagFocus f = new ConceptBagFocus(this, newConceptBag(activeConcepts));
        this.focus = f;
        setFocus(f);


        deriver = //exe.concurrent() ?
                //new FireConcepts.FireConceptsBuffered(newPremiseBuilder(), this)
                //:
                new FireConcepts(focus, this);

        deriver.rate.setValue(20);
    }

    public Bag<Concept,PLink<Concept>> newConceptBag(int initialCapacity) {

        return new PLinkHijackBag(initialCapacity, 4);
//        return new CurveBag<>(initialCapacity, PriMerge.plus,
//                exe.concurrent() ?
//                    new ConcurrentHashMap<>(initialCapacity, 0.9f) :
//                    new HashMap<>(initialCapacity, 0.9f)
//        );

    }


    /**
     * suitable for single-thread, testing use only. provides no limitations on size so it will grow unbounded. use with caution
     */
    public static class DefaultTermIndex extends MapTermIndex {

        public DefaultTermIndex(int capacity) {
            super(
                    new DefaultConceptBuilder(),
                    new HashMap<>(capacity),
                    new HashMap<>(capacity)
                    //new ConcurrentHashMap<>(capacity),
                    //new ConcurrentHashMap<>(capacity)
                    //new ConcurrentHashMapUnsafe(capacity)
            );
        }
    }


}
