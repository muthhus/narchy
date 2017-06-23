package nars.task.util;

import jcog.event.On;
import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.Task;
import nars.derive.meta.AtomicPred;
import nars.derive.meta.BoolPred;
import nars.term.Compound;
import nars.term.Term;
import nars.util.SoftException;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Generic handler for matching individual Tasks's
 */
abstract public class TaskMatch extends AtomicPred<Task> implements Consumer<Task> {

    @NotNull protected final NAR nar;
    private final On on;
    private BoolPred<Term> term;
    //private final BoolPred<Truth> truth = null;
    private BoolPred<Byte> punctuation;
    //private final BoolPred<LongLongPair> time = null;
    //private final BoolPred<Priority> budget = null;



    abstract public static class TermMatch extends AtomicPred<Term> {

        public final Term term;

        public TermMatch(String s) throws Narsese.NarseseException {
            this($.$(s));
        }

        public TermMatch(Term t) {
            this.term = t;
        }

        @Override
        abstract public boolean test(Term p);

        @NotNull
        @Override
        public String toString() {
            return getClass().getSimpleName() + '(' + term + ')';
        }
    }

    public TaskMatch(@NotNull NAR n) {
        this.nar = n;
        this.on = n.onTask(this);
    }

    public void setTerm(BoolPred<Term> term) {
        this.term = term;
    }

    public void setPunctuation(BoolPred<Byte> punctuation) {
        this.punctuation = punctuation;
    }

    @NotNull
    @Override
    public String toString() {
        return id().toString();
    }

    //HACK
    public Compound id() {
        return $.func(getClass().getSimpleName(),
            $.nonNull(term), $.nonNull(punctuation)
                //, $.nonNull(truth), $.nonNull(time), $.nonNull(budget)
            );
    }

    public void off() {
        this.on.off();
    }

    @Override
    public boolean test(Task t) {
        if (term!=null && !term.test(t.term())) return false;
        //..
        return true;
    }

    @Override
    public void accept(@NotNull Task _x) {

        Task x = nar.post(_x);

        test(x);




    }


    protected void onError(SoftException e) {
        //default: do nothing
    }

    /** accepts the next match
     *
     * @param task
     * @param xy
     * @return true for callee to continue matching, false to stop
     */
    abstract protected void accept(Task task, Map<Term, Term> xy);


}
