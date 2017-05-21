package nars.util.exe;

import jcog.pri.Priority;
import nars.NAR;
import nars.concept.Concept;
import nars.task.ITask;
import nars.task.util.InvalidTaskException;
import nars.term.util.InvalidTermException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProxyTask<T extends ITask> implements ITask {

    public final T the;

    public ProxyTask(T x) {
        the = x;
    }

    @Override
    public void run(NAR n) throws Concept.InvalidConceptException, InvalidTermException, InvalidTaskException {
        the.run(n);
    }

    @Override
    public @NotNull Priority priority() {
        return the;
    }

    @Override
    public float pri() {
        return the.pri();
    }

    @Override
    public float setPri(float p) {
        return the.setPri(p);
    }

    @Override
    public @Nullable Priority clone() {
        return the.clone();
    }

    @Override
    public boolean equals(Object obj) {
        return the.equals(obj);
    }

    @Override
    public int hashCode() {
        return the.hashCode();
    }

    @Override
    public Object key() {
        return the.key();
    }

    @Override
    public boolean delete() {
        return the.delete();
    }
}
