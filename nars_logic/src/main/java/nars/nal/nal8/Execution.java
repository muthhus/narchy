package nars.nal.nal8;

import com.google.common.collect.Lists;
import nars.$;
import nars.Memory;
import nars.NAR;
import nars.budget.Budget;
import nars.budget.UnitBudget;
import nars.java.NALObjects;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;
import nars.util.event.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Execution context which Operator implementations
 * receive, supporting any variety of synchronous/
 * asynchronous feedback, access to the invoking NAR,
 * and utility methods for extracting features
 * of the operation task in the context of the executing NAR.
 */
public class Execution implements Runnable {

    private static final float DEFAULT_EXECUTION_CONFIDENCE = 0.99f;
    public final NAR nar;
    public final Task task;
    private final Topic<Execution> listeners;

    public Execution(NAR nar, Task task, Topic<Execution> listeners) {
        this.nar = nar;
        this.task = task;
        this.listeners = listeners;
    }

    /** should only be called by NAR */
    @Override public final void run() {
        //if (task.getDeleted()) return;

        listeners.emit(this);
    }

    public final Compound term() {
        return task.term();
    }

    /**
     * unwrapped (without ^)
     */
    @NotNull
    public final Operator operator() {
        return Operator.operatorTerm(term());
    }
    public final Term[] argArray() {
        return Operator.opArgsArray(term());
    }


    //arg(int i)

    //feedback(Term t)
    //feedback(Task t)
    //feedback(Task[] t)
    //feedback(Object o)

    public void feedback(Task feedback) {
        //feedback(Collections.singletonList(feedback));
        feedbackAll(
            feedback,
            noticeExecuted()
        );
    }
    void feedbackAll(Task... feedback) {
        feedback(Lists.newArrayList(feedback));
    }

    /**
     * called after execution completed
     */
    public void feedback(@Nullable Iterable<Task> feedback) {

        //Display a message in the output stream to indicate the reportExecution of an operation



        //feedback tasks as input
        //should we allow immediate tasks to create feedback?
        if (feedback != null) {

            //final Operation t = op.getTerm();

            feedback.forEach(f -> {
                //if (t == null) continue;

                //TODO avoid using a string like this
                //f.log("Feedback: " + t /*"Feedback"*/);

                nar.input(f.log("Feedback"));
            });
        } else {
            //default: noticed executed
            if (!task.isCommand()) {
                noticeExecuted(task);
            }
        }

    }

    protected Task noticeExecuted() {
        return noticeExecuted(task);
    }

    /**
     * internal notice of the execution
     * @param operation
     */
    protected Task noticeExecuted(@NotNull Task operation) {

        Budget b = !operation.isDeleted() ? operation.budget() : UnitBudget.zero;

        Memory memory = nar.memory;

        return $.belief(operation.term(),

                operation.truth()). //equal to input, balanced
                //1f, DEFAULT_EXECUTION_CONFIDENCE).

                        budget(b).
                        present(memory).
                //parent(operation). //https://github.com/opennars/opennars/commit/23d34d5ddaf7c71348d0a70a88e2805ec659ed1c#diff-abb6b480847c96e2dbf488d303fb4962L235
                        because("Executed")
        ;

    }

    public void feedback(Truth y) {


        //this will get the original input operation term, not after it has been inlined.
        feedback( new MutableTask(
                Operator.result(task.term(), NALObjects.TRUE)
                //task.concept()
        ).judgment()
                .truth(y).present(nar.memory)
                .budget(task.budget())
        );

    }
}
