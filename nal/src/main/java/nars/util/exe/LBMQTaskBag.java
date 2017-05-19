package nars.util.exe;

import lbmq.LinkedBlockingMultiQueue;
import nars.task.ITask;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * uses: LinkedBlockingMultiQueue
 */
public class LBMQTaskBag extends TaskBag {

    final int GRANULARITY = 4;
    final LinkedBlockingMultiQueue<Integer, ITask> q;
    final Set<ITask> active;

    public LBMQTaskBag(int capacity) {

        q = new LinkedBlockingMultiQueue();
        active = Collections.newSetFromMap(new ConcurrentHashMap(capacity));

        int subCap = capacity / GRANULARITY;

        for (int i = 0; i < GRANULARITY; i++) {
            q.addSubQueue(i, i, subCap);
        }

    }

    @Override
    public boolean add(ITask x) {
        float p = x.pri();
        if (p != p)
            return false;

        if (active.contains(x))
            return false;

        int pi = (int) (p * GRANULARITY);
        if (pi >= GRANULARITY) pi = GRANULARITY - 1; //?


        if (q.offer(pi, x)) {
            active.add(x);
            return true;
        }
        return false;
    }

    @Override
    public ITask next() {
//            ITask x = q.poll(); //non-blocking

        ITask x = null;
        try {
            x = q.take(); //blocking
        } catch (InterruptedException e) {
            return null;
        }
        if (x != null) {
            active.remove(x);
        }
        return x;
    }

    @Override
    public int size() {
        return q.totalSize();
    }

    @Override
    public void commit() {

    }

    @Override
    public void clear() {
        q.clear();
    }

    @Override
    public float load() {
        return 0;
    }

    @Override
    public void forEach(Consumer<ITask> t) {

    }
}
