package nars.op.mental;

import jcog.data.MutableIntRange;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.bag.impl.CurveBag;
import nars.bag.leak.Leak;
import nars.budget.BLink;
import nars.budget.Budget;
import nars.budget.BudgetMerge;
import nars.budget.RawBLink;
import nars.concept.AtomConcept;
import nars.concept.CompoundConcept;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.table.BeliefTable;
import nars.table.QuestionTable;
import nars.task.GeneratedTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.container.TermContainer;
import nars.term.subst.Unify;
import nars.truth.Truth;
import nars.truth.TruthDelta;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static nars.Op.BELIEF;
import static nars.bag.impl.CurveBag.power4BagCurve;
import static nars.term.Terms.compoundOrNull;
import static nars.time.Tense.ETERNAL;

/**
 * compound<->dynamic atom abbreviation.
 *
 * @param S serial term type
 */
public class Abbreviation/*<S extends Term>*/ extends Leak<Compound, BLink<Compound>> {


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
        super(new CurveBag(capacity,
                new CurveBag.NormalizedSampler(power4BagCurve, n.random),
                BudgetMerge.plusBlend, new ConcurrentHashMap()), selectionRate, n);

        this.nar = n;
        this.termPrefix = termPrefix;
        this.setCapacity(capacity);
        this.abbreviationConfidence =
                new MutableFloat(nar.confidenceDefault(BELIEF));
        //new MutableFloat(1f - nar.truthResolution.floatValue());
        //new MutableFloat(nar.confidenceDefault(Symbols.BELIEF));
        volume = new MutableIntRange(volMin, volMax);
    }

    @Nullable
    @Override
    protected void in(@NotNull Task task, @NotNull Consumer<BLink<Compound>> each) {

        if (task instanceof AbbreviationTask)
            return;

        Budget b = task.budget().clone();
        if (b != null)
            input(b, each, task.term(), 1f);
    }

    private void input(@NotNull Budget b, @NotNull Consumer<BLink<Compound>> each, @NotNull Compound t, float scale) {
        int vol = t.volume();
        if (vol < volume.lo())
            return;

        if (vol <= volume.hi()) {
            if (t.vars() == 0 && !t.hasTemporal()) {
                Concept abbreviable = (Concept) nar.concept(t);
                if ((abbreviable == null) ||
                        !(abbreviable instanceof PermanentConcept) &&
                                abbreviable.get(Abbreviation.class) == null) {

                    each.accept(new RawBLink<Compound>(t, b));
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
    protected float onOut(BLink<Compound> b) {

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


    protected boolean abbreviate(@NotNull Compound abbreviated, @NotNull Budget b) {

        String id;
//            id = newCanonicalTerm(abbreviated);
//            if (id == null)
        id = newSerialTerm();


        Compound abbreviation = newRelation(abbreviated, id);

        Term[] aa;
        if (abbreviation != null) {

            @Nullable Concept a = nar.concept(abbreviated);
            if (a != null) {

                if (a.putIfAbsent(Abbreviation.class, id) == null) {

                    Concept alias = aliasConcept ? nar.on(AliasConcept.get(id, abbreviated, nar, abbreviation)) : null;

                    Termed aliasTerm = alias != null ? alias : $.the(id);

                    //if (abbreviation != null) {

                    //logger.info("{} <=== {}", alias, abbreviatedTerm);
                    Compound abbreviatedTerm = abbreviated.term();

                    Task abbreviationTask = new AbbreviationTask(
                            abbreviation, BELIEF, $.t(1f, abbreviationConfidence.floatValue()),
                            nar.time(), ETERNAL, ETERNAL,
                            new long[]{nar.time.nextStamp()}, abbreviatedTerm, aliasTerm
                    );
                    abbreviationTask.log("Abbreviate");
                    abbreviationTask.setBudget(b);

                    nar.input(abbreviationTask);
                    logger.info("{}", abbreviationTask);
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
                $.sim(abbreviated, $.the(id))
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

    /**
     * the proxy concepts present a bidirectional facade between a referenced and an alias term (alias term can be just a serial # atom wrapped in a product).
     * <p>
     * it replaces the index entry for the referenced with itself and also adds itself so from its start it intercepts all references to itself or the aliased (abbreviated) term whether this occurrs at the top level or as a subterm in another term (or as a subterm in another abbreviation, etc..)
     * <p>
     * the index is usually a weakmap or equivalent in which abbreviations can be forgotten as well as any other concept.
     * <p>
     * seen from a superterm containing one, it appears as a simple volume=2 concept meanwhile it could be aliasing a concept much larger than it. common "phrase" concepts with a volume >> 2 are good candidates for abbreviation. but when printed, the default toString() method is proxied so it will automatically decompress on output (or other serialization).
     */
    public static final class AliasConcept extends AtomConcept {

        @NotNull
        public final CompoundConcept abbr;
        private TermContainer templates;

        static public AliasConcept get(@NotNull String compressed, @NotNull Compound decompressed, @NotNull NAR nar, @NotNull Term... additionalTerms) {
            Concept c = nar.concept(decompressed, true);
            if (c != null) {
                AliasConcept a = new AliasConcept(compressed, (CompoundConcept) c, nar, additionalTerms);
                return a;
            }
            return null;
        }

        AliasConcept(@NotNull String abbreviation, CompoundConcept abbr, @NotNull NAR nar, @NotNull Term... additionalTerms) {
            super(abbreviation, Op.ATOM, abbr.termlinks(), abbr.tasklinks());

            this.abbr = abbr;

//            Term[] tl = ArrayUtils.add(abbreviated.templates().terms(), abbreviated.term());
//            if (additionalTerms.length > 0)
//                tl = ArrayUtils.addAll(tl, additionalTerms);
//            this.templates = TermVector.the(tl);

            //rewriteLinks(nar);
        }

//
//        /**
//         * rewrite termlinks and tasklinks which contain the abbreviated term...
//         * (but are not equal to since tasks can not have atom content)
//         * ...replacing it with this alias
//         */
//        private void rewriteLinks(@NotNull NAR nar) {
//            Term that = abbr.term();
//            termlinks().compute(existingLink -> {
//                Term x = existingLink.get();
//                Term y = nar.concepts.replace(x, that, this);
//                return (y != null && y != x && y != Term.False) ?
//                        termlinks().newLink(y, existingLink) :
//                        existingLink;
//            });
//            tasklinks().compute(existingLink -> {
//                Task xt = existingLink.get();
//                Term x = xt.term();
//
//                if (!x.equals(that) && !x.hasTemporal()) {
//                    Term y = $.terms.replace(x, that, this);
//                    if (y != x && y instanceof Compound) {
//                        Task yt = MutableTask.clone(xt, (Compound) y, nar);
//                        if (yt != null)
//                            return termlinks().newLink(yt, existingLink);
//                    }
//                }
//
//                return existingLink;
//
//            });
//        }


        @Override
        public void delete(NAR nar) {
            abbr.delete(nar);
            super.delete(nar);
        }


        @Override
        public boolean unify(@NotNull Term y, @NotNull Unify subst) {

            Term tt = abbr.term();
            if (y instanceof AliasConcept) {
                //try to unify with y's abbreviated
//                if (tt.unify( ((AliasConcept)y).abbr.term(), subst ))
//                    return true;

                //if this is constant (no variables) then all it needs is equality test
                if (tt.equals(((AliasConcept) y).abbr.term()))
                    return true;
            }

            //try to unify with 'y'
            return tt.equals(y) || tt.unify(y, subst);
        }

//        @Override
//        public boolean equals(Object u) {
//            return super.equals(u);
//        }


//        @Override
//        public final Activation process(@NotNull Task input, NAR nar) {
//            return abbr.process(input, nar);
//        }

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

        @NotNull
        private final Compound abbreviated;
        @NotNull
        private final Term alias;

        public AbbreviationTask(Compound term, byte punc, Truth truth, long creation, long start, long end, long[] evidence, @NotNull Compound abbreviated, @NotNull Termed alias) {
            super(term, punc, truth, creation, start, end, evidence);
            this.abbreviated = abbreviated;
            this.alias = alias.term();
        }


        @Override
        public void feedback(TruthDelta delta, float deltaConfidence, float deltaSatisfaction, @NotNull NAR nar) {

            super.feedback(delta, deltaConfidence, deltaSatisfaction, nar);


            if (deltaConfidence == deltaConfidence /* wasn't deleted, even for questions */ && !isDeleted()) {
                @Nullable Concept c = concept(nar);
                c.put(Abbreviation.class, alias);

            }
        }
    }
}
