package nars;

import nars.task.LambdaQuestionTask;
import nars.term.Compound;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * NAR Input methods
 */
public interface NARIn {


    void input(Task... t);

    Termed term(@NotNull String t) throws Narsese.NarseseException;

    NAR nar();

    @Nullable
    default Task ask(@NotNull String questionTerm, long occ, @NotNull BiConsumer<LambdaQuestionTask,Task> eachAnswer) throws Narsese.NarseseException {
        return ask((Compound)term(questionTerm), occ, eachAnswer);
    }

    @Nullable
    default Task ask(@NotNull Compound term, long occ, @NotNull BiConsumer<LambdaQuestionTask,Task> eachAnswer) {
        return ask(term, occ, Op.QUESTION, eachAnswer);
    }

    @Nullable
    default LambdaQuestionTask ask(@NotNull Compound term, long occ, byte punc /* question or quest */, @NotNull BiConsumer<LambdaQuestionTask, Task> eachAnswer) {
        assert(punc == Op.QUESTION || punc == Op.QUEST);
        return inputAndGet( new LambdaQuestionTask(term, punc, occ, 16, (NAR)this, eachAnswer) );
    }

    @Nullable
    default LambdaQuestionTask ask(@NotNull Compound term, long occ, byte punc /* question or quest */, @NotNull Consumer<Task> eachAnswer) {
        assert(punc == Op.QUESTION || punc == Op.QUEST);
        return inputAndGet( new LambdaQuestionTask(term, punc, occ, 16, (NAR)this, eachAnswer) );
    }

    @NotNull
    default Task inputAndGet(@NotNull String taskText) throws Narsese.NarseseException {
        return inputAndGet(Narsese.the().task(taskText, nar()));
    }

    @NotNull default <T extends Task> T inputAndGet(@NotNull T t) {
        input(t);
        return t;
    }


}
