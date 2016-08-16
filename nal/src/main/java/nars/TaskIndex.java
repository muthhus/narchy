package nars;

import org.eclipse.collections.impl.map.mutable.ConcurrentHashMapUnsafe;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


import static nars.concept.CompoundConcept.DuplicateMerge;
import static nars.truth.TruthFunctions.eternalize;
import static nars.truth.TruthFunctions.projection;

/**
 * Created by me on 8/14/16.
 */
public final class TaskIndex {

    final static Logger logger = LoggerFactory.getLogger(TaskIndex.class);

    protected final Map<Task, Task> tasks;
    //private final ConcurrentMap<Task, Task> tasksMap;


    public TaskIndex() {
        this.tasks =
                new ConcurrentHashMap(128 * 1024 /* estimate TODO */);
                //new ConcurrentHashMapUnsafe(128 * 1024 /* estimate TODO */);

        //Caffeine.newBuilder()

//                .removalListener((k,v,cause) -> {
//                    if (cause != RemovalCause.EXPLICIT)
//                        logger.error("{} removal: {},{}", cause, k, v);
//                })

//                .build();
//        tasks.cleanUp();
//
//        this.tasksMap = tasks.asMap();
    }

    public void start(NAR nar) {
        if (Param.DEBUG) {
            int sweepInterval = 32;
            nar.onFrame(nn -> {
                if (nn.time()%sweepInterval == 0)
                    removeDeleted();
            });
        }
    }

    public void removeDeleted() {
        Iterator<Task> ii = tasks.values().iterator();
        while (ii.hasNext()) {
            Task x = ii.next();
            if (x.isDeleted()) {
                logger.error("lingering deleted task: {}", x);
                ii.remove();
            }
        }
    }


    public boolean add(Task x) {


        Task existing = tasks.putIfAbsent(x,x);
        if (existing!=null) {
            DuplicateMerge.merge(existing.budget(), x, 1f);
            x.delete();
            return false;
        } else {
            return true;
        }

    }


    public final void remove(@NotNull Task tt) {
        tasks.remove(tt);
    }

    public final void remove(@NotNull List<Task> tt) {

        int s = tt.size();
        if (s == 0)
            return;

        //synchronized (this.tasks) {
        for (int i = 0; i < s; i++) {
            Task x = tt.get(i);
            tasks.remove(x);
            x.delete();

            /*
            TODO eternalization for non-deleted temporal tasks that reach here:

            float eternalizationFactor = Param.ETERNALIZE_FORGOTTEN_TEMPORAL_TASKS_CONFIDENCE_FACTOR;
            if (eternalizationFactor > 0f && displaced.size() > 0 && eternal.capacity() > 0) {
                eternalizeForgottenTemporals(displaced, nar, eternalizationFactor);
            }
             */

        }


    }

    public void replace(Task in, Task out) {
        add(out);
    }

    public void clear() {
        tasks.clear();
    }

}
