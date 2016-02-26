package nars.concept;

import nars.bag.Bag;
import nars.bag.impl.CurveBag;
import nars.budget.BudgetMerge;
import nars.nal.space.Space;
import nars.nal.space.SpaceConcept;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.function.Function;

/**
 * Created by me on 2/24/16.
 */
public class DefaultConceptBuilder implements Function<Term, Concept> {

    /**
     * default for new concepts
     */
    public final MutableInt taskLinkBagSize = new MutableInt(16);

    /**
     * default for new concepts
     */
    public final MutableInt termLinkBagSize = new MutableInt(16);
    public final Random rng;


    public DefaultConceptBuilder() {
        this(null, 1, 1);
    }

    public DefaultConceptBuilder(Random r, int tasklinkBagSize, int termlinkBagSize) {

        this.rng = r;
        this.taskLinkBagSize.setValue(tasklinkBagSize);
        this.termLinkBagSize.setValue(termlinkBagSize);
    }

    @Override
    public Concept apply(Term t) {


        Random random = rng;

        Bag<Task> taskLinks =
                new CurveBag<Task>(taskLinkBagSize.intValue(), random)
                        .merge(BudgetMerge.plusDQBlend);

        Bag<Termed> termLinks =
                new CurveBag<Termed>(termLinkBagSize.intValue(), random)
                        .merge(BudgetMerge.plusDQBlend);

        Concept c;
        //switch (t.op()) {
            /*Op.NEGATE:

                break;*/
            //default:
        c = (!(t.isCompound())) ?

                newAtomConcept(t, taskLinks, termLinks) :

                newCompoundConcept(t, taskLinks, termLinks);
        //}
        return c;
    }

    @NotNull
    protected Concept newCompoundConcept(Term t, Bag<Task> taskLinks, Bag<Termed> termLinks) {
        return (!(t instanceof Space)) ?

                new DefaultConcept(t, taskLinks, termLinks) :

                new SpaceConcept((Space) t, taskLinks, termLinks);
    }

    protected @NotNull AtomConcept newAtomConcept(Term t, Bag<Task> taskLinks, Bag<Termed> termLinks) {
        return new AtomConcept(t, termLinks, taskLinks);
    }



}
