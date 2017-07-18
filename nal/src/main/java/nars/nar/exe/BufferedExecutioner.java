package nars.nar.exe;

import com.google.common.base.Joiner;
import jcog.bag.Bag;
import jcog.bag.impl.HijackBag;
import jcog.bag.impl.hijack.PriorityHijackBag;
import jcog.data.FloatParam;
import jcog.math.MultiStatistics;
import jcog.math.RecycledSummaryStatistics;
import jcog.pri.Pri;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.task.ITask;
import nars.task.NALTask;
import nars.truth.Truthed;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static nars.Op.BELIEF;
import static nars.Op.COMMAND;

/**
 * Buffers all executions between each cycle in order to remove duplicates
 */
public class BufferedExecutioner extends Executioner {

    @Deprecated int inputBatch = 32;
    @Deprecated int fireBatch = 16;


    //    private final DisruptorBlockingQueue<ITask> overflow;
    protected boolean trace;

    /**
     * if < 0, executes them all. 0 pauses, and finite value > 0 will cause them to be sorted first if the value exceeds the limit
     * interpreted as its integer value, although currently it is FloatParam
     */
    public final FloatParam conceptsPerCycleMax = new FloatParam();

//    /**
//     * temporary collection of tasks to remove after sampling
//     */
//    protected final FasterList<ITask> toRemove = new FasterList();



    public final Bag<ITask, ITask> tasks =
            new PriorityHijackBag<ITask, ITask>(4) {
                @Override
                protected Consumer<ITask> forget(float rate) {
                    return null;
                }

                @Override
                protected ITask merge(@NotNull ITask existing, @NotNull ITask incoming, @Nullable MutableFloat overflowing) {
                    Param.taskMerge.merge(existing, incoming);
                    return existing;
                }

                @Override
                public ITask key(@NotNull ITask value) {
                    return value;
                }
            };
//            new ArrayBag<>(0, PriMerge.max, new ConcurrentHashMap<>()) {
//
//        @Override
//        public float floatValueOf(ITask x) {
//            return x.pri();
//        }
//
//        @Nullable
//        @Override
//        public ITask key(@NotNull ITask l) {
//            return l.ref;
//        }
//    };


    /**
     * active tasks
     */
    public final Bag<ITask, ITask> concepts =
//            new ArrayBag<>(PriMerge.plus, new ConcurrentHashMap<>()) {


            new PriorityHijackBag<>(4) {
                @Override
                protected final Consumer<ITask> forget(float rate) {
                    return null;
                }


                @Override
                protected ITask merge(@NotNull ITask existing, @NotNull ITask incoming, @Nullable MutableFloat overflowing) {
                    Param.conceptMerge.merge(existing, incoming);
                    return existing;
                }

                @Override
                public Bag<ITask, ITask> commit() {
                    return this; //do nothing
                }

                @NotNull
                @Override
                public HijackBag commit(Consumer c) {
                    return this; //do nothing
                }

//                @Override
//                public ITask put(@NotNull ITask x) {
//                    ITask y = super.put(x);
////                    if (y == null) {
////                        overflow.offer(x);
////                    }
//                    return y;
//                }

//                @Override
//                public void onRemoved(@NotNull ITask value) {
//
//                    //DO NOTHING, DONT DELETE
//
////                    if (value.priElseZero() >= Pri.EPSILON) {
////                        if (overflow.remainingCapacity() < 1) {
////                            overflow.poll(); //forget
////                        }
////                        overflow.offer(value); //save
////                    } else {
////                        ITask x = overflow.poll();
////                        if (x != null && x.priElseZero() >= Pri.EPSILON)
////                            put(x); //restore
////                    }
//                }
//

                @NotNull
                @Override
                public final ITask key(ITask value) {
                    return value;
                }


            };

    //final DecideRoulette<ITask> activeBuffer = new DecideRoulette<>(CLink::priElseZero);

    private float forgetEachActivePri;


    public BufferedExecutioner(int conceptCapacity, int taskCapacity) {
        this(conceptCapacity, taskCapacity, 1f);
    }

    public BufferedExecutioner(int conceptCapacity, int taskCapacity, float executedPerCycle) {
        concepts.setCapacity(conceptCapacity);
        tasks.setCapacity(taskCapacity);
        conceptsPerCycleMax.setValue(Math.ceil(conceptCapacity * executedPerCycle));
    }

    @Override
    public void cycle(@NotNull NAR nar) {
        //flush();

        nar.eventCycleStart.emit(nar);

        flush();
    }

    @Override
    public int concurrency() {
        return 1;
    }

    @Override
    public void stop() {
        flush();
        super.stop();
    }


    @Override
    public void start(NAR nar) {
        super.start(nar);
        flush(); //<- may not be necessary
    }



//    @Override
//    public void stop() {
//        flush();
//        super.stop();
//    }

    final AtomicBoolean busy = new AtomicBoolean(false);


    @Override
    public void forEach(Consumer<ITask> each) {
        tasks.forEachKey(each);
        concepts.forEachKey(each);
    }


    @Override
    public void runLater(Runnable r) {
        r.run(); //default to synchronous execution
    }


    protected void flush() {
        if (!busy.compareAndSet(false, true))
            return;

        //System.out.println(getClass() + " flush " + nal.size() + " " + active.size());

        try {




            boolean t = this.trace;
            if (t)
                concepts.print();

            final int[] toFire = {conceptsPerCycleMax.intValue()};


            //Random rng = nar.random();


            //EXEC
            float eFrac = ((float) toFire[0]) / concepts.capacity();
            float pAvg = (1f /*PForget.DEFAULT_TEMP*/) * ((HijackBag) concepts).depressurize(eFrac) * (eFrac);
            this.forgetEachActivePri =
                    pAvg >= Pri.EPSILON ? pAvg : 0;


            concepts.commit();

            do {


                concepts.sample(fireBatch, x -> {

                    execute(x);


                    if (forgetEachActivePri > 0) {
                        x.priSub(forgetEachActivePri);
                    }

                    --toFire[0];

                    //activeBuffer.add(x);
                    //(Consumer<? super ITask>)(buffer::add)
                });


                //input a batch of tasks
                tasks.pop(inputBatch, this::execute);


            } while (--toFire[0] /* decrement to fire if nothing else than to prevent infinite stall */ > 0 /*|| toInput[0] > 0*/);


//            for (int i = 0; i < toExe; i++) {
//                @Nullable ITask x = activeBuffer.decide(rng);
//                actuallyRun(x);
//                if (forgetEachActivePri > 0) {
//                    x.priSub(forgetEachActivePri);
//                }
//            }

            //active.sample(toExe, this::actuallyRun);

//            if (!toRemove.isEmpty()) {
//                toRemove.clear(active::remove);
//            }

//            } else {
//                //sort
//                if (sorted == null || sorted.capacity() != (toExe + 1)) {
//                    sorted = new SortedArray<ITask>(new ITask[toExe + 1]);
//                }
//                pending.sample(pending.capacity(), s -> {
//                    sorted.add(s, Prioritized::oneMinusPri);
//                    if (sorted.size() > toExe)
//                        sorted.removeLast();
//                });
//                assert (sorted.size() == toExe);
//                sorted.forEach(this::actuallyRun);
//            }

            //Runtime.getRuntime().runFinalization();

        } finally {
            busy.set(false);
        }
    }

    protected void execute(ITask x) {
        ITask[] next;
        try {
            if (x == null) return; //HACK

            if (x.isDeleted()) {
                next = null;
            } else {
                next = x.run(nar);
            }

        } catch (Throwable e) {
            NAR.logger.error("exe {} {}", x, e /*(Param.DEBUG) ? e : e.getMessage()*/);
            x.delete();
            return;
        }

        if (next == ITask.DeleteMe) {
            x.delete();
        }

        actuallyFeedback(x, next);
    }

    protected void actuallyFeedback(ITask parent, @Nullable ITask[] children) {
        if (children != null && children.length > 0)
            nar.input(children);
    }


    @Override
    public void run(@NotNull ITask input) {

        boolean nal = input instanceof NALTask;

        if ((busy.get()==false && nal)|| input.punc() == COMMAND) {
            //if not busy, input NAL tasks directly. this allows direct insertion of tasks while the reasoner is paused
            //commands executed immediately
            execute(input);
        } else {
            (nal ? tasks : concepts).putAsync(input);
        }
    }


    public CharSequence stats() {

        RecycledSummaryStatistics pri = new RecycledSummaryStatistics();

        ObjectFloatHashMap<Class<? extends ITask>> typeToPri = new ObjectFloatHashMap();
        //.value("pri", x -> x.priElseZero());


        MultiStatistics<NALTask> beliefs = new MultiStatistics<NALTask>()
                .value("pri", Task::pri)
                .value("freq", Truthed::freq)
                .value("conf", Truthed::conf);

        concepts.forEachKey(x -> {
            float p = x.pri();
            if (p != p)
                return;
            typeToPri.addToValue(x.getClass(), p);
            if (x.punc() == BELIEF) {
                beliefs.accept((NALTask) x);
            }
            pri.accept(p);
        });

        //.classify("type", x -> x.getClass().toString()

        return Joiner.on("\n").join(typeToPri, beliefs, pri);
    }
}
