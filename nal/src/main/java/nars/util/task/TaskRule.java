package nars.util.task;

import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.Task;
import nars.task.ImmutableTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.MapSubst;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static nars.term.Terms.compoundOrNull;

/**
 * matches a belief pattern and creates an identity result
 */
public class TaskRule extends TaskMatch{

    /** the output pattern */
    public final Compound output;

    public TaskRule(String input, String output, NAR nar) throws Narsese.NarseseException {
        super(input, nar);
        this.output = $.$(output);
    }

    @Override
    protected void onMatch(Task X, Map<Term, Term> xy) {

        Compound y = compoundOrNull(nar.concepts.transform(output, new MapSubst(xy)));
        if (y==null) return;

        y = Task.post(y, nar);
        if (y==null) return;

        y = compoundOrNull(nar.concepts.normalize(y));
        if (y==null) return;

        if (!Task.taskContentValid(y, X.punc(), nar, true))
            return;

        Task Y = ((ImmutableTask)X).clone(y);
        if (Y != null)
            nar.input(Y);
    }

}
