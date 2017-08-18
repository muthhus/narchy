package nars.exe;

import jcog.Util;
import jcog.bag.Bag;
import jcog.bag.impl.ConcurrentCurveBag;
import jcog.data.FloatParam;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.control.Derivation;
import nars.derive.Deriver;
import nars.derive.PrediTerm;
import nars.task.ITask;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMapUnsafe;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

import static jcog.bag.Bag.BagSample;
import static jcog.bag.Bag.BagSample.*;

/**
 * probabalistic continuation kernel
 */
public class UnifiedExec extends Exec {

    Bag<ITask, ITask> plan;

    int workRemaining = 0;

    @Override
    protected synchronized void clear() {
        plan.clear();
    }

    @Override
    public void add(@NotNull ITask input) {
        plan.put(input);
    }


    public static final Logger logger = LoggerFactory.getLogger(UnifiedExec.class);

    private BagSample exec(ITask x) {
        Iterable<? extends ITask> next = null;

        try {
            next = x.run(nar);
        } catch (Exception e) {
            logger.error("{} {}", x, e);
        }

        boolean keep;
        if (next == null) {
            keep = false;
        } else {
            next.forEach(this::add);
            keep = true;
        }

        if (done())
            return keep ? Stop : RemoveAndStop;
        else
            return keep ? Next : Remove;
    }

    private boolean done() {
        //realtime: System.currentTimeMillis() > nextCycle

        //iterative:
        return --workRemaining > 0;
    }

    @Override
    public synchronized void start(NAR nar) {
        super.start(nar);
        plan = new ConcurrentCurveBag(PriMerge.plus, new ConcurrentHashMapUnsafe<>(1024), nar.random(), 1024);
    }

    @Override
    public synchronized void stop() {
        if (plan != null) {
            plan.clear();
            plan = null;
        }
    }


    @Override
    public int concurrency() {
        return 1;
    }


    @Override
    public Stream<ITask> stream() {
        return plan.stream();
    }

//    public static void main(String... args) {
//        NARS n = NARS.realtime();
//        n.exe(new Execontinue());
//
////        new Loop(1f) {
////
////            @Override
////            public boolean next() {
////                System.out.println();
////                System.out.println( Joiner.on(" ").join(exe.plan) );
////                return true;
////            }
////        };
//
//        NAR nn = n.get();
//
//        try {
//            nn.log();
//            nn.input("a:b. b:c. c:d.");
//        } catch (Narsese.NarseseException e) {
//            e.printStackTrace();
//        }
//
//    }


}
