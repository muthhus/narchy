package nars.task;

import nars.NAR;
import nars.Task;
import nars.bag.impl.ArrayBag;
import nars.budget.BudgetMerge;
import nars.budget.DependentBLink;
import nars.term.Compound;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Question task which accepts a callback to be invoked on answers
 */
public class LambdaQuestionTask extends ImmutableTask {

    private @NotNull final BiConsumer<? super LambdaQuestionTask /* Q */, Task /* A */ > eachAnswer;

    final ArrayBag<Task> answers;

    /** wrap an existing question task */
    public LambdaQuestionTask(Task q, int history, NAR nar, BiConsumer<? super LambdaQuestionTask,Task> eachAnswer ) {
        this(q.term(), q.punc(), q.mid() /*, q.end()*/, history, nar, eachAnswer );
    }

    public LambdaQuestionTask(@NotNull Compound term, byte punc, long occ, int history, NAR nar, @NotNull Consumer<Task> eachAnswer) {
        this(term, punc, occ, history, nar, (q, a) -> {
            eachAnswer.accept(a);
        });
    }

    public LambdaQuestionTask(@NotNull Compound term, byte punc, long occ, int history, NAR nar, @NotNull BiConsumer<? super LambdaQuestionTask, Task> eachAnswer) {
        super(term, punc, null, nar.time(), occ, occ, new long[] { nar.time.nextStamp() } );

        budget(nar);

        this.answers = newBag(history);
        this.eachAnswer = eachAnswer;
    }

    protected ArrayBag<Task> newBag(int history) {
        return new ArrayBag<>(history, BudgetMerge.maxHard, new ConcurrentHashMap<>(history));
    }

    @Override
    public Task onAnswered(Task answer, NAR nar) {
        //answer = super.onAnswered(answer, nar);

        boolean novel = !answers.contains(answer);
        answers.put(new DependentBLink<Task>(answer));
        if (novel) {
            eachAnswer.accept(this, answer);
        }

        return answer;
    }
}
