package nars.nar.util;

import nars.Op;
import nars.bag.Bag;
import nars.bag.impl.CurveBag;
import nars.budget.merge.BudgetMerge;
import nars.budget.policy.ConceptPolicy;
import nars.budget.policy.DefaultConceptPolicy;
import nars.concept.AtomConcept;
import nars.concept.CompoundConcept;
import nars.concept.Concept;
import nars.concept.OperationConcept;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.term.variable.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

/**
 * Created by me on 2/24/16.
 */
public class DefaultConceptBuilder implements Concept.ConceptBuilder {

    final Function<Atom, AtomConcept> atomBuilder =
            (Atom a) -> {
                Map map = new HashMap();
                return new AtomConcept(a, termbag(map), taskbag(map));
            };

    private final ConceptPolicy conceptInitialize, conceptActivate;


    //private static volatile int serial = 0;

//    final Function<Variable, VariableConcept> varBuilder =
//            (Variable v) -> new VariableConcept(v);

    @Nullable
    final Concept newConcept(@NotNull Compound t){

        Map map = new HashMap();
        @NotNull Bag<Term> termbag = termbag(map);
        @NotNull Bag<Task> taskbag = taskbag(map);

        switch (t.op()) {
            case INH:
                if (Op.isOperation(t))
                    return new OperationConcept(t, termbag, taskbag);
                break;

            case NEG:
                throw new RuntimeException("building a negated concept should not be attempted");
                //return t; //return new NegationConcept(t, termbag, taskbag);

        }

        return new CompoundConcept(t, termbag, taskbag);
    }



    //return (!(t instanceof Space)) ?
    //new SpaceConcept((Space) t, taskLinks, termLinks);

    @NotNull
    public Bag<Task> taskbag(Map map) {
        return new CurveBag<>(1, defaultCurveSampler, mergeDefault, map);
    }


    @NotNull
    public Bag<Term> termbag(Map map) {
        return new CurveBag<>(1, defaultCurveSampler, mergeDefault, map);
    }


    private final BudgetMerge mergeDefault = BudgetMerge.plusDQBlend;




    final static Logger logger = LoggerFactory.getLogger(DefaultConceptBuilder.class);


    @NotNull
    public final Random rng; //shared
    @NotNull
    public final CurveBag.CurveSampler defaultCurveSampler; //shared


    public DefaultConceptBuilder(@NotNull Random r) {
        this.rng = r;

        this.conceptInitialize = new DefaultConceptPolicy(10, 8, 1, 8, 4);
        this.conceptActivate = new DefaultConceptPolicy(12, 10, 4, 24, 12);

        this.defaultCurveSampler =
                new CurveBag.NormalizedSampler(
                //new CurveBag.DirectSampler(
                    //CurveBag.linearBagCurve,
                    CurveBag.power2BagCurve,
                    //CurveBag.power6BagCurve,
                    rng);
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
            result = newConcept(  (Compound) term );
        } else {

            if (term instanceof Variable) {
                //final int s = this.serial;
                //serial++;
                //result = varBuilder.apply((Variable) term);
                return term;
            } else if (term instanceof Atom) {
                result = atomBuilder.apply((Atom) term);
            }

        }
        if (result == null) {
            logger.error("unknown conceptualization method for term: {} of class {} ", term, term.getClass());
            throw new UnsupportedOperationException();
        }


        //logger.trace("{} conceptualized to {}", term, result);

        return result;

    }

    @Override
    public ConceptPolicy initialized() {
        return conceptInitialize;
    }
    @Override
    public ConceptPolicy activated() {
        return conceptActivate;
    }

}
