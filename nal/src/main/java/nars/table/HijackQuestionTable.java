package nars.table;

import nars.NAR;
import nars.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

/**
 * Created by me on 2/16/17.
 */
public class HijackQuestionTable implements QuestionTable {

    @Override
    public int capacity() {
        return 0;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean remove(Task x) {
        return false;
    }


    @Override
    public @Nullable Task add(@NotNull Task t, @NotNull BeliefTable answers, @NotNull NAR n) {
        return null;
    }

    @Override
    public void capacity(int newCapacity, NAR nar) {

    }

    @NotNull
    @Override
    public Iterator<Task> iterator() {
        return null;
    }
}
