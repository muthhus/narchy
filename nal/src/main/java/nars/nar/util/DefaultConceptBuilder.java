package nars.nar.util;

import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.bag.Bag;
import nars.bag.impl.CurveBag;
import nars.bag.impl.experimental.HijackBag;
import nars.budget.merge.BudgetMerge;
import nars.budget.policy.ConceptPolicy;
import nars.budget.policy.DefaultConceptPolicy;
import nars.concept.AtomConcept;
import nars.concept.CompoundConcept;
import nars.concept.Concept;
import nars.concept.dynamic.DynamicConcept;
import nars.concept.dynamic.DynamicTruthModel;
import nars.concept.util.ConceptBuilder;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.obj.Termject;
import nars.term.obj.TermjectConcept;
import nars.term.var.Variable;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

//import org.eclipse.collections.impl.map.mutable.ConcurrentHashMapUnsafe;

/**
 * Created by me on 2/24/16.
 */
public class DefaultConceptBuilder implements ConceptBuilder {

    public DefaultConceptBuilder() {

        this.sleep = new DefaultConceptPolicy("sleep", 6, 6, 1, 16, 8);
        this.init = sleep;

        this.awake = new DefaultConceptPolicy("awake", 12, 12, 3, 32, 16);
    }

//    private static final int DEFAULT_ATOM_LINK_MAP_CAPACITY = 128;
//    private static final int DEFAULT_CONCEPT_LINK_MAP_CAPACITY = 32;

    final Function<Atomic, AtomConcept> atomBuilder =
            (Atomic a) -> {
//                Map map1 = newBagMap(DEFAULT_ATOM_LINK_MAP_CAPACITY);
//                Map map2 =
//                        map1; //shared
//                        //newBagMap(DEFAULT_ATOM_LINK_MAP_CAPACITY);

                switch (a.op()) {
                    default:
                        Map sharedMap = newBagMap(1);
                        return new AtomConcept(a, newCurveBag(sharedMap), newCurveBag(sharedMap));
                }

            };

    @NotNull
    public <X> Bag<X> newHijackBag(int reprobes) {
        return new HijackBag<>(1, reprobes, mergeDefault, nar.random);
    }

    @NotNull
    public <X> Bag<X> newCurveBag(@NotNull Map m) {
        return new CurveBag<>(8, defaultCurveSampler, BudgetMerge.plusBlend, m);
    }


    @NotNull
    private final ConceptPolicy init;
    @NotNull
    private final ConceptPolicy awake;
    @NotNull
    private final ConceptPolicy sleep;
    private NAR nar;


    //private static volatile int serial = 0;

//    final Function<Variable, VariableConcept> varBuilder =
//            (Variable v) -> new VariableConcept(v);

    @Nullable
    final Concept newConcept(@NotNull Compound t) {

//        Map map1 = newBagMap(DEFAULT_CONCEPT_LINK_MAP_CAPACITY);
//        Map map2 =
//                map1; //shared
//                //newBagMap(DEFAULT_CONCEPT_LINK_MAP_CAPACITY);

        Map sharedMap = newBagMap(t.volume());
        @NotNull Bag<Term> termbag = newCurveBag(sharedMap);
        @NotNull Bag<Task> taskbag = newCurveBag(sharedMap);

        DynamicTruthModel dmt = null;

        switch (t.op()) {

            case INH:
//                if (Op.isOperation(t))
//                    return new OperationConcept(t, termbag, taskbag, nar);


                Term subj = t.term(0);
                Term pred = t.term(1);

                Op so = subj.op();
                Op po = pred.op();
                if (po.atomic || po.image || po == so.PROD) {
                    if ((so == Op.SECTi) || (so == Op.SECTe)) {
                        //(P --> M), (S --> M), notSet(S), notSet(P), neqCom(S,P) |- ((S | P) --> M), (Belief:Intersection)
                        //(P --> M), (S --> M), notSet(S), notSet(P), neqCom(S,P) |- ((S & P) --> M), (Belief:Union)
                        Compound csubj = (Compound) subj;
                        int s = csubj.size();
                        Term[] x = new Term[s];
                        for (int i = 0; i < s; i++)
                            x[i] = $.inh(csubj.term(i), pred);

                        switch(so) {
                            case SECTi: dmt = new DynamicTruthModel.Intersection(x); break;
                            case SECTe: dmt = new DynamicTruthModel.Union(x); break;
                        }
                    }
                } else if (so.atomic || so.image || so == so.PROD) {
                    if ((so == Op.SECTi) || (so == Op.SECTe)) {
                        //(M --> P), (M --> S), notSet(S), notSet(P), neqCom(S,P) |- (M --> (P & S)), (Belief:Intersection)
                        //(M --> P), (M --> S), notSet(S), notSet(P), neqCom(S,P) |- (M --> (P | S)), (Belief:Union)                        Compound csubj = (Compound) subj;
                        Compound cpred = (Compound) pred;
                        int s = cpred.size();
                        Term[] x = new Term[s];
                        for (int i = 0; i < s; i++)
                            x[i] = $.inh(subj, cpred.term(i));

                        switch(so) {
                            case SECTi: dmt = new DynamicTruthModel.Union(x); break;
                            case SECTe: dmt = new DynamicTruthModel.Intersection(x); break;
                        }
                    }

                }

                break;

            case CONJ:
                //allow variables onlyif they are not themselves direct subterms of this
                boolean dynamicConj = true;
                for (Term x : t.terms()) {
                    if (x instanceof Variable) {
                        dynamicConj = false;
                        break;
                    }
                }
                if (dynamicConj) {
                    dmt = DynamicTruthModel.Intersection;
                }
                break;

            case NEG:
                throw new RuntimeException("negation terms must not be conceptualized");

        }


        return
                dmt != null ?
                        new DynamicConcept(t, dmt, dmt, termbag, taskbag, nar) :
                        new CompoundConcept<>(t, termbag, taskbag, nar)
                ;
    }


    /**
     * use average blend so that reactivations of adjusted task budgets can be applied repeatedly without inflating the link budgets they activate; see CompoundConcept.process
     */
    private final BudgetMerge mergeDefault = BudgetMerge
            .plusBlend;
    //.avgBlend;


    final static Logger logger = LoggerFactory.getLogger(DefaultConceptBuilder.class);


    @NotNull
    public CurveBag.CurveSampler defaultCurveSampler; //shared


    @Override
    public void start(@NotNull NAR nar) {

        this.nar = nar;

        this.defaultCurveSampler =
                //new CurveBag.DirectSampler(
                new CurveBag.NormalizedSampler(
                        //new CurveBag.DirectSampler(
                        //CurveBag.linearBagCurve,
                        CurveBag.power2BagCurve,
                        //CurveBag.power4BagCurve,
                        //CurveBag.power6BagCurve,
                        nar.random);
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

            if (term instanceof Termject) {
                //if (term.op() == INT || term.op() == INTRANGE) {
                //Map m = newBagMap(DEFAULT_ATOM_LINK_MAP_CAPACITY);

                Map sharedMap = newBagMap(term.volume());
                result = new TermjectConcept((Termject) term, newCurveBag(sharedMap), newCurveBag(sharedMap));
            }

            if (term instanceof Variable) {
                //final int s = this.serial;
                //serial++;
                //result = varBuilder.apply((Variable) term);
                return term;
            } else if (term instanceof Atomic) {
                result = atomBuilder.apply((Atomic) term);
            }

        }
        if (result == null) {
            throw new UnsupportedOperationException(
                    "unknown conceptualization method for term \"" +
                            term + "\" of class: " + term.getClass()
            );
        }


        //logger.trace("{} conceptualized to {}", term, result);
        return result;

    }

    @NotNull
    @Override
    public ConceptPolicy init() {
        return init;
    }

    @NotNull
    @Override
    public ConceptPolicy awake() {
        return awake;
    }

    @NotNull
    @Override
    public ConceptPolicy sleep() {
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
            if (volume < 3) {
                return new ConcurrentHashMap();
            } else {
                return new SynchronizedUnifiedMap(0, loadFactor);
            }
        } else {
            return new UnifiedMap(0, loadFactor);
        }

    }

    public static final class SynchronizedUnifiedMap<K, V> extends UnifiedMap<K, V> {

        public SynchronizedUnifiedMap(int cap, float loadFactor) {
            super(cap, loadFactor);
        }

        @Override
        public synchronized V remove(Object key) {
            return super.remove(key);
        }

        @Override
        public synchronized V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            return super.compute(key, remappingFunction);
        }
    }
}
