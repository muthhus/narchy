package nars.task;

import nars.NAR;
import nars.Task;
import nars.bag.ArrayBag;
import nars.budget.BudgetMerge;
import nars.link.DependentBLink;
import nars.term.Compound;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Question task which accepts a callback to be invoked on answers
 */
public class LambdaQuestionTask extends MutableTask {

    private @NotNull final BiConsumer<LambdaQuestionTask /* Q */, Task /* A */ > eachAnswer;

    final ArrayBag<Task> answers;

    public LambdaQuestionTask(@NotNull Termed<Compound> term, char punc, long occ, int history, @NotNull Consumer<Task> eachAnswer) {
        this(term, punc, occ, history, (q, a) -> {
            eachAnswer.accept(a);
        });
    }

    public LambdaQuestionTask(@NotNull Termed<Compound> term, char punc, long occ, int history, @NotNull BiConsumer<LambdaQuestionTask, Task> eachAnswer) {
        super(term, punc, null);
        this.answers = new ArrayBag<>(history, BudgetMerge.maxHard, new ConcurrentHashMap<>(history));
        this.eachAnswer = eachAnswer;
        occurr(occ);
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
