package nars.conceptualize;

import jcog.bag.Bag;
import jcog.bag.impl.CurveBag;
import jcog.map.SynchronizedHashMap;
import jcog.map.SynchronizedUnifiedMap;
import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.concept.AtomConcept;
import nars.concept.CompoundConcept;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.concept.dynamic.DynamicConcept;
import nars.concept.dynamic.DynamicTruthModel;
import nars.conceptualize.state.ConceptState;
import nars.conceptualize.state.DefaultConceptState;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import static jcog.pri.op.PriMerge.plus;
import static nars.Op.*;
import static nars.term.Terms.compoundOrNull;
import static nars.time.Tense.DTERNAL;

//import org.eclipse.collections.impl.map.mutable.ConcurrentHashMapUnsafe;

/**
 * Created by me on 2/24/16.
 */
public class DefaultConceptBuilder implements ConceptBuilder {

    public static final PriMerge DEFAULT_BLEND = plus;

    public DefaultConceptBuilder() {
        this(
            new DefaultConceptState("sleep", 64, 64, 3, 24, 12),
            new DefaultConceptState("awake", 64, 64, 3, 24, 12)
        );
    }

    public DefaultConceptBuilder(ConceptState sleep, ConceptState awake) {
        this.sleep = sleep;
        this.init = awake;
        this.awake = awake;
    }


//    @NotNull
//    public <X> Bag<X> newHijackBag(int reprobes) {
//        return new HijackBag<>(1, reprobes, mergeDefault, nar.random);
//    }

    @NotNull
    @Deprecated public <X> Bag<X,PriReference<X>> newBag(@NotNull Map m, PriMerge blend) {
        return new CurveBag<>(8, blend, m);
    }


    @NotNull
    private final ConceptState init;
    @NotNull
    private final ConceptState awake;
    @NotNull
    private final ConceptState sleep;
    private NAR nar;



//    final Function<Variable, VariableConcept> varBuilder =
//            (Variable v) -> new VariableConcept(v);

    public <X> X withBags(Term t, BiFunction<Bag<Term,PriReference<Term>>,Bag<Task,PriReference<Task>>,X> f) {
        Map sharedMap = newBagMap(t.volume());
        @NotNull Bag<Term,PriReference<Term>> termbag = newBag(sharedMap, DEFAULT_BLEND);
        @NotNull Bag<Task,PriReference<Task>> taskbag = newBag(sharedMap, DEFAULT_BLEND);


//        @NotNull Bag<Term,BLink<Term>> termbag = new BLinkHijackBag<>(3, BudgetMerge.maxBlend, nar.random);
//        @NotNull Bag<Task,BLink<Task>> taskbag = new BLinkHijackBag<>(3, BudgetMerge.maxBlend, nar.random);

        return f.apply(termbag, taskbag);
    }

    @Nullable
    final Concept newConcept(@NotNull Compound t) {


        if (t.volume() > nar.termVolumeMax.intValue()) {
//            if (Param.DEBUG)
//                throw new UnsupportedOperationException("tried to conceptualize concept too large");
            return null;
        }

        @NotNull Compound tt = t;
        return withBags(tt, (termbag, taskbag) -> {
            boolean validForTask = Task.taskContentValid(t, (byte)0, null /*nar -- checked above */, true);
            if (!validForTask) {
                return newCompound(tt, termbag, taskbag);
            } else {
                return newTask(tt, termbag, taskbag);
            }
        });
    }

    /** for fragmentary concepts which by themselves or due to being un-normalizable,
     * can not be the content of Tasks yet may still exist as concepts
     */
    private CompoundConcept newCompound(@NotNull Compound t, Bag<Term, PriReference<Term>> termbag, Bag<Task, PriReference<Task>> taskbag) {
        return new CompoundConcept(t, termbag, taskbag, nar);
    }

    private TaskConcept newTask(@NotNull Compound t, Bag<Term, PriReference<Term>> termbag, Bag<Task, PriReference<Task>> taskbag) {
        DynamicTruthModel dmt = null;

        switch (t.op()) {

            case INH:

                Term subj = t.sub(0);
                Term pred = t.sub(1);

                Op so = subj.op();
                Op po = pred.op();

                if (dmt == null && (po.atomic || po.image || po == PROD)) {
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
                                if ((x[i] = nar.terms.the(INH, DTERNAL, csubj.sub(i), pred)) == null) {
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
                    } else if (po.image) {
                        Compound img = (Compound) pred;
                        Term[] ee = new Term[img.size()];

                        int relation = img.dt();
                        int s = ee.length;
                        for (int j = 1, i = 0; i < s; ) {
                            if (j == relation)
                                ee[i++] = subj;
                            if (i < s)
                                ee[i++] = img.sub(j++);
                        }
                        Compound b = compoundOrNull(nar.terms.the(INH, DTERNAL, $.p(ee), img.sub(0)));
                        if (b != null)
                            dmt = new DynamicTruthModel.Identity(t, b);
                    }

                }

                if (dmt == null && (so.atomic || so.image || so == PROD)) {
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
                                if ((x[i] = nar.terms.the(INH, DTERNAL, subj, cpred.sub(i))) == null) {
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
                    } else if (so.image) {
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
                        Compound b = compoundOrNull(nar.terms.the(INH, DTERNAL, img.sub(0), $.p(ee)));
                        if (b != null)
                            dmt = new DynamicTruthModel.Identity(t, b);
                    }

                }

                break;

            case CONJ:
                //allow variables onlyif they are not themselves direct subterms of this
                if (validUnwrappableSubterms(t.subterms())) {
                    dmt = DynamicTruthModel.Intersection;
                }
                break;

            case NEG:
                throw new RuntimeException("negation terms must not be conceptualized");
        }

        return
                dmt != null ?
                        new DynamicConcept(t, dmt, null, termbag, taskbag, nar) :
                        new TaskConcept(t, termbag, taskbag, nar)
                ;
    }

    private static boolean validUnwrappableSubterms(@NotNull TermContainer subterms) {
        return !subterms.OR(x -> x instanceof Variable);
    }

    @Override public void start(@NotNull NAR nar) {
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

                return withBags(term, (termbag, taskbag) ->
                        new AtomConcept((Atom)term, termbag, taskbag));

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
        float loadFactor = 0.9f;

        if (nar.exe.concurrent()) {
//            //return new ConcurrentHashMap(defaultInitialCap, 1f);
//            //return new NonBlockingHashMap(cap);
//            return new org.eclipse.collections.impl.map.mutable.ConcurrentHashMapUnsafe<>();
//            //ConcurrentHashMapUnsafe(cap);
//        } else {
//            return new HashMap(defaultInitialCap, 1f);
            if (volume < 16) {
                return new ConcurrentHashMap(8, loadFactor);
            } else if (volume < 32) {
                return new SynchronizedHashMap(2, loadFactor);
                //return new TrieMap();
            } else {
                return new SynchronizedUnifiedMap(2, loadFactor);
            }
        } else {
            //return new UnifiedMap(0, loadFactor);
            return new HashMap(2, loadFactor);
        }

    }

}
