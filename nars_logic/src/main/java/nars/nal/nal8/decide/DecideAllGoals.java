package nars.nal.nal8.decide;

import nars.Symbols;
import nars.task.Task;
import org.jetbrains.annotations.NotNull;

/**
 * true if the task is a goal, even if it is negative (frequency < 0.5)
 */
public class DecideAllGoals implements Decider {

    public static final DecideAllGoals the = new DecideAllGoals();

    @Override
    public boolean test(@NotNull Task task) {
        return (task.getPunctuation() == Symbols.GOAL);
    }

}
