//package nars.nar;
//
//import nars.$;
//import nars.NAR;
//import nars.concept.Concept;
//import nars.index.term.TermIndex;
//import nars.index.term.map.MapTermIndex;
//import nars.link.BLink;
//import nars.nal.Deriver;
//import nars.nal.nal8.AbstractOperator;
//import nars.nar.core.ConceptBagCycle;
//import nars.nar.exe.Executioner;
//import nars.nar.util.DefaultConceptBuilder;
//import nars.op.data.*;
//import nars.op.mental.doubt;
//import nars.op.mental.schizo;
//import nars.op.out.say;
//import nars.op.sys.reset;
//import nars.op.time.STMTemporalLinkage;
//import nars.term.Termed;
//import nars.term.atom.Atom;
//import nars.time.Time;
//import objenome.O;
//import objenome.in;
//import objenome.the;
//import org.apache.commons.lang3.mutable.MutableFloat;
//import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Random;
//import java.util.function.Consumer;
//
///**
// * Various extensions enabled
// */
//@the
//public class Default3 extends NAR {
//
//    //private static final Logger logger = LoggerFactory.getLogger(Default.class);
//
//    @in
//    public @NotNull ConceptBagCycle core;
//
//
////    @Deprecated
////    public Default3() {
////        this(1024, 1, 1, 3);
////    }
////
////    public Default3(int activeConcepts, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept) {
////        this(activeConcepts, conceptsFirePerCycle, taskLinksPerConcept, termLinksPerConcept, new XorShift128PlusRandom(1));
////    }
////
////    public static final int INDEX_TO_CORE_INITIAL_SIZE_RATIO = 8;
////
//
////    public Default3(int activeConcepts, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept, @NotNull Random random) {
////        this(activeConcepts, conceptsFirePerCycle, taskLinksPerConcept, termLinksPerConcept, random,
////                new DefaultTermTermIndex(activeConcepts * INDEX_TO_CORE_INITIAL_SIZE_RATIO),
////                new FrameTime());
////    }
////
////
////    public Default3(int activeConcepts, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept, @NotNull Random random, @NotNull TermIndex index, @NotNull Time time) {
////        this(activeConcepts, conceptsFirePerCycle, taskLinksPerConcept, termLinksPerConcept, random, index, time, new SingleThreadExecutioner());
////    }
////
////    public Default3(int activeConcepts, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept, @NotNull Random random, @NotNull TermIndex index, @NotNull Time time, Executioner exe) {
////        super(time, index, random, Param.defaultSelf(), exe);
////
////
////
////
////
////
////
////        ConceptBagCycle c = new ConceptBagCycle(this, newDeriver(), activeConcepts);
////
////        c.termlinksFiredPerFiredConcept.set(termLinksPerConcept);
////        c.tasklinksFiredPerFiredConcept.set(taskLinksPerConcept);
////        c.conceptsFiredPerCycle.set(conceptsFirePerCycle);
////
////
////        int level = level();
////
////        if (level >= 7) {
////
////            initNAL7();
////
////            if (level >= 8) {
////
////                initNAL8();
////
////            }
////
////        }
////
////
////    }
////
//
//
//    public Default3(@NotNull Time time, @NotNull TermIndex concepts, @NotNull Random rng, @NotNull Atom self, Executioner exe) {
//        super(time, concepts, rng, self, exe);
//    }
//
//    public static NAR the() {
//        O o = O.start().with(newDeriver(), ConceptBagCycle.class);
//        return o.a(Default3.class);
//    }
//
//    protected static Deriver newDeriver() {
//        List<Deriver> modules = $.newArrayList();
//        modules.add(Deriver.get("default.meta.nal"));
//        modules.add(Deriver.get("nal4.meta.nal"));
//
//        return (x) -> modules.forEach(e -> e.accept(x));
//    }
//
////    @Nullable
////    private STMTemporalLinkage stmLinkage = null;
//
//    /** NAL7 plugins */
//    protected void initNAL7() {
//
//        STMTemporalLinkage stmLinkage = new STMTemporalLinkage(this, 2);
//
//    }
//
//    /* NAL8 plugins */
//    protected void initNAL8() {
//        for (AbstractOperator o : defaultOperators)
//            on(o);
//    }
//
//
//    @Nullable
//    @Override
//    public final Concept concept(Termed term, float priToAdd) {
//        return core.active.add(term, priToAdd);
//    }
//
//    @Override
//    public final void activationAdd(Iterable<ObjectFloatPair<Concept>> concepts, MutableFloat overflow) {
//        core.activate(concepts, overflow);
//    }
//
//    @Override
//    public final float priority(@NotNull Termed concept) {
//        BLink<Concept> c = core.active.get(concept);
//        return c != null ? c.priIfFiniteElseZero() : 0;
//    }
//
//
//
//    @NotNull
//    @Override
//    public NAR forEachActiveConcept(@NotNull Consumer<Concept> recip) {
//        core.active.forEachKey(recip);
//        return this;
//    }
//
//
//    /**
//     * suitable for single-thread, testing use only. provides no limitations on size so it will grow unbounded. use with caution
//     */
//    public static class DefaultTermTermIndex extends MapTermIndex {
//
//        public DefaultTermTermIndex(int capacity) {
//            super(
//                    new DefaultConceptBuilder(),
//                    new HashMap(capacity),
//                    new HashMap(capacity)
//                    //new ConcurrentHashMap<>(capacity),
//                    //new ConcurrentHashMap<>(capacity)
//                    //new ConcurrentHashMapUnsafe(capacity)
//            );
//        }
//    }
//
//    public final AbstractOperator[] defaultOperators = {
//
//            //system control
//
//            //PauseInput.the,
//            new reset(),
//            //new eval(),
//            //new Wait(),
//
////            new believe(),  // accept a statement with a default truth-value
////            new want(),     // accept a statement with a default desire-value
////            new wonder(),   // find the truth-value of a statement
////            new evaluate(), // find the desire-value of a statement
//            //concept operations for internal perceptions
////            new remind(),   // create/activate a concept
////            new consider(),  // do one inference step on a concept
////            new name(),         // turn a compount term into an atomic term
//            //new Abbreviate(),
//            //new Register(),
//
//            //new echo(),
//
//
//            new doubt(),        // decrease the confidence of a belief
////            new hesitate(),      // decrease the confidence of a goal
//
//            //Meta
//            new reflect(),
//            //new jclass(),
//
//            // feeling operations
//            //new feelHappy(),
//            //new feelBusy(),
//
//
//            // math operations
//            //new length(),
//            //new add(),
//
//            new intToBitSet(),
//
//            //new MathExpression(),
//
//            new complexity(),
//
//            //Term manipulation
//            new flat.flatProduct(),
//            new similaritree(),
//
//            //new NumericCertainty(),
//
//            //io operations
//            new say(),
//
//            new schizo(),     //change Memory's SELF term (default: SELF)
//
//            //new js(), //javascript evalaution
//
//            /*new json.jsonfrom(),
//            new json.jsonto()*/
//         /*
//+         *          I/O operations under consideration
//+         * observe          // get the most active input (Channel ID: optional?)
//+         * anticipate       // get the input matching a given statement with variables (Channel ID: optional?)
//+         * tell             // output a judgment (Channel ID: optional?)
//+         * ask              // output a question/quest (Channel ID: optional?)
//+         * demand           // output a goal (Channel ID: optional?)
//+         */
//
////        new Wait()              // wait for a certain number of clock cycle
//
//
//        /*
//         * -think            // carry out a working cycle
//         * -do               // turn a statement into a goal
//         *
//         * possibility      // return the possibility of a term
//         * doubt            // decrease the confidence of a belief
//         * hesitate         // decrease the confidence of a goal
//         *
//         * feel             // the overall happyness, average solution quality, and predictions
//         * busy             // the overall business
//         *
//
//
//         * do               // to turn a judgment into a goal (production rule) ??
//
//         *
//         * count            // count the number of elements in a set
//         * arithmatic       // + - * /
//         * comparisons      // < = >
//         * logic        // binary logic
//         *
//
//
//
//         * -assume           // local assumption ???
//         *
//         * observe          // get the most active input (Channel ID: optional?)
//         * anticipate       // get input of a certain pattern (Channel ID: optional?)
//         * tell             // output a judgment (Channel ID: optional?)
//         * ask              // output a question/quest (Channel ID: optional?)
//         * demand           // output a goal (Channel ID: optional?)
//
//
//        * name             // turn a compount term into an atomic term ???
//         * -???              // rememberAction the history of the system? excutions of operatons?
//         */
//    };
//
//
//
//
//
//}
