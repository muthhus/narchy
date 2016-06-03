package nars.nar.util;

import nars.Op;
import nars.bag.Bag;
import nars.bag.impl.CurveBag;
import nars.budget.Budgeted;
import nars.budget.merge.BudgetMerge;
import nars.concept.*;
import nars.link.BLink;
import nars.link.WeakBLinkToBudgeted;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
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
    final Function<Compound, Termed> compoundBuilder = (Compound t) -> {

        switch (t.op()) {
            case INHERIT:
                if (Op.isOperation(t))
                    return new OperationConcept(t, termbag(), taskbag());
                break;
            case NEGATE:
                return t; //return new NegationConcept(t, termbag, taskbag);
        }

        //default:
        return new CompoundConcept(t, termbag(), taskbag());
    };


    //return (!(t instanceof Space)) ?
    //new SpaceConcept((Space) t, taskLinks, termLinks);

    @NotNull
    @Override public Bag<Task> taskbag() {

        return new CurveBag<Task>(rng) {
            @NotNull
            @Override
            protected BLink<Task> newLink(Task i, Budgeted b, float scale) {
                return new WeakBLinkToBudgeted<>(i, b, scale);
            }
        }.merge(mergeDefault());
    }


    @NotNull
    @Override public Bag<Termed> termbag() {

        return new CurveBag<Termed>(rng).merge(mergeDefault());

//        //weak links may be conceptually wrong
//        return new CurveBag<Termed>(rng) {
//            @Override
//            protected BLink<Termed> newLink(Termed i, Budgeted b, float scale) {
//                return new BLink.WeakBLink<>(i, b, scale);
//            }
//        }.merge(mergeDefault());
    }

    @NotNull
    private BudgetMerge mergeDefault() {
        return BudgetMerge.avgDQBlend;
        //return BudgetMerge.plusDQBlend;
        //return BudgetMerge.plusDQDominant;
    }


    final static Logger logger = LoggerFactory.getLogger(DefaultConceptBuilder.class);


    @NotNull
    public final Random rng;


    public DefaultConceptBuilder(@NotNull Random r) {
        this.rng = r;
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
            result = compoundBuilder.apply(  (Compound) term );
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
