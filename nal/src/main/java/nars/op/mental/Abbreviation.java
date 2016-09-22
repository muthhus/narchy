package nars.op.mental;

import nars.*;
import nars.bag.Bag;
import nars.bag.impl.CurveBag;
import nars.budget.Budget;
import nars.budget.merge.BudgetMerge;
import nars.concept.AtomConcept;
import nars.concept.Concept;
import nars.link.BLink;
import nars.link.DefaultBLink;
import nars.op.MutaTaskBag;
import nars.task.GeneratedTask;
import nars.task.MutableTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import nars.term.subst.FindSubst;
import nars.truth.TruthDelta;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static nars.nal.UtilityFunctions.or;
import static nars.time.Tense.ETERNAL;
import static nars.util.Util.unitize;

/**
 * compound<->dynamic atom abbreviation.
 *
 * @param S serial term type
 */
public class Abbreviation/*<S extends Term>*/ extends MutaTaskBag<BLink<Compound>> {

    static final Logger logger = LoggerFactory.getLogger(Abbreviation.class);

    private static final AtomicInteger currentTermSerial = new AtomicInteger(0);

    /** when a concept is important and exceeds a syntactic complexity above
     * this value multiplied by the NAR's volume limit, then LET NARS NAME IT. */
    public final MutableFloat abbreviationRelativeVolThreshold = new MutableFloat(0.5f);





    //TODO different parameters for priorities and budgets of both the abbreviation process and the resulting abbreviation judgment
    //public AtomicDouble priorityFactor = new AtomicDouble(1.0);
    /**
     * generated abbreviation belief's confidence
     */
    public final MutableFloat abbreviationConfidence;

    /** abbreviations per processed task */
    public final MutableFloat abbreviationProbability = new MutableFloat(1f);

    @NotNull
    protected final NAR nar;
    private final String termPrefix;
    private float volThresh;
    private float threshFalloffRate;


    public Abbreviation(@NotNull NAR n, String termPrefix, float selectionRate, int capacity) {
        super(selectionRate, new CurveBag<>(BudgetMerge.plusBlend, n.random), n);
        this.nar = n;
        this.termPrefix = termPrefix;
        this.bag.setCapacity(capacity);
        this.abbreviationConfidence = new MutableFloat(nar.confidenceDefault(Symbols.BELIEF));
    }

    @Nullable
    @Override
    protected BLink<Compound> filter(Task task) {
        Term t = task.term();

        float score;
        if ((score = scoreIfExceeds(task,nar.random.nextFloat())) > 0) {
            @NotNull Budget b = task.budget();
            return new DefaultBLink(t,
                    or(b.priIfFiniteElseZero(), score),
                    b.dur(),
                    b.qua());
        } else {
            return null;
        }
    }

    @Override
    protected void next(NAR n) {
        //calculate this once per frame. not every task process
        volThresh = abbreviationRelativeVolThreshold.floatValue() * nar.compoundVolumeMax.intValue();

        //how quickly to decrease probability for each degree of volume away from the thresh
        //this is measured relative to the larger of either the distance to the max
        //volume, or min volume (1)
        threshFalloffRate = Math.min(
                Math.abs(nar.compoundVolumeMax.floatValue() - volThresh),
                nar.compoundVolumeMax.floatValue() - 1 );

        super.next(n);
    }

    @Override
    protected void accept(BLink<Compound> b) {

        Term term = b.get();
        Concept abbreviated = nar.concept(term);
        if (abbreviated != null && abbreviated.get(Abbreviation.class) == null) {
            abbreviate(abbreviated, b);
        }

    }

    @Nullable
    static Compound abbreviate(@NotNull Concept abbreviated, @NotNull Term id) {
        return
                (Compound) $.sim(abbreviated.term(), id);
                //(Compound) $.equi(abbreviated.term(), id);
                //(Compound) $.secte(abbreviated.term(), id);

        //old 1.6 pattern:
        //Operation compound = Operation.make(
        //    Product.make(termArray(termAbbreviating)), abbreviate);*/
    }

    @NotNull
    protected String newSerialTerm() {

        return (termPrefix + "_" + Integer.toString(currentTermSerial.incrementAndGet(), 36));

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

    private float scoreIfExceeds(Task task, float min) {
        float s = task.priIfFiniteElseZero() * task.qua();
        if (s >= min) {
            s *= abbreviationProbability.floatValue();
            if (s >= min) {

                //higher probability for terms nearer the thresh. smaller and larger get less chance
                s *= 1f - unitize(
                        Math.abs(task.volume() - volThresh) /
                                (threshFalloffRate) );

                if (s >= min)
                    return s;
            }
        }
        return -1;
    }

    protected void abbreviate(@NotNull Concept abbreviated, Budget b) {
        //Concept abbreviation = nar.activate(, NewAbbreviationBudget);

        AtomicAbbreviation alias = new AtomicAbbreviation(newSerialTerm(), abbreviated);
        nar.on(alias);

        Compound abbreviation = abbreviate(abbreviated, alias);
        if (abbreviation != null) {


            abbreviated.put(Abbreviation.class, alias); //abbreviated by the serial

            //logger.info("Abbreviation {}", abbreviation);


            MutableTask t = new GeneratedTask(abbreviation, Symbols.BELIEF,
                    $.t(1, abbreviationConfidence.floatValue())) {

                @Override public void feedback(TruthDelta delta, float deltaConfidence, float deltaSatisfaction, NAR nar) {
                    super.feedback(delta, deltaConfidence, deltaSatisfaction, nar);
                    if (!isDeleted()) {
                        //redirect concept resolution to the abbreviation alias
                        nar.concepts.set(abbreviated.term(), alias);
                    }
                }

            }
//                @Override
//                public void feedback(TruthDelta delta, float deltaConfidence, float deltaSatisfaction, NAR nar) {
//                    Concept abbreviatedConcept = nar.concept(abbreviated, true);
//
//                    //Concept aliasConcept = nar.concept(alias, true);
//                    if (abbreviatedConcept!=null) {
//                        abbreviatedConcept.put(Abbreviation.class, abbreviation);
//                        //abbreviatedConcept.crossLink(b, this, aliasConcept, 1f, nar);
//                    } else {
//                        logger.error("alias unconceptualized: {}", alias);
//                    }
//                }
            ;
            t.time(nar.time(), ETERNAL);
            t.setBudget(b);
            t.log("Abbreviate");

            nar.inputLater( t );

            logger.info("new: {}", t);

        }
    }

    static class AtomicAbbreviation extends AtomConcept {

        private final Concept abbr;

        public AtomicAbbreviation(String term, Concept abbreviated) {
            super(term, Op.ATOM, abbreviated.termlinks(), abbreviated.tasklinks());
            this.abbr = abbreviated;
        }

        @Override
        public @Nullable TermContainer templates() {
            return abbr.templates();
        }

        /** equality will have already been tested here, and the parent super.unify() method is just return false. so skip it and just try the abbreviated */
        @Override public boolean unify(@NotNull Term y, @NotNull FindSubst subst) {
            return /*super.unify(y, subst) || */abbr.term().unify(y, subst);
        }
    }


}
