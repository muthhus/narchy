package nars.control;

import jcog.pri.Priority;
import nars.control.premise.Derivation;
import nars.task.DerivedTask;
import nars.task.ITask;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

public final class BufferedDerivation extends Derivation implements BiFunction<ITask, ITask, ITask> {

    public final Map<ITask,ITask> buffer = new LinkedHashMap();

    @Override
    public ITask apply(ITask prev, ITask next) {
        return prev.merge(next);
    }

    @Override
    public void derive(DerivedTask x) {
        buffer.merge(x, x, this);
    }

    @Nullable
    public ITask[] flush(float normalizeTo) {
        int bs = buffer.size();
        if (bs > 0) {
            ITask[] xx = buffer.values().toArray(new ITask[bs]);
            buffer.clear();

            Priority.normalize(xx, normalizeTo);

            return xx;
        }
        return null;
    }


}
