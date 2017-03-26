package nars.util.task;

import nars.NAR;
import nars.Narsese;
import nars.Task;
import nars.task.ImmutableTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.MapSubst;
import nars.term.transform.VariableNormalization;
import nars.term.var.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static nars.term.Terms.compoundOrNull;

/**
 * matches a belief pattern and creates an identity result
 */
public class TaskRule extends TaskMatch{

    static final Logger logger = LoggerFactory.getLogger(TaskRule.class);

    /** the output pattern */
    public final Compound output;

    /** version of output with the original GenericVariable's, for display or reference purposes */
    private final Compound outputRaw;

    /** mapping of input variables to normalized variables */
    private final Map<Variable, Variable> io;

    public TaskRule(String input, String output, NAR nar) throws Narsese.NarseseException {
        super(input, nar);
        this.outputRaw = (Compound) nar.concepts.parseRaw(output);

        VariableNormalization varNorm = new VariableNormalization(outputRaw.size() /* est */);

        this.output = compoundOrNull(nar.concepts.transform(outputRaw, varNorm));
        if (this.output == null)
            throw new RuntimeException("output pattern is not compound");

        this.io = varNorm.map;
    }

    @Override
    protected void eachMatch(Task X, Map<Term, Term> xy) {

        Compound y = compoundOrNull(nar.concepts.transform(output, new MapSubst(xy)));
        if (y==null) return;

        y = Task.content(y, nar);
        if (y==null) return;

        y = compoundOrNull(nar.concepts.normalize(y));
        if (y==null) return;

        if (!Task.taskContentValid(y, X.punc(), nar, true))
            return;

        Task Y = ((ImmutableTask)X).clone(y);
        if (Y != null) {
            logger.info("{}\t{}", X, Y);
            nar.input(Y);
        }
    }

}
