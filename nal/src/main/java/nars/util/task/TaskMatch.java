package nars.util.task;

import jcog.event.On;
import nars.*;
import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.SubUnify;
import nars.term.util.InvalidTermException;
import nars.util.SoftException;
import org.jetbrains.annotations.NotNull;

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
    public void accept(@NotNull Task _x) {

        Task x = nar.post(_x);

        if (test(x)) {
            final SubUnify match = new SubUnify(nar.concepts, Op.VAR_PATTERN, nar.random, Param.SubUnificationMatchRetries) {

                int count = 0;

                @Override
                public boolean onMatch() {
                    eachMatch(x, xy);
                    count++;
                    return false; //only one, but true would allow multiple
                }

            };

            try {
                match.tryMatch(pattern, x.term());
            } catch (InvalidTermException | InvalidTaskException e) {
                onError(e);
            }


        }
    }

    protected void onError(SoftException e) {
        //default: do nothing
    }

    abstract protected void eachMatch(Task task, Map<Term, Term> xy);
}
