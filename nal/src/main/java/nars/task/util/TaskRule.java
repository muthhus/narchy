package nars.task.util;

import nars.*;
import nars.task.NALTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.MapSubst;
import nars.term.subst.SubUnify;
import nars.term.transform.VariableNormalization;
import nars.term.util.InvalidTermException;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static nars.term.Terms.compoundOrNull;

/**
 * matches a belief pattern and creates an identity result
 */
public class TaskRule extends TaskMatch {

    static final Logger logger = LoggerFactory.getLogger(TaskRule.class);

    /** the output pattern */
    public final Compound output;

    /** version of output with the original GenericVariable's, for display or reference purposes */
    private final Compound outputRaw;

    /** mapping of input variables to normalized variables */
    private final Map<Variable, Variable> io;

    private final Term input;
    private final Compound id;

    public TaskRule(String input, String output, NAR nar) throws Narsese.NarseseException {
        super(nar);

        this.input = $.$(input);
        this.outputRaw = (Compound) nar.terms.termRaw(output);

        VariableNormalization varNorm = new VariableNormalization(outputRaw.size() /* est */);

        this.output = compoundOrNull(nar.terms.transform(outputRaw, varNorm));
        if (this.output == null)
            throw new RuntimeException("output pattern is not compound");

        this.io = varNorm.map;
        this.id = $.impl($.p(this.input, outputRaw, this.output), $.varQuery(0));

//        setTerm(new TermMatch(input) {
//
//            @Override
//            public boolean test(Term p) {
//
//                return true;
//            }
//        });

    }

    private class MySubUnify extends SubUnify {

        public static final int unification_ttl = 16;
        private final Task x;

        public MySubUnify(Task x) {
            super(TaskRule.this.nar.terms, Op.VAR_PATTERN, TaskRule.this.nar.random(), unification_ttl);
            this.x = x;
        }

        @Override
        public boolean onMatch() {
            accept(x, xy);
            return false;
        }

    }

    @Override
    public boolean test(Task x) {
        if (super.test(x)) {

            final SubUnify match = new MySubUnify(x);

            try {
                match.tryMatch(input, x.term());
            } catch (InvalidTermException | InvalidTaskException e) {
                onError(e);
            }

            return true;
        }

        return false;
    }

    @NotNull
    @Override
    public String toString() {
        return id.toString();
    }

    @Override
    protected void accept(Task X, Map<Term, Term> xy) {

        Compound y = compoundOrNull(new MapSubst(xy).transform(output, nar.terms));
        if (y==null) return;

        y = Task.content(y, nar);
        if (y==null) return;

        y = nar.terms.normalize(y);
        if (y==null) return;

        if (!Task.taskContentValid(y, X.punc(), nar, true))
            return;

        Task Y = ((NALTask)X).clone(y);
        if (Y != null) {
            logger.info("{}\t{}", X, Y);
            nar.input(Y);
        }
    }

}
