package nars.conceptualize;

import jcog.bag.Bag;
import jcog.bag.impl.CurveBag;
import jcog.bag.impl.hijack.DefaultHijackBag;
import jcog.pri.PriReference;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.concept.BaseConcept;
import nars.concept.Concept;
import nars.concept.dynamic.DynamicBeliefTable;
import nars.concept.dynamic.DynamicConcept;
import nars.concept.dynamic.DynamicTruthModel;
import nars.conceptualize.state.ConceptState;
import nars.conceptualize.state.DefaultConceptState;
import nars.table.*;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.term.container.TermContainer;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;

//import org.eclipse.collections.impl.map.mutable.ConcurrentHashMapUnsafe;

/**
 * Created by me on 2/24/16.
 */
public class DefaultConceptBuilder implements ConceptBuilder {

    public DefaultConceptBuilder() {
        this(
                new DefaultConceptState("sleep", 48, 48, 8, 24, 24),
                new DefaultConceptState("awake", 48, 48, 8, 24, 24)
        );
    }

    public DefaultConceptBuilder(ConceptState sleep, ConceptState awake) {
        this.sleep = sleep;
        this.init = awake;
        this.awake = awake;
    }


    @NotNull
    private final ConceptState init;
    @NotNull
    private final ConceptState awake;
    @NotNull
    private final ConceptState sleep;
    private NAR nar;

    @Override
    public Bag[] newLinkBags(Term t) {
        int v = t.volume();
        if (v < 16) {
            Map sharedMap = newBagMap(v);
            Random rng = nar.random();
            @NotNull Bag<Term, PriReference<Term>> termbag =
                    new CurveBag<>(Param.termlinkMerge, sharedMap, rng, 0);
            @NotNull Bag<Task, PriReference<Task>> taskbag =
                    new CurveBag<>(Param.tasklinkMerge, sharedMap, rng, 0);
            return new Bag[]{termbag, taskbag};
        } else {
            return new Bag[]{
                    new DefaultHijackBag<>(Param.termlinkMerge, 4),
                    new DefaultHijackBag<>(Param.tasklinkMerge, 4)
            };
        }


//        public <X> Bag<X, PriReference<X>> newBag(@NotNull Map m, PriMerge blend) {
//            return new DefaultHijackBag<>(blend, reprobes);
//        }
//        public <X> X withBags(Term t, BiFunction<Bag<Term, PriReference<Term>>, Bag<Task, PriReference<Task>>, X> f) {
//
//            Bag<Term, PriReference<Term>> termlink =
//                    new DefaultHijackBag<>(DefaultConceptBuilder.DEFAULT_BLEND, reprobes);
//            //BloomBag<Term> termlink = new BloomBag<Term>(32, IO::termToBytes);
//
//            Bag<Task, PriReference<Task>> tasklink = new DefaultHijackBag<>(DefaultConceptBuilder.DEFAULT_BLEND, reprobes);
//
//            return f.apply(termlink, tasklink);
//        }

    }

    @Nullable
    final Concept newConcept(@NotNull Compound t) {


        if (t.volume() > nar.termVolumeMax.intValue()) {
//            if (Param.DEBUG)
//                throw new UnsupportedOperationException("tried to conceptualize concept too large");
            return null;
        }

        @NotNull Compound tt = t;
        boolean validForTask = Task.taskContentValid(t, (byte) 0, null /*nar -- checked above */, true);
        if (!validForTask) {
            return newCompound(tt);
        } else {
            return newTask(tt);
        }
    }

    /**
     * for fragmentary concepts which by themselves or due to being un-normalizable,
     * can not be the content of Tasks yet may still exist as concepts
     */
    private BaseConcept newCompound(@NotNull Compound t) {
        return new BaseConcept(t, this);
    }

    private BaseConcept newTask(@NotNull Compound t) {
        DynamicTruthModel dmt = null;

        switch (t.op()) {

            case INH:

                Term subj = t.sub(0);
                Term pred = t.sub(1);

                Op so = subj.op();
                Op po = pred.op();

                if (dmt == null && (po.atomic || po == PROD)) {
                    if ((so == Op.SECTi) || (so == Op.SECTe) || (so == Op.DIFFi)) {
                        //(P --> M), (S --> M), notSet(S), notSet(P), neqCom(S,P) |- ((S | P) --> M), (Belief:Intersection)
                        //(P --> M), (S --> M), notSet(S), notSet(P), neqCom(S,P) |- ((S & P) --> M), (Belief:Union)
                        //(P --> M), (S --> M), notSet(S), notSet(P), neqCom(S,P) |- ((P ~ S) --> M), (Belief:Difference)
                        Compound csubj = (Compound) subj;
                        if (validUnwrappableSubterms(csubj.subterms())) {
                            int s = csubj.size();
                            Term[] x = new Term[s];
                            boolean valid = true;
                            for (int i = 0; i < s; i++) {
                                if ((x[i] = INH.the(DTERNAL, csubj.sub(i), pred)) == null) {
                                    valid = false;
                                    break;
                                }
                            }

                            if (valid) {
                                switch (so) {
                                    case SECTi:
                                        dmt = new DynamicTruthModel.Intersection(x);
                                        break;
                                    case SECTe:
                                        dmt = new DynamicTruthModel.Union(x);
                                        break;
                                    case DIFFi:
                                        dmt = new DynamicTruthModel.Difference(x[0], x[1]);
                                        break;
                                }
                            }
                        }
                    } /*else if (po.image) {
                        Compound img = (Compound) pred;
                        Term[] ee;

                        int relation = img.dt();
                        if (relation != DTERNAL) {
                            int s = img.size();
                            ee = new Term[s];

                            for (int j = 1, i = 0; i < s; ) {
                                if (j == relation)
                                    ee[i++] = subj;
                                if (i < s)
                                    ee[i++] = img.sub(j++);
                            }
                        } else {
                            ee = t.toArray();
                        }
                        Compound b = compoundOrNull(INH.the(DTERNAL, $.p(ee), img.sub(0)));
                        if (b != null)
                            dmt = new DynamicTruthModel.Identity(t, b);
                    }*/

                }

                if (dmt == null && (so.atomic || so == PROD)) {
                    if ((po == Op.SECTi) || (po == Op.SECTe) || (po == DIFFe)) {
                        //(M --> P), (M --> S), notSet(S), notSet(P), neqCom(S,P) |- (M --> (P & S)), (Belief:Intersection)
                        //(M --> P), (M --> S), notSet(S), notSet(P), neqCom(S,P) |- (M --> (P | S)), (Belief:Union)
                        //(M --> P), (M --> S), notSet(S), notSet(P), neqCom(S,P) |- (M --> (P - S)), (Belief:Difference)
                        Compound cpred = (Compound) pred;
                        if (validUnwrappableSubterms(cpred.subterms())) {
                            int s = cpred.size();
                            Term[] x = new Term[s];
                            boolean valid = true;
                            for (int i = 0; i < s; i++) {
                                if ((x[i] = INH.the(DTERNAL, subj, cpred.sub(i))) == null) {
                                    valid = false;
                                    break;
                                }
                            }

                            if (valid) {
                                switch (po) {
                                    case SECTi:
                                        dmt = new DynamicTruthModel.Union(x);
                                        break;
                                    case SECTe:
                                        dmt = new DynamicTruthModel.Intersection(x);
                                        break;
                                    case DIFFe:
                                        dmt = new DynamicTruthModel.Difference(x[0], x[1]);
                                        break;
                                }
                            }
                        }
                    } /*else if (so.image) {
                        Compound img = (Compound) subj;
                        Term[] ee = new Term[img.size()];

                        int relation = img.dt();
                        int s = ee.length;
                        for (int j = 1, i = 0; i < s; ) {
                            if (j == relation)
                                ee[i++] = pred;
                            if (i < s)
                                ee[i++] = img.sub(j++);
                        }
                        Compound b = compoundOrNull(INH.the(DTERNAL, img.sub(0), $.p(ee)));
                        if (b != null)
                            dmt = new DynamicTruthModel.Identity(t, b);
                    }*/

                }

                break;

            case CONJ:
                //allow variables onlyif they are not themselves direct subterms of this
                if (validUnwrappableSubterms(t.subterms())) {
                    dmt = new DynamicTruthModel.DynamicIntersection(t);
                }
                break;

            case NEG:
                throw new RuntimeException("negation terms must not be conceptualized");
        }

        if (dmt != null) {

            BeliefTable beliefs = dmt != null ?
                    new DynamicBeliefTable(t, newTemporalBeliefTable(t), dmt, true) :
                    newBeliefTable(t, true);

            BeliefTable goals = dmt != null ?
                    new DynamicBeliefTable(t, newTemporalBeliefTable(t), dmt, true) :
                    newBeliefTable(t, false);

            return new DynamicConcept(t, beliefs, goals, nar);
        } else {
            return new BaseConcept(t, this);
        }
    }

    @Override
    public BeliefTable newBeliefTable(Term c, boolean beliefOrGoal) {
        //TemporalBeliefTable newTemporalTable(final int tCap, NAR nar) {
        //return new HijackTemporalBeliefTable(tCap);
        //return new RTreeBeliefTable(tCap);
        DefaultBeliefTable b = new DefaultBeliefTable(newTemporalBeliefTable(c));
        return b;
    }

    @Override
    public TemporalBeliefTable newTemporalBeliefTable(Term c) {
        if (c.complexity() < 16) {
            return new RTreeBeliefTable();
        } else {
            return new HijackTemporalBeliefTable();
        }
    }


    @Override
    public QuestionTable newQuestionTable() {
        return new HijackQuestionTable(0, 2);
    }

    private static boolean validUnwrappableSubterms(@NotNull TermContainer subterms) {
        return !subterms.OR(x -> x instanceof Variable);
    }

    @Override
    public void start(@NotNull NAR nar) {
        this.nar = nar;
    }


    @Override
    @Nullable
    public Termed apply(@NotNull Term term) {

        //already a concept, assume it is from here
        if (term instanceof Concept) {
            return term;
        }

        Concept result = null;


        if (term instanceof Compound) {

            result = newConcept((Compound) term);

        } else {


            if (term instanceof Variable) {
                //final int s = this.serial;
                //serial++;
                //result = varBuilder.apply((Variable) term);
                return term;
            } else if (term instanceof Atom) {

                return
                        new BaseConcept(term, this);

//                result = new AtomConcept((Atomic)term,
//                        new HijackBag<>(32, 2, BudgetMerge.maxBlend, nar.random),
//                        new HijackBag<>(32, 2, BudgetMerge.maxBlend, nar.random)
//                );

            }

        }
        if (result == null) {
            /*throw new UnsupportedOperationException(
                    "unknown conceptualization method for term \"" +
                            term + "\" of class: " + term.getClass()
            );*/
            return null;
        }


        //logger.trace("{} conceptualized to {}", term, result);
        return result;

    }

    @NotNull
    @Override
    public ConceptState init() {
        return init;
    }

    @NotNull
    @Override
    public ConceptState awake() {
        return awake;
    }

    @NotNull
    @Override
    public ConceptState sleep() {
        return sleep;
    }

    @NotNull
    public Map newBagMap(int volume) {
        //int defaultInitialCap = 0;
        float loadFactor = 0.75f;

        if (concurrent()) {
//            //return new ConcurrentHashMap(defaultInitialCap, 1f);
//            //return new NonBlockingHashMap(cap);
//            return new org.eclipse.collections.impl.map.mutable.ConcurrentHashMapUnsafe<>();
//            //ConcurrentHashMapUnsafe(cap);
//        } else {
//            return new HashMap(defaultInitialCap, 1f);
            //   if (volume < 16) {
            return new ConcurrentHashMap(0, loadFactor);
            //return new ConcurrentHashMapUnsafe(0);
//            } else if (volume < 32) {
//                return new SynchronizedHashMap(0, loadFactor);
//                //return new TrieMap();
//            } else {
//                return new SynchronizedUnifiedMap(0, loadFactor);
//            }
        } else {
            //return new UnifiedMap(0, loadFactor);
            return new HashMap(0, loadFactor);
        }

    }

    public boolean concurrent() {
        return nar.exe.concurrent();
    }

}
