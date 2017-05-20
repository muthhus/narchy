package nars.task;

import jcog.Util;
import jcog.pri.Pri;
import jcog.pri.Priority;
import nars.attention.SpreadingActivation;
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


    abstract public @NotNull String toString();/* {
        return super.toString() + " " + getClass().getSimpleName();
    }*/

}
