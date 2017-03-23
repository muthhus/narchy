package nars.util.task;

import nars.*;
import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.SubUnify;
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
    private final NAR nar;

    public TaskMatch(String pattern, @NotNull NAR n) throws Narsese.NarseseException {
        this($.$(pattern), n);
    }

    public TaskMatch(Compound pattern, @NotNull NAR n) {
        this.nar = n;
        this.pattern = pattern;
        n.eventTaskProcess.on(this);
    }

    @Override
    public void accept(@NotNull Task task) {
        if (task.isQuestion()) {
            Map<Term,Term> result = new HashMap();
            final SubUnify match = new SubUnify(nar.concepts, Op.VAR_PATTERN, nar.random, Param.SubUnificationMatchRetries) {
                @Override
                public boolean onMatch() {
                    xy.forEachVersioned((x,y)->{ result.put(x,y); return true; }  );
                    return true;
                }
            }; //re-using this is not thread-safe
            match.tryMatch(pattern, task.term());
            if (!result.isEmpty()) {
                onMatch(task, result);
            }
        }
    }

    abstract protected void onMatch(Task task, Map<Term, Term> xy);
}
