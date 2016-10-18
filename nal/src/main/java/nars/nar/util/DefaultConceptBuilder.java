package nars.nar.util;

import nars.NAR;
import nars.Op;
import nars.Task;
import nars.bag.Bag;
import nars.bag.impl.CurveBag;
import nars.bag.impl.experimental.HijackBag;
import nars.budget.merge.BudgetMerge;
import nars.budget.policy.ConceptPolicy;
import nars.budget.policy.DefaultConceptPolicy;
import nars.concept.*;
import nars.concept.util.ConceptBuilder;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.obj.Termject;
import nars.term.obj.TermjectConcept;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

//import org.eclipse.collections.impl.map.mutable.ConcurrentHashMapUnsafe;

/**
 * Created by me on 2/24/16.
 */
 public class DefaultConceptBuilder implements ConceptBuilder {

    public DefaultConceptBuilder() {

        this.sleep = new DefaultConceptPolicy("sleep", 6, 6, 1, 8, 4);
        this.init = sleep;

        this.awake = new DefaultConceptPolicy("awake", 12, 12, 3, 24, 16);
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
                        Map sharedMap = newBagMap();
                        return new AtomConcept(a, newCurveBag(sharedMap), newCurveBag(sharedMap));
                }

            };

    public <X> Bag<X> newHijackBag(int reprobes) {
        return new HijackBag<>(1, reprobes, mergeDefault, nar.random);
    }

    public <X> Bag<X> newCurveBag(Map m) {
        return new CurveBag<>(defaultCurveSampler, BudgetMerge.plusBlend, m);
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
    final Concept newConcept(@NotNull Compound t){

//        Map map1 = newBagMap(DEFAULT_CONCEPT_LINK_MAP_CAPACITY);
//        Map map2 =
//                map1; //shared
//                //newBagMap(DEFAULT_CONCEPT_LINK_MAP_CAPACITY);

        Map sharedMap = newBagMap();
        @NotNull Bag<Term> termbag = newCurveBag(sharedMap);
        @NotNull Bag<Task> taskbag = newCurveBag(sharedMap);

        boolean dynamic = false;

        switch (t.op()) {

            case INH:
                if (Op.isOperation(t))
                    return new OperationConcept(t, termbag, taskbag, nar);
                break;

            case CONJ:
                if (t.vars() == 0)
                    dynamic = true;
                break;

            case NEG:
                throw new RuntimeException("negation terms must not be conceptualized");

        }

        return
            (!dynamic) ?
                new CompoundConcept<>(t, termbag, taskbag, nar)
                    :
                new DynamicCompoundConcept(t, termbag, taskbag, nar)
        ;

    }

    /** use average blend so that reactivations of adjusted task budgets can be applied repeatedly without inflating the link budgets they activate; see CompoundConcept.process */
    private final BudgetMerge mergeDefault = BudgetMerge
            .plusBlend;
            //.avgBlend;


    final static Logger logger = LoggerFactory.getLogger(DefaultConceptBuilder.class);



    @NotNull
    public CurveBag.CurveSampler defaultCurveSampler; //shared




    @Override
    public void start(NAR nar) {

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

                Map sharedMap = newBagMap();
                result = new TermjectConcept((Termject)term, newCurveBag(sharedMap), newCurveBag(sharedMap));
            }

            if (term instanceof Variable) {
                //final int s = this.serial;
                //serial++;
                //result = varBuilder.apply((Variable) term);
                return term;
            }
            else if (term instanceof Atomic) {
                result = atomBuilder.apply((Atomic) term);
            }

        }
        if (result == null) {
            throw new UnsupportedOperationException(
                    "unknown conceptualization method for term \"" +
                            term + "\" of class: "  + term.getClass()
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

    public Map newBagMap() {
        int defaultInitialCap = 4;
        if (nar.exe.concurrent()) {
            return new ConcurrentHashMap(defaultInitialCap);
            //return new NonBlockingHashMap(cap);
            //return new org.eclipse.collections.impl.map.mutable.ConcurrentHashMap<>();
            //ConcurrentHashMapUnsafe(cap);
        } else {
            return new HashMap(defaultInitialCap);
        }
    }
}
