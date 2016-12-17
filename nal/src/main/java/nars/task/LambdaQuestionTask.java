package nars.task;

import nars.NAR;
import nars.Task;
import nars.term.Compound;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Question task which accepts a callback to be invoked on answers
 */
public class LambdaQuestionTask extends MutableTask {
    private
    @NotNull
    final Predicate<Task> eachAnswer;

    boolean includeRepeats = false;

    public LambdaQuestionTask(@NotNull Termed<Compound> term, char punc, long occ, @NotNull Consumer<Task> eachAnswer) {
        this(term, punc, occ, (t) -> {
            eachAnswer.accept(t);
            return true;
        });
    }

    public LambdaQuestionTask(@NotNull Termed<Compound> term, char punc, long occ, @NotNull Predicate<Task> eachAnswer) {
        super(term, punc, null);
        occurr(occ);
        this.eachAnswer = eachAnswer;
    }

    @Override
    public Task onAnswered(Task answer, NAR nar) {
        super.onAnswered(answer, nar);
        if (includeRepeats || !answer.isDeleted()) {
            if (!eachAnswer.test(answer)) {
                return null;
            }
        }
        return answer;
    }
}
