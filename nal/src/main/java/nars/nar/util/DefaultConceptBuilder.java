package nars.nar.util;

import nars.Op;
import nars.bag.Bag;
import nars.bag.impl.CurveBag;
import nars.budget.merge.BudgetMerge;
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

import java.util.Random;
import java.util.function.Function;

/**
 * Created by me on 2/24/16.
 */
public class DefaultConceptBuilder implements Concept.ConceptBuilder {

    final Function<Atom, AtomConcept> atomBuilder =
            (Atom a) -> new AtomConcept(a, termbag(), taskbag());


    //private static volatile int serial = 0;

//    final Function<Variable, VariableConcept> varBuilder =
//            (Variable v) -> new VariableConcept(v);

    @Nullable
    final Termed newConcept(Compound t){

        switch (t.op()) {
            case INH:
                if (Op.isOperation(t))
                    return new OperationConcept(t, termbag(), taskbag());
                break;

            case NEG:
                throw new RuntimeException("building a negated concept should not be attempted");
                //return t; //return new NegationConcept(t, termbag, taskbag);

        }

        return new CompoundConcept(t, termbag(), taskbag());
    }



    //return (!(t instanceof Space)) ?
    //new SpaceConcept((Space) t, taskLinks, termLinks);

    @NotNull
    @Override public Bag<Task> taskbag() {
        return new CurveBag<>(defaultCurveSampler, mergeDefault);
    }


    @NotNull
    @Override public Bag<Termed> termbag() {
        return new CurveBag<>(defaultCurveSampler, mergeDefault);
    }


    private final BudgetMerge mergeDefault = BudgetMerge.plusDQBlend;




    final static Logger logger = LoggerFactory.getLogger(DefaultConceptBuilder.class);


    @NotNull
    public final Random rng; //shared
    public final CurveBag.CurveSampler defaultCurveSampler; //shared


    public DefaultConceptBuilder(@NotNull Random r) {
        this.rng = r;
        this.defaultCurveSampler = new CurveBag.DirectSampler(CurveBag.power2BagCurve, rng);
    }


    @Override
    @Nullable
    public Termed apply(@NotNull Term term) {

        //already a concept, assume it is from here
        if (term instanceof Concept) {
            return term;
        }

        Termed result = null;
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
}
