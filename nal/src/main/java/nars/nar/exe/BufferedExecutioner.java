package nars.nar.exe;

import com.google.common.base.Joiner;
import jcog.bag.Bag;
import jcog.bag.impl.ArrayBag;
import jcog.bag.impl.CurveBag;
import jcog.bag.impl.HijackBag;
import jcog.bag.impl.hijack.PriorityHijackBag;
import jcog.data.FloatParam;
import jcog.math.MultiStatistics;
import jcog.math.RecycledSummaryStatistics;
import jcog.pri.Pri;
import jcog.pri.op.PriMerge;
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

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static nars.Op.BELIEF;
import static nars.Op.COMMAND;

/**
 * Buffers all executions between each cycle in order to remove duplicates
 */
public class BufferedExecutioner extends Executioner {

    /**
     * number of tasks input after each firing; defines an input load processing ratio
     */
    int inputsPerFire = 32;


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

                @Override
                protected Random random() {
                    return rng;
                }
            };


    /**
     * active tasks
     */
    public final Bag<ITask, ITask> concepts =
            new CurveBag(0, Param.conceptMerge, new ConcurrentHashMap<>()) {

            //new PriorityHijackBag<>(4) {
                //@Override protected final Consumer<ITask> forget(float rate) {
                    //return null;
                //}
//                @Override protected ITask merge(@NotNull ITask existing, @NotNull ITask incoming, @Nullable MutableFloat overflowing) {
//                    Param.conceptMerge.merge(existing, incoming);
//                    return existing;
//                }
//                @NotNull
//                @Override
//                public final ITask key(ITask value) {
//                    return value;
//                }


//                @Override
//                public Bag<ITask, ITask> commit() {
//                    return this; //do nothing
//                }
//
//                @NotNull
//                @Override
//                public HijackBag commit(Consumer c) {
//                    return this; //do nothing
//                }


                @Override
                protected Random random() {
                    return rng;
                }
            };
    private Random rng;

    //final DecideRoulette<ITask> activeBuffer = new DecideRoulette<>(CLink::priElseZero);


    public BufferedExecutioner(int conceptCapacity, int taskCapacity) {
        this(conceptCapacity, taskCapacity, 1f);
    }

    public BufferedExecutioner(int conceptCapacity, int taskCapacity, float executedPerCycle) {
        concepts.setCapacity(conceptCapacity);
        tasks.setCapacity(taskCapacity);
        conceptsPerCycleMax.setValue(executedPerCycle);
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

        this.rng = nar.random();

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

        try {

            boolean t = this.trace;
            if (t)
                concepts.print();

            final int toFire =
                    (int) Math.ceil(conceptsPerCycleMax.floatValue() * concepts.capacity())
            ;

            float eFrac = ((float) toFire) / concepts.capacity();
            float pAvg = (1f /*PForget.DEFAULT_TEMP*/) * ((CurveBag) concepts).depressurize(eFrac) * (eFrac);
            float forgetEachActivePri =
                    pAvg >= Pri.EPSILON ? pAvg : 0;


            tasks.pop(inputsPerFire, this::execute); //pre-fire inputs

            concepts.commit(null).sample(toFire, x -> {

                execute(x);

                if (forgetEachActivePri > 0) {
                    x.priSub(forgetEachActivePri);
                }

                tasks.pop(inputsPerFire, this::execute);

                //activeBuffer.add(x);
                //(Consumer<? super ITask>)(buffer::add)
            });

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

        if ((nal && !busy.get()) || input.punc() == COMMAND) {
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
