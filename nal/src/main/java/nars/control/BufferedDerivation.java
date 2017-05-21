package nars.control;

import nars.Task;
import nars.premise.Derivation;
import nars.task.DerivedTask;
import nars.task.ITask;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public final class BufferedDerivation extends Derivation {

    final Set<DerivedTask> buffer =
            new UnifiedSet();
            //new LinkedHashSet();

    @Override
    public void derive(DerivedTask x) {
        buffer.add(x);
    }

    @Nullable
    public ITask[] flush() {
        int bs = buffer.size();
        if (bs > 0) {
            ITask[] dd = buffer.toArray(new ITask[bs]);
            buffer.clear();
            return dd;
        }
        return null;
    }
}
