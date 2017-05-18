package nars.task;

import jcog.pri.Pri;
import jcog.pri.Priority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


abstract public class AbstractTask extends Pri implements ITask {


    public AbstractTask(float pri) {
        super(pri);
    }

    @Override
    public @Nullable Priority clone() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull String toString() {
        return super.toString() + " " + getClass().getSimpleName();
    }
}
