package nars;

import nars.task.ActiveQuestionTask;
import nars.task.ITask;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * NAR Input methods
 */
public interface NARIn {

    void input(ITask... t);

    default <T extends Term> T term(@NotNull String t) throws Narsese.NarseseException {
        return $.terms.term(t);
    }

    @Nullable
    default Task question(@NotNull String questionTerm, long occ, @NotNull BiConsumer<ActiveQuestionTask,Task> eachAnswer) throws Narsese.NarseseException {
        return question(term(questionTerm), occ, eachAnswer);
    }

    @Nullable
    default Task question(@NotNull Compound term, long occ, @NotNull BiConsumer<ActiveQuestionTask,Task> eachAnswer) {
        return question(term, occ, Op.QUESTION, eachAnswer);
    }

    @Nullable
    default ActiveQuestionTask question(@NotNull Compound term, long occ, byte punc /* question or quest */, @NotNull BiConsumer<ActiveQuestionTask, Task> eachAnswer) {
        assert(punc == Op.QUESTION || punc == Op.QUEST);
        return inputAndGet( new ActiveQuestionTask(term, punc, occ, 16, (NAR)this, eachAnswer) );
    }

    @Nullable
    default ActiveQuestionTask ask(@NotNull Compound term, long occ, byte punc /* question or quest */, @NotNull Consumer<Task> eachAnswer) {
        assert(punc == Op.QUESTION || punc == Op.QUEST);
        return inputAndGet( new ActiveQuestionTask(term, punc, occ, 16, (NAR)this, eachAnswer) );
    }


    @NotNull default <T extends Task> T inputAndGet(@NotNull T t) {
        input(t);
        return t;
    }


}
