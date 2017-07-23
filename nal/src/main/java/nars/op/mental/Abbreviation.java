package nars.op.mental;

import jcog.bag.impl.CurveBag;
import jcog.data.MutableIntRange;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.Priority;
import jcog.pri.op.PriMerge;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.bag.leak.TaskLeak;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.task.NALTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static nars.Op.BELIEF;
import static nars.term.Terms.compoundOrNull;
import static nars.time.Tense.ETERNAL;

/**
 * compound<->dynamic atom abbreviation.
 *
 * @param S serial term type
 */
public class Abbreviation/*<S extends Term>*/ extends TaskLeak<Compound, PriReference<Compound>> {


    /**
     * generated abbreviation belief's confidence
     */
    @NotNull
    public final MutableFloat abbreviationConfidence;

    /**
     * whether to use a (strong, proxying) alias atom concept
     */
    boolean aliasConcept = false;

    private static final Logger logger = LoggerFactory.getLogger(Abbreviation.class);

    private static final AtomicInteger currentTermSerial = new AtomicInteger(0);

    @NotNull
    protected final NAR nar;
    private final String termPrefix;

    /**
     * accepted volume range, inclusive
     */
    public final MutableIntRange volume;


    public Abbreviation(@NotNull NAR n, String termPrefix, int volMin, int volMax, float selectionRate, int capacity) {
        super(
                new CurveBag(PriMerge.plus, new ConcurrentHashMap<>(capacity), n.random(), capacity
        ), selectionRate, n);

        this.nar = n;
        this.termPrefix = termPrefix;
        this.setCapacity(capacity);
        this.abbreviationConfidence =
                new MutableFloat(nar.confDefault(BELIEF));
        //new MutableFloat(1f - nar.truthResolution.floatValue());
        //new MutableFloat(nar.confidenceDefault(Symbols.BELIEF));
        volume = new MutableIntRange(volMin, volMax);
    }

    @Nullable
    @Override
    protected void in(@NotNull Task task, @NotNull Consumer<PriReference<Compound>> each) {

        if (task.meta(Abbreviation.class)!=null)
            return;

        Priority b = task;
        if (b != null)
            input(b, each, (Compound)task.term(), 1f);
    }

    private void input(@NotNull Priority b, @NotNull Consumer<PriReference<Compound>> each, @NotNull Compound t, float scale) {
        int vol = t.volume();
        if (vol < volume.lo())
            return;

        if (vol <= volume.hi()) {
            if (t.vars() == 0 && t.conceptual().equals(t) /* identical to its conceptualize */ ) {
                Concept abbreviable = (Concept) nar.concept(t);
                if ((abbreviable == null) ||
                        !(abbreviable instanceof PermanentConcept) &&
                                abbreviable.get(Abbreviation.class) == null) {

                    each.accept(new PLink<>(t, b.priSafe(0)));
                }
            }
        } else {
            //recursiely try subterms of a temporal or exceedingly large concept
            //budget with a proportion of this compound relative to their volume contribution
            float subScale = 1f / (1 + t.size());
            t.forEachCompound(x -> {
                if (x instanceof Compound)
                    input(b, each, ((Compound) x), subScale);
            });
        }
    }


    @Override
    protected float onOut(PriReference<Compound> b) {

        abbreviate(b.get(), b);
        return 1f;

    }


    @NotNull
    protected String newSerialTerm() {

        return termPrefix + Integer.toString(currentTermSerial.incrementAndGet(), 36);

//        int olen = name.length();
//        switch (olen) {
//            case 0:
//                throw new RuntimeException("empty atom name: " + name);
//
////            //re-use short term names
////            case 1:
////            case 2:
////                return theCached(name);
//
//            default:
//                if (olen > Short.MAX_VALUE/2)
//                    throw new RuntimeException("atom name too long");

        //  }
    }

//    private float scoreIfExceeds(Budget task, float min) {
//        float s = or(task.priIfFiniteElseZero(), task.qua());
//        if (s >= min) {
//            s *= abbreviationProbability.floatValue();
//            if (s >= min) {
//
//                //higher probability for terms nearer the thresh. smaller and larger get less chance
////                s *= 1f - unitize(
////                        Math.abs(task.volume() - volThresh) /
////                                (threshFalloffRate) );
//
//                if (s >= min)
//                    return s;
//            }
//        }
//        return -1;
//    }


    //private boolean createRelation = false;


    protected boolean abbreviate(@NotNull Compound abbreviated, @NotNull Priority b) {

        String id;
//            id = newCanonicalTerm(abbreviated);
//            if (id == null)
        id = newSerialTerm();


        Compound abbreviation = newRelation(abbreviated, id);

        Term[] aa;
        if (abbreviation != null) {

            @Nullable Concept a = nar.concept(abbreviated);
            if (a != null && a.term() instanceof Compound) {

                if (a.putIfAbsent(Abbreviation.class, id) == null) {

                    AliasConcept ac = AliasConcept.get(id, a, nar);
                    Concept alias = aliasConcept ?
                            nar.on(ac) : null;




                    nar.terms.set(abbreviated, ac); //set the abbreviated term to resolve to the abbreviation

                    Termed aliasTerm = alias != null ? alias : Atomic.the(id);

                    //if (abbreviation != null) {

                    //logger.info("{} <=== {}", alias, abbreviatedTerm);
                    Term abbreviatedTerm = abbreviated.term();

                    Task abbreviationTask = new NALTask(
                            abbreviation, BELIEF, $.t(1f, abbreviationConfidence.floatValue()),
                            nar.time(), ETERNAL, ETERNAL,
                            new long[]{nar.time.nextStamp()}
                    );
                    abbreviationTask.meta(Abbreviation.class, new Term[] { abbreviatedTerm, aliasTerm.term() });
                    abbreviationTask.log("Abbreviate"); //, abbreviatedTerm, aliasTerm
                    abbreviationTask.priority().setPri(b);
                    //abbreviationTask.priority();
//        if (srcCopy == null) {
//            delete();
//        } else {
//            float p = srcCopy.priSafe(-1);
//            if (p < 0) {
//                delete();
//            } else {
//                setPriority(p);
//            }
//        }
//
//        return this;

                    nar.input(abbreviationTask);
                    logger.info("+ {}", abbreviationTask);
                    return true;
                }
            }
        }

        return false;
    }

//        final NLPGen nlpGen = new NLPGen();
//
//        @Nullable private String newCanonicalTerm(@NotNull Termed abbreviated) {
//            if (abbreviated.volume() < 12)
//                return "\"" + nlpGen.toString(abbreviated.term(), 1f, 1f, Tense.Eternal) + "\"";
//            return null;
//        }

    @Nullable
    Compound newRelation(@NotNull Compound abbreviated, @NotNull String id) {
        return compoundOrNull(
                $.sim(abbreviated, Atomic.the(id))
                //$.equi
        );
        //(Compound) $.equi(abbreviated.term(), id);
        //(Compound) $.secte(abbreviated.term(), id);

        //old 1.6 pattern:
        //Operation compound = Operation.make(
        //    Product.make(termArray(termAbbreviating)), abbreviate);*/
    }


//    public static class AbbreviationAlias extends Abbreviation {
//        public AbbreviationAlias(@NotNull NAR n, String termPrefix, int volMin, int volMax, float selectionRate, int capacity) {
//            super(n, termPrefix, volMin, volMax, selectionRate, capacity);
//        }
//
//        protected void abbreviate(@NotNull CompoundConcept abbreviated, Budget b) {
//
//            String id = newSerialTerm();
//
//            Compound abbreviatedTerm = abbreviated.term();
//
//            AliasConcept alias = new AliasConcept(id, abbreviated, nar);
//            nar.concepts.set(alias, alias);
//            nar.concepts.set(abbreviatedTerm, alias); //override the abbreviated on lookups
//            logger.info("{} <=== {}", alias, abbreviatedTerm);
//
//        }
//    }
//


//    public static class AbbreviationTask extends NALTask {
//
//        @NotNull
//        private final Compound abbreviated;
//        @NotNull
//        private final Term alias;
//
//        public AbbreviationTask(Compound term, byte punc, Truth truth, long creation, long start, long end, long[] evidence, @NotNull Compound abbreviated, @NotNull Termed alias) {
//            super(term, punc, truth, creation, start, end, evidence);
//            this.abbreviated = abbreviated;
//            this.alias = alias.term();
//        }
//
//
////        @Override
////        public void feedback(TruthDelta delta, float deltaConfidence, float deltaSatisfaction, @NotNull NAR nar) {
////
////            super.feedback(delta, deltaConfidence, deltaSatisfaction, nar);
////
////
////            if (deltaConfidence == deltaConfidence /* wasn't deleted, even for questions */ && !isDeleted()) {
////                @Nullable Concept c = concept(nar);
////                c.put(Abbreviation.class, alias);
////
////            }
////        }
//    }
}
