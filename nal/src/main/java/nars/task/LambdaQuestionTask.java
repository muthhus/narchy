package nars.task;

import jcog.bag.impl.ArrayBag;
import jcog.bag.impl.PLinkArrayBag;
import jcog.pri.PLinkUntilDeleted;
import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Task;
import nars.term.Compound;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Question task which accepts a callback to be invoked on answers
 */
public class LambdaQuestionTask extends NALTask {

    private @NotNull final BiConsumer<? super LambdaQuestionTask /* Q */, Task /* A */ > eachAnswer;

    final ArrayBag<Task, PriReference<Task>> answers;

    /** wrap an existing question task */
    public LambdaQuestionTask(Task q, int history, NAR nar, BiConsumer<? super LambdaQuestionTask,Task> eachAnswer ) {
        this(q.term(), q.punc(), q.mid() /*, q.end()*/, history, nar, eachAnswer );
    }

    public LambdaQuestionTask(@NotNull Compound term, byte punc, long occ, int history, NAR nar, @NotNull Consumer<Task> eachAnswer) {
        this(term, punc, occ, history, nar, (q, a) -> eachAnswer.accept(a));
    }

    public LambdaQuestionTask(@NotNull Compound term, byte punc, long occ, int history, NAR nar, @NotNull BiConsumer<? super LambdaQuestionTask, Task> eachAnswer) {
        super(term, punc, null, nar.time(), occ, occ, new long[] { nar.time.nextStamp() } );

        budget(nar);

        this.answers = newBag(history);
        this.eachAnswer = eachAnswer;
    }

    protected ArrayBag<Task, PriReference<Task>> newBag(int history) {
        return new PLinkArrayBag<>(history, PriMerge.max, new ConcurrentHashMap<>(history)) {
            @Override
            public void onAdded(@NotNull PriReference<Task> t) {
                eachAnswer.accept(LambdaQuestionTask.this, t.get());
            }
        };
    }

    @Override
    public Task onAnswered(Task answer, NAR nar) {
        //answer = super.onAnswered(answer, nar);
        answers.put(new PLinkUntilDeleted<>(answer, answer.priSafe(0)));
        return answer;
    }
}
