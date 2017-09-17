package nars.exe;

import jcog.bag.Bag;
import jcog.bag.impl.hijack.PriorityHijackBag;
import jcog.sort.TopN;
import nars.NAR;
import nars.control.Activate;
import nars.control.Premise;
import nars.task.ITask;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.stream.Stream;

import static jcog.bag.Bag.BagSample;
import static jcog.bag.Bag.BagSample.*;

/**
 * unified executor
 * probabalistic continuation kernel
 */
public class UniExec extends Exec implements Runnable {

    public static final int BATCH_SIZE = 256;
    public static final int CAPACITY = 16*1024+1;

    Bag<ITask, ITask> plan;

    int workRemaining;

    float activationFactor = 0.05f;
    float premiseFactor = 0.5f;

    @Override
    protected synchronized void clear() {
        plan.clear();
    }

    @Override
    public void add(@NotNull ITask input) {
//        if (nar != null && input.isInput())
//            execute(input);
//        else
            plan.putAsync(input);
    }


    public static final Logger logger = LoggerFactory.getLogger(UniExec.class);

    private BagSample execute(ITask x) {
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

        plan =
                new PriorityHijackBag<>(CAPACITY,2) {

                    @Override
                    public void setCapacity(int _newCapacity) {
                        super.setCapacity(_newCapacity);
                        resize(capacity());
                    }

                    @Override
                    public ITask key(ITask value) {
                        return value;
                    }

                    @Override
                    protected boolean replace(float incoming, float existing) {
                        return hijackGreedy(incoming, existing);
                    }

                    @Override
                    public float pri( ITask key) {
                        return UniExec.this.pri(key);
                    }

                    @Override
                    protected ITask merge(@NotNull ITask existing, @NotNull ITask incoming, MutableFloat overflowing) {
                        float overflow = UniExec.this.merge(existing, incoming); //modify existing
                        if (overflow > 0) {
                            //pressurize(-overflow);
                            if (overflowing != null) overflowing.add(overflow);
                        }
                        return existing;
                    }

                    @Override
                    public void onRemove(@NotNull ITask value) {
//                        if (value instanceof Task) {
//                            ignore((Task) value);
//                        }
                    }

                    @Override
                    public void onReject(@NotNull ITask value) {
//                        if (value instanceof Task) {
//                            ignore((Task) value);
//                        }
                    }

                    @Override
                    protected Consumer<ITask> forget(float rate) {
                        return null;
//                        return new PriForget(rate) {
//                            @Override
//                            public void accept(@NotNull Priority b) {
//                                if (b instanceof Activate || b instanceof )
//                                super.accept(b);
//                            }
//                        };
                    }
                };
//            new ConcurrentCurveBag(this,
//                new ConcurrentHashMapUnsafe<>(1024), nar.random(), 1024);

        super.start(nar);
    }

    public float pri(ITask key) {
        if (key instanceof Activate) {
            float v = key.pri(); if (v!=v) return Float.NaN;
            return activationFactor * v;
        } else if (key instanceof Premise) {
            float v = key.pri(); if (v!=v) return Float.NaN;
            return premiseFactor * v;
        }

        return key.pri();
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

    @Override
    public synchronized void run() {
        workRemaining = BATCH_SIZE;
        plan/*.commit()*/.sample(this::top);
    }

    final int WINDOW_SIZE = 32;
    final int WINDOW_RATIO = 8;
    private final TopN<ITask> top = new TopN<>(new ITask[WINDOW_SIZE], this::pri);
    int windowTTL = WINDOW_RATIO;

    private BagSample top(ITask x) {

        top.add(x);

        if (--windowTTL <= 0) {
            windowTTL = WINDOW_RATIO;
            ITask t = top.pop();
            if (t!=null)
                execute(t);
        }

        return BagSample.Next;
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
