package nars.concept;

import com.gs.collections.api.block.function.primitive.ObjectIntToObjectFunction;
import nars.bag.Bag;
import nars.bag.impl.CurveBag;
import nars.budget.BudgetMerge;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.variable.Variable;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * Created by me on 2/24/16.
 */
public class DefaultConceptBuilder implements Function<Term,Concept> {

    final Function<Atomic, AtomConcept> atomBuilder =
            (Atomic a) -> new AtomConcept(a, taskbag(), termbag());


    //private static volatile int serial = 0;

    final Function<Variable, VariableConcept> varBuilder =
            (Variable v) -> new VariableConcept(v, termbag(), taskbag());

    final Function<Compound, CompoundConcept> compoundBuilder =
            (Compound t) -> new CompoundConcept(t, termbag(), taskbag());



    //return (!(t instanceof Space)) ?
    //new SpaceConcept((Space) t, taskLinks, termLinks);

    private Bag<Task> taskbag() {
          return new CurveBag<Task>(taskLinkBagSize.intValue(), rng)
              .merge(mergeDefault());
    }

    private BudgetMerge mergeDefault() {
        return BudgetMerge.plusDQBlend;
    }

    private Bag<Termed> termbag() {
        return new CurveBag<Termed>(termLinkBagSize.intValue(), rng)
                .merge(mergeDefault());

    }

    final static Logger logger = LoggerFactory.getLogger(DefaultConceptBuilder.class);


    /**
     * default for new concepts
     */
    public final MutableInt taskLinkBagSize = new MutableInt(16);

    /**
     * default for new concepts
     */
    public final MutableInt termLinkBagSize = new MutableInt(16);


    public final Random rng;



    public DefaultConceptBuilder(Random r, int tasklinkBagSize, int termlinkBagSize) {

        this.rng = r;
        this.taskLinkBagSize.setValue(tasklinkBagSize);
        this.termLinkBagSize.setValue(termlinkBagSize);
    }


    @Override
    @Nullable
    public Concept apply(@NotNull Term term) {

        //already a concept, assume it is from here
        if (term instanceof Concept) {
            return (Concept)term;
        }

        Concept result = null;
        if (term instanceof Compound) {
            result = compoundBuilder.apply((Compound) term);
        } else {

            if (term instanceof Variable) {
                //final int s = this.serial;
                //serial++;
                result = varBuilder.apply((Variable) term);
            } else if (term instanceof Atomic) {
                result = atomBuilder.apply((Atomic) term);
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
