package nars.op.mental;

import nars.*;
import nars.bag.impl.CurveBag;
import nars.budget.Activation;
import nars.budget.Budget;
import nars.budget.merge.BudgetMerge;
import nars.concept.AtomConcept;
import nars.concept.CompoundConcept;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.link.BLink;
import nars.link.DefaultBLink;
import nars.op.MutaTaskBag;
import nars.table.BeliefTable;
import nars.table.QuestionTable;
import nars.task.GeneratedTask;
import nars.task.MutableTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.term.subst.Unify;
import nars.time.Tense;
import nars.truth.TruthDelta;
import nars.util.NLPGen;
import nars.util.data.MutableInteger;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static nars.nal.UtilityFunctions.or;
import static nars.time.Tense.ETERNAL;

/**
 * compound<->dynamic atom abbreviation.
 *
 * @param S serial term type
 */
abstract public class Abbreviation/*<S extends Term>*/ extends MutaTaskBag<BLink<Compound>> {

    static final Logger logger = LoggerFactory.getLogger(Abbreviation.class);

    private static final AtomicInteger currentTermSerial = new AtomicInteger(0);

    /**
     * when a concept is important and exceeds a syntactic complexity above
     * this value multiplied by the NAR's volume limit, then LET NARS NAME IT.
     */
    public final MutableInteger minAbbreviableVolume = new MutableInteger();

    //TODO different parameters for priorities and budgets of both the abbreviation process and the resulting abbreviation judgment
    //public AtomicDouble priorityFactor = new AtomicDouble(1.0);
    /**
     * generated abbreviation belief's confidence
     */
    public final MutableFloat abbreviationConfidence;

    /**
     * abbreviations per processed task
     */
    public final MutableFloat abbreviationProbability = new MutableFloat(2f);


    @NotNull
    protected final NAR nar;
    private final String termPrefix;
    private final int maxVol;


    public Abbreviation(@NotNull NAR n, String termPrefix, int volMin, int volMax, float selectionRate, int capacity) {
        super(selectionRate, new CurveBag<>(BudgetMerge.plusBlend, n.random), n);

        this.nar = n;
        this.termPrefix = termPrefix;
        this.bag.setCapacity(capacity);
        this.minAbbreviableVolume.set(volMin);
        this.abbreviationConfidence = new MutableFloat(1f - nar.truthResolution.floatValue());
            //new MutableFloat(nar.confidenceDefault(Symbols.BELIEF));
        this.maxVol = volMax;
    }

    @Nullable
    @Override
    protected BLink<Compound> filter(Task task) {

        if (task instanceof AbbreviationTask) //avoids feedback
            return null;

        Term t = task.term();
        int vol = t.volume();
        int minVol = this.minAbbreviableVolume.intValue();
        if ((vol >= minVol) && (vol <= maxVol)) {

            float score;
            if ((score = scoreIfExceeds(task, nar.random.nextFloat())) > 0) {

                score *= (1f / (1f + Math.max(0, (t.volume() - minVol)))); //decrease score by any additional complexity above the volume threshold
                @NotNull Budget b = task.budget();
                return new DefaultBLink(t,
                        score,
                        b.dur(),
                        b.qua());
            }
        }

        return null;
    }


    @Override
    protected void accept(BLink<Compound> b) {

        Term term = b.get();
        Concept abbreviable = nar.concept(term);
        if ((abbreviable != null) &&
                !(abbreviable instanceof PermanentConcept) &&
                !(abbreviable instanceof AliasConcept) &&
                term.vars() == 0 &&
                term.hasTemporal() &&
                abbreviable.get(Abbreviation.class) == null &&
                abbreviable.get(Concept.Savior.class) == null) {

            abbreviate((CompoundConcept) abbreviable, b);
        }

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
        float s = or(task.priIfFiniteElseZero(), task.qua());
        if (s >= min) {
            s *= abbreviationProbability.floatValue();
            if (s >= min) {

                //higher probability for terms nearer the thresh. smaller and larger get less chance
//                s *= 1f - unitize(
//                        Math.abs(task.volume() - volThresh) /
//                                (threshFalloffRate) );

                if (s >= min)
                    return s;
            }
        }
        return -1;
    }

    abstract protected void abbreviate(@NotNull CompoundConcept abbreviated, Budget b);

    public static class AbbreviationRelation extends Abbreviation {

        public AbbreviationRelation(@NotNull NAR n, String termPrefix, int volMin, int volMax, float selectionRate, int capacity) {
            super(n, termPrefix, volMin, volMax, selectionRate, capacity);
        }

        @Override
        protected void abbreviate(@NotNull CompoundConcept abbreviated, Budget b) {

            String id;
//            id = newCanonicalTerm(abbreviated);
//            if (id == null)
                id = newSerialTerm();

            Compound abbreviatedTerm = abbreviated.term();

            Compound abbreviation = newRelation(abbreviated, id);

            AliasConcept alias = new AliasConcept(id, abbreviated, nar, abbreviation);
            nar.on(alias);


            //if (abbreviation != null) {

            //logger.info("{} <=== {}", alias, abbreviatedTerm);

            AbbreviationTask abbreviationTask = new AbbreviationTask(
                    abbreviation, abbreviatedTerm, alias, abbreviationConfidence.floatValue());
            long now = nar.time();
            abbreviationTask.time(now,
                    //now);
                    ETERNAL);
            abbreviationTask.setBudget(b);
            abbreviationTask.log("Abbreviate");

            nar.inputLater( abbreviationTask );

            logger.info("{} <== {}\t{}", alias, abbreviatedTerm, abbreviationTask);
        }

        final NLPGen nlpGen = new NLPGen();

        @Nullable private String newCanonicalTerm(@NotNull Termed abbreviated) {
            if (abbreviated.volume() < 12)
                return "\"" + nlpGen.toString(abbreviated.term(), 1f, 1f, Tense.Eternal) + "\"";
            return null;
        }

        @Nullable
        static Compound newRelation(@NotNull Concept abbreviated, @NotNull String id) {
            return
                    (Compound) $.sim(abbreviated.term(), $.the(id));
            //(Compound) $.equi(abbreviated.term(), id);
            //(Compound) $.secte(abbreviated.term(), id);

            //old 1.6 pattern:
            //Operation compound = Operation.make(
            //    Product.make(termArray(termAbbreviating)), abbreviate);*/
        }

    }

    public static class AbbreviationAlias extends Abbreviation {
        public AbbreviationAlias(@NotNull NAR n, String termPrefix, int volMin, int volMax, float selectionRate, int capacity) {
            super(n, termPrefix, volMin, volMax, selectionRate, capacity);
        }

        protected void abbreviate(@NotNull CompoundConcept abbreviated, Budget b) {

            String id = newSerialTerm();

            Compound abbreviatedTerm = abbreviated.term();

            AliasConcept alias = new AliasConcept(id, abbreviated, nar);
            nar.concepts.set(alias, alias);
            nar.concepts.set(abbreviatedTerm, alias); //override the abbreviated on lookups
            logger.info("{} <=== {}", alias, abbreviatedTerm);

        }
    }

    /**
     * the proxy concepts present a bidirectional facade between a referenced and an alias term (alias term can be just a serial # atom wrapped in a product).
     * <p>
     * it replaces the index entry for the referenced with itself and also adds itself so from its start it intercepts all references to itself or the aliased (abbreviated) term whether this occurrs at the top level or as a subterm in another term (or as a subterm in another abbreviation, etc..)
     * <p>
     * the index is usually a weakmap or equivalent in which abbreviations can be forgotten as well as any other concept.
     * <p>
     * seen from a superterm containing one, it appears as a simple volume=2 concept meanwhile it could be aliasing a concept much larger than it. common "phrase" concepts with a volume >> 2 are good candidates for abbreviation. but when printed, the default toString() method is proxied so it will automatically decompress on output (or other serialization).
     */
    static final class AliasConcept extends AtomConcept {

        private final Concept abbr;
        private TermContainer templates;

        public AliasConcept(@NotNull String term, @NotNull Concept abbreviated, @NotNull NAR nar, Term... additionalTerms) {
            super(term, Op.ATOM, abbreviated.termlinks(), abbreviated.tasklinks());

            abbreviated.put(Concept.Savior.class, this);

            this.abbr = abbreviated;
            Term[] tl = ArrayUtils.add(abbreviated.templates().terms(), abbreviated.term());
            if (additionalTerms.length > 0)
                tl = ArrayUtils.addAll(tl, additionalTerms);
            this.templates = TermVector.the(tl);
            //rewriteLinks(nar);
        }


        /**
         * rewrite termlinks and tasklinks which contain the abbreviated term...
         * (but are not equal to since tasks can not have atom content)
         * ...replacing it with this alias
         */
        private void rewriteLinks(@NotNull NAR nar) {
            Term that = abbr.term();
            termlinks().compute(existingLink -> {
                Term x = existingLink.get();
                Term y = nar.concepts.replace(x, that, this);
                return (y != null && y != x && y!=Term.False) ?
                        termlinks().newLink(y, existingLink) :
                        existingLink;
            });
            tasklinks().compute(existingLink -> {
                Task xt = existingLink.get();
                Term x = xt.term();

                if (!x.equals(that) && !x.hasTemporal()) {
                    Term y = $.terms.replace(x, that, this);
                    if (y != x && y instanceof Compound) {
                        Task yt = MutableTask.clone(xt, (Compound) y, nar);
                        if (yt != null)
                            return termlinks().newLink(yt, existingLink);
                    }
                }

                return existingLink;

            });
        }


        @Override
        public void delete(NAR nar) {
            abbr.delete(nar);
            super.delete(nar);
        }

        @Override
        public @Nullable TermContainer templates() {
            return templates;
        }

//        @Override
//        public ConceptPolicy policy() {
//            return abbr.policy();
//        }
//        @Override
//        public void policy(@NotNull ConceptPolicy p, long now, List<Task> removed) {
//
//            //abbr.policy(p, now, removed);
//        }

        /**
         * equality will have already been tested here, and the parent super.unify() method is just return false. so skip it and just try the abbreviated
         */
        @Override
        public boolean unify(@NotNull Term y, @NotNull Unify subst) {
            return /*super.unify(y, subst) || */abbr.term().unify(y, subst);
        }

        @Override
        public boolean equals(Object u) {
            return super.equals(u);
        }

        @Override
        public final Activation process(@NotNull Task input, NAR nar) {
            return abbr.process(input, nar);
        }

        @Override
        public @Nullable Map<Object, Object> meta() {
            return abbr.meta();
        }

        @NotNull
        @Override
        public BeliefTable beliefs() {
            return abbr.beliefs();
        }

        @NotNull
        @Override
        public BeliefTable goals() {
            return abbr.goals();
        }

        @NotNull
        @Override
        public QuestionTable questions() {
            return abbr.questions();
        }

        @NotNull
        @Override
        public QuestionTable quests() {
            return abbr.quests();
        }
    }


    public static class AbbreviationTask extends GeneratedTask {

        @NotNull private final Compound abbreviated;
        @NotNull private final AliasConcept alias;

        public AbbreviationTask(Compound abbreviation, @NotNull Compound abbreviated, AliasConcept alias, float conf) {
            super(abbreviation, Symbols.BELIEF, $.t(1, conf));
            this.abbreviated = abbreviated;
            this.alias = alias;
        }

        @Override
        public void feedback(TruthDelta delta, float deltaConfidence, float deltaSatisfaction, NAR nar) {

            super.feedback(delta, deltaConfidence, deltaSatisfaction, nar);


            if (deltaConfidence==deltaConfidence /* wasn't deleted, even for questions */ && !isDeleted()) {
                @Nullable Concept c = concept(nar);
                c.put(Abbreviation.class, alias);
            }
        }
    }
}
