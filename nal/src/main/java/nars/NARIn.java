package nars;

import nars.concept.Concept;
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

    Concept input(Task t);

    Termed term(@NotNull String t) throws Narsese.NarseseException;

    NAR nar();

    @Nullable
    default Task ask(@NotNull String questionTerm, long occ, @NotNull BiConsumer<LambdaQuestionTask,Task> eachAnswer) throws Narsese.NarseseException {
        return ask(term(questionTerm), occ, eachAnswer);
    }

    @Nullable
    default Task ask(@NotNull Termed<Compound> term, long occ, @NotNull BiConsumer<LambdaQuestionTask,Task> eachAnswer) {
        return ask(term, occ, Op.QUESTION, eachAnswer);
    }

    @Nullable
    default LambdaQuestionTask ask(@NotNull Termed<Compound> term, long occ, char punc /* question or quest */, @NotNull BiConsumer<LambdaQuestionTask, Task> eachAnswer) {
        assert(punc == Op.QUESTION || punc == Op.QUEST);
        return inputAndGet( new LambdaQuestionTask(term, punc, occ, 16, eachAnswer) );
    }

    @Nullable
    default LambdaQuestionTask ask(@NotNull Termed<Compound> term, long occ, char punc /* question or quest */, @NotNull Consumer<Task> eachAnswer) {
        assert(punc == Op.QUESTION || punc == Op.QUEST);
        return inputAndGet( new LambdaQuestionTask(term, punc, occ, 16, eachAnswer) );
    }

    @NotNull
    default Task inputAndGet(@NotNull String taskText) {
        return inputAndGet(Narsese.the().task(taskText, nar()));
    }

    @NotNull default <T extends Task> T inputAndGet(@NotNull T t) {
        input(t);
        return t;
    }


}
