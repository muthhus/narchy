//package nars.exe;
//
//import com.google.common.base.Joiner;
//import jcog.bag.Bag;
//import jcog.bag.impl.hijack.PriorityHijackBag;
//import jcog.data.FloatParam;
//import jcog.event.On;
//import jcog.math.MultiStatistics;
//import jcog.math.RecycledSummaryStatistics;
//import jcog.pri.Pri;
//import nars.NAR;
//import nars.Param;
//import nars.Task;
//import nars.control.Activate;
//import nars.task.ITask;
//import nars.task.NALTask;
//import nars.truth.Truthed;
//import org.apache.commons.lang3.mutable.MutableFloat;
//import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.io.PrintStream;
//import java.util.Random;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.function.Consumer;
//
//import static nars.Op.BELIEF;
//import static nars.Op.COMMAND;
//
///**
// * Buffers all executions between each cycle in order to remove duplicates
// */
//public class BufferedExecutioner extends Executioner {
//
//    /**
//     * number of tasks input after each firing;
//     * defines an input load processing ratio
//     * TODO make this automatically controlled according to the
//     * input task load that occurred while it was firing
//     */
//    int inputsPerFire = 8;
//
//    final AtomicBoolean busy = new AtomicBoolean(false);
//
//
//    //    private final DisruptorBlockingQueue<ITask> overflow;
//    protected boolean trace;
//
//    /**
//     * if < 0, executes them all. 0 pauses, and finite value > 0 will cause them to be sorted first if the value exceeds the limit
//     * interpreted as its integer value, although currently it is FloatParam
//     */
//    public final FloatParam conceptsPerCycleMax = new FloatParam();
//
////    /**
////     * temporary collection of tasks to remove after sampling
////     */
////    protected final FasterList<ITask> toRemove = new FasterList();
//
//
//    public final Bag<Task, Task> tasks =
//            new PriorityHijackBag<>(4) {
//                @Override
//                protected Consumer<Task> forget(float rate) {
//                    return null;
//                }
//
//                @Override
//                protected Task merge(@NotNull Task existing, @NotNull Task incoming, @Nullable MutableFloat overflowing) {
//                    Param.taskMerge.merge(existing, incoming);
//                    ((NALTask) existing).merge(incoming);
//                    return existing;
//                }
//
//                @Override
//                public Task key(@NotNull Task value) {
//                    return value;
//                }
//
//                @Override
//                protected Random random() {
//                    return rng;
//                }
//            };
//
//
//    /**
//     * active tasks
//     */
//    public final Bag<ITask, ITask> concepts =
////            new CurveBag(0, Param.conceptMerge, new ConcurrentHashMap<>()) {
//
//            new PriorityHijackBag<>(4) {
//                @Override
//                protected final Consumer<ITask> forget(float rate) {
//                    return null;
//                }
//
//                @Override
//                protected ITask merge(@NotNull ITask existing, @NotNull ITask incoming, @Nullable MutableFloat overflowing) {
//                    Param.conceptMerge.merge(existing, incoming);
//                    return existing;
//                }
//
//                @NotNull
//                @Override
//                public final ITask key(ITask value) {
//                    return value;
//                }
//
//
////                @Override
////                public Bag<ITask, ITask> commit() {
////                    return this; //do nothing
////                }
////
////                @NotNull
////                @Override
////                public HijackBag commit(Consumer c) {
////                    return this; //do nothing
////                }
//
//
//                @Override
//                protected Random random() {
//                    return rng;
//                }
//            };
//    private Random rng;
//    private On onClear;
//
//    //final DecideRoulette<ITask> activeBuffer = new DecideRoulette<>(CLink::priElseZero);
//
//
//    public BufferedExecutioner(int conceptCapacity, int taskCapacity) {
//        this(conceptCapacity, taskCapacity, 1f);
//    }
//
//    public BufferedExecutioner(int conceptCapacity, int taskCapacity, float executedPerCycle) {
//        concepts.setCapacity(conceptCapacity);
//        tasks.setCapacity(taskCapacity);
//        conceptsPerCycleMax.setValue(executedPerCycle);
//    }
//
//    @Override
//    public void cycle() {
//
//        if (!busy.compareAndSet(false, true))
//            return;
//
//        try {
//
//            boolean t = this.trace;
//            if (t)
//                concepts.print();
//
//            final int toFire =
//                    (int) Math.ceil(conceptsPerCycleMax.floatValue() * concepts.capacity());
//
//            float eFrac = ((float) toFire) / concepts.capacity();
//            float pAvg = (1f /*PForget.DEFAULT_TEMP*/) * concepts.depressurize(eFrac) * (eFrac);
//            float forgetEachActivePri =
//                    pAvg >= Pri.EPSILON ? pAvg : 0;
//
//
//            tasks.pop(inputsPerFire, this::execute); //pre-fire inputs
//
//            concepts.commit(null).sample(toFire, x -> {
//
//                execute(x);
//
//                if (forgetEachActivePri > 0) {
//                    x.priSub(forgetEachActivePri);
//                }
//
//                tasks.pop(inputsPerFire, this::execute);
//
//                //activeBuffer.add(x);
//                //(Consumer<? super ITask>)(buffer::add)
//            });
//
//        } finally {
//            busy.set(false);
//        }
//    }
//
//    @Override
//    public int concurrency() {
//        return 1;
//    }
//
//    @Override
//    public synchronized void stop() {
//
//        On c = this.onClear;
//        if (c !=null) {
//            this.onClear = null;
//            c.off();
//        }
//
//        super.stop();
//
//    }
//
//
//    @Override
//    public synchronized void start(NAR nar) {
//        super.start(nar);
//
//        this.rng = nar.random();
//        this.onClear = nar.eventClear.on((n) -> {
//            tasks.clear();
//            concepts.clear();
//        });
//
//    }
//
//    @Override
//    public void print(PrintStream out) {
//        System.out.println("Concepts");
//        concepts.print();
//        concepts.forEach(x -> ((Activate)x).get().print());
//        System.out.println("\nTasks");
//        tasks.print();
//    }
//
//
//
//
//    @Override
//    public void forEach(Consumer<ITask> each) {
//        tasks.forEach(each);
//        concepts.forEach(each);
//    }
//
//
//
//    @Override
//    public void runLater(Runnable r) {
//        //pendingRuns.add(r);
//        r.run(); //default: inline
//    }
//
//    @Override
//    public void runLaterAndWait(Runnable r) {
//        r.run(); //default: inline
//    }
//
//
//    protected void execute(ITask x) {
//        ITask[] next;
//        try {
//            if (x == null) return; //HACK
//
//            if (x.isDeleted()) {
//                next = null;
//            } else {
//                next = x.run(nar);
//            }
//
//        } catch (Throwable e) {
//            NAR.logger.error("exe {} {}", x, e /*(Param.DEBUG) ? e : e.getMessage()*/);
//            x.delete();
//            return;
//        }
//
//        if (next == ITask.DeleteMe) {
//            x.delete();
//        }
//
//        actuallyFeedback(x, next);
//    }
//
//    protected void actuallyFeedback(ITask parent, @Nullable ITask[] children) {
//        if (children != null && children.length > 0)
//            nar.input(children);
//    }
//
//
//    @Override
//    public void run(@NotNull ITask input) {
//
//        if (input.isDeleted())
//            return; //TODO track these
//
//        boolean nal = input instanceof NALTask;
//
//        if ((nal && !busy.get()) || input.punc() == COMMAND) {
//            //if not busy, input NAL tasks directly. this allows direct insertion of tasks while the reasoner is paused
//            //commands executed immediately
//            execute(input);
//        } else {
//            if (nal) tasks.putAsync((NALTask) input);
//            else concepts.putAsync(input);
//        }
//    }
//
//
//    public CharSequence stats() {
//
//        RecycledSummaryStatistics pri = new RecycledSummaryStatistics();
//
//        ObjectFloatHashMap<Class<? extends ITask>> typeToPri = new ObjectFloatHashMap();
//        //.value("pri", x -> x.priElseZero());
//
//
//        MultiStatistics<NALTask> beliefs = new MultiStatistics<NALTask>()
//                .value("pri", Task::pri)
//                .value("freq", Truthed::freq)
//                .value("conf", Truthed::conf);
//
//        concepts.forEachKey(x -> {
//            float p = x.pri();
//            if (p != p)
//                return;
//            typeToPri.addToValue(x.getClass(), p);
//            if (x.punc() == BELIEF) {
//                beliefs.accept((NALTask) x);
//            }
//            pri.accept(p);
//        });
//
//        //.classify("type", x -> x.getClass().toString()
//
//        return Joiner.on("\n").join(typeToPri, beliefs, pri);
//    }
//}
