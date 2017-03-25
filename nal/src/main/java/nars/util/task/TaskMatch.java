package nars.util.task;

import jcog.event.On;
import nars.*;
import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.SubUnify;
import nars.term.util.InvalidTermException;
import nars.util.SoftException;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by me on 6/1/16.
 */
abstract public class TaskMatch implements Consumer<Task> {

    public final Compound pattern;
    @NotNull
    protected final NAR nar;
    private final On on;

    public TaskMatch(String pattern, @NotNull NAR n) throws Narsese.NarseseException {
        this($.$(pattern), n);
    }

    public TaskMatch(Compound pattern, @NotNull NAR n) {
        this.nar = n;
        this.pattern = pattern;
        this.on = n.onTask(this);
    }

    public void off() {
        this.on.off();
    }

    protected boolean test(Task t) {
        return true;
    }

    @Override
    public void accept(@NotNull Task task) {

        task = nar.post(task);

        if (test(task)) {
            Map<Term,Term> result = new HashMap();
            final int[] matches = {0};
            final SubUnify match = new SubUnify(nar.concepts, Op.VAR_PATTERN, nar.random, Param.SubUnificationMatchRetries) {
                @Override
                public boolean onMatch() {
                    xy.forEachVersioned((x,y)->{ result.put(x,y); return true; }  );
                    matches[0]++;
                    return true;
                }
            };

            if (match.tryMatch(pattern, task.term()) && matches[0] > 0) {
                try {
                    onMatch(task, result);
                } catch (InvalidTermException | InvalidTaskException e) {
                    onError(e);
                }
            }
        }
    }

    protected void onError(SoftException e) {
        //default: do nothing
    }

    abstract protected void onMatch(Task task, Map<Term, Term> xy);
}
