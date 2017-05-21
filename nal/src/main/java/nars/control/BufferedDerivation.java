package nars.control;

import nars.premise.Derivation;
import nars.task.DerivedTask;
import nars.task.ITask;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public final class BufferedDerivation extends Derivation implements BiFunction<ITask, ITask, ITask> {

    final Map<ITask,ITask> buffer =
            new LinkedHashMap();

    @Override
    public ITask apply(ITask prev, ITask next) {
        return prev.merge(next);
    }

    @Override
    public void derive(DerivedTask x) {
        buffer.merge(x, x, this);
    }

    @Nullable
    public ITask[] flush() {
        int bs = buffer.size();
        if (bs > 0) {
            ITask[] dd = buffer.values().toArray(new ITask[bs]);
            buffer.clear();
            return dd;
        }
        return null;
    }


}
