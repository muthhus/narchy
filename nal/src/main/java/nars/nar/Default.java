package nars.nar;


import jcog.random.XorShift128PlusRandom;
import nars.NAR;
import nars.conceptualize.ConceptBuilder;
import nars.conceptualize.DefaultConceptBuilder;
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



    public final STMTemporalLinkage stmLinkage = new STMTemporalLinkage(this, 1);
    //public final STMTemporalLinkage2 stmLinkage = new STMTemporalLinkage2(this, 4, 2, 2);

    @Deprecated
    public Default() {
        this(128);
    }

    public Default(int activeConcepts) {
        this(
            new DefaultTermIndex(activeConcepts * INDEX_TO_CORE_INITIAL_SIZE_RATIO),
            new CycleTime(),
            new BufferedSynchronousExecutor(activeConcepts, 0.25f));
    }

    public static final int INDEX_TO_CORE_INITIAL_SIZE_RATIO = 8;


    public Default(@NotNull TermIndex concepts, @NotNull Time time, Executioner exe) {
        this(new XorShift128PlusRandom(1), concepts, time, exe);
    }

    public Default(@NotNull Random random, @NotNull TermIndex concepts, @NotNull Time time, Executioner exe) {
        super(time, concepts, random, exe);

    }


    /**
     * suitable for single-thread, testing use only. provides no limitations on size so it will grow unbounded. use with caution
     */
    public static class DefaultTermIndex extends MapTermIndex {

        public DefaultTermIndex(int capacity) {
            this(capacity, new DefaultConceptBuilder());
        }

        public DefaultTermIndex(int capacity, ConceptBuilder cb) {
            super(
                    cb,
                    new HashMap<>(capacity),
                    new HashMap<>(capacity)
                    //new ConcurrentHashMap<>(capacity),
                    //new ConcurrentHashMap<>(capacity)
                    //new ConcurrentHashMapUnsafe(capacity)
            );
        }
    }


}
