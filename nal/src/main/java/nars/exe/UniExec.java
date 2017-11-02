package nars.exe;

import jcog.bag.Bag;
import jcog.bag.impl.ConcurrentCurveBag;
import jcog.bag.impl.CurveBag;
import jcog.exe.Can;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.concept.Concept;
import nars.control.Activate;
import nars.task.ITask;

import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * unified executor
 * concurrent, thread-safe. one central concept bag
 *
 */
public class UniExec extends Exec {

    private final int CAPACITY;

    protected Bag<Activate, Activate> active;

    public UniExec(int capacity) {

        CAPACITY = capacity;
    }

    @Override
    protected synchronized void clear() {
        if (active !=null)
            active.clear();
    }

    @Override
    public void activate(Concept c, float activationApplied) {
        active.putAsync(new Activate(c, activationApplied * nar.conceptActivation.floatValue()));
    }

    @Override
    public void add(ITask t) {
        execute(t);
    }

    @Override
    public void fire( Predicate<Activate> each) {
        active.sample( each);
    }

    @Override
    public synchronized void start(NAR nar) {

        active =
            concurrent() ?
                new ConcurrentCurveBag<>(PriMerge.plus, new HashMap<>(), nar.random(), CAPACITY)
                    //new ConcurrentArrayBag<ITask,ITask>(this, new ConcurrentHashMap(), CAPACITY) {
                        :
                new CurveBag<>(PriMerge.plus, new HashMap(), nar.random(), CAPACITY);

        super.start(nar);
    }


    @Override
    public void cycle(List<Can> can) {
        super.cycle(can);

        active.commit(active.forget(nar.forgetRate.floatValue()));
    }

    @Override
    public synchronized void stop() {
        if (active != null) {
            active.clear();
        }
    }

    @Override
    public int concurrency() {
        return 1;
    }

    @Override
    public Stream<Activate> active() {
        return active.stream(); //.filter(Objects::nonNull);
    }

//    final int WINDOW_SIZE = 32;
//    final int WINDOW_RATIO = 8;
//    private final TopN<ITask> top = new TopN<>(new ITask[WINDOW_SIZE], this::pri);
//    int windowTTL = WINDOW_RATIO;
//
//    private BagSample top(ITask x) {
//
//        top.add(x);
//
//        if (--windowTTL <= 0) {
//            windowTTL = WINDOW_RATIO;
//            ITask t = top.pop();
//            if (t!=null)
//                exeSample((ITask) t);
//        }
//
//        return BagSample.Next;
//    }

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
