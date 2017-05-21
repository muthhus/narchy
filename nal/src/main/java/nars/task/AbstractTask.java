package nars.task;

import jcog.pri.Pri;
import org.jetbrains.annotations.NotNull;


abstract public class AbstractTask extends Pri implements ITask {


    public AbstractTask(float pri) {
        super(pri);
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof ITask && hashCode() == obj.hashCode()) {
            ITask that = (ITask) obj;
            Object thatKey = that.key();
            if (thatKey != that && key().equals(thatKey))
                return true;
        }
        return false;
    }

    abstract public int hashCode();

    abstract public @NotNull String toString();/* {
        return super.toString() + " " + getClass().getSimpleName();
    }*/

}
