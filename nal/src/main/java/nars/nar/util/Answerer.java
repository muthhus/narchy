package nars.nar.util;

import nars.NAR;
import nars.Op;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.OneMatchFindSubst;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by me on 6/1/16.
 */
abstract public class Answerer implements Consumer<Task> {

    public final Compound pattern;
    private final NAR nar;

    public Answerer(Compound pattern, NAR n) {
        this.nar = n;
        this.pattern = pattern;
        n.eventTaskProcess.on(this);
    }

    @Override
    public void accept(Task task) {
        if (task.isQuestion()) {
            final OneMatchFindSubst match = new OneMatchFindSubst(); //re-using this is not thread-safe
            if (match.tryMatch(Op.VAR_PATTERN, pattern, task.term())) {
                onMatch(match.xy);
            }
        }
    }

    abstract protected void onMatch(Map<Term, Term> xy);
}
