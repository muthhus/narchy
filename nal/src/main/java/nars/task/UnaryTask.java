package nars.task;

import jcog.Util;
import org.jetbrains.annotations.NotNull;

/**
 * task which is identified by one value parameter, and the class type itself
 */
abstract public class UnaryTask<X> extends AbstractTask {

    @NotNull public final X value;
    private final int hash;

    public UnaryTask(@NotNull X value, float pri) {
        super(pri);
        this.value = value;
        this.hash = Util.hashCombine(getClass().hashCode(), value.hashCode());
    }


    @Override
    public final @NotNull String toString() {
        return getClass().getSimpleName() + "(\"" + value + "\")";
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj!=null && (obj.getClass() == getClass()) && ((nars.task.UnaryTask) obj).value.equals(value)) {
            //optional: merge??
            return true;
        }

        return false;
    }

    @Override
    public final int hashCode() {
        return hash;
    }

}
