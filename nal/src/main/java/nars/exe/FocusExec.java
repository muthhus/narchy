package nars.exe;

import jcog.bag.Bag;
import jcog.bag.impl.ConcurrentCurveBag;
import jcog.bag.impl.CurveBag;
import jcog.list.FasterList;
import jcog.random.XorShift128PlusRandom;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.control.Activate;
import nars.control.CycleService;
import nars.control.NARService;
import nars.control.Premise;
import nars.task.ITask;
import nars.task.NALTask;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMapUnsafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * uses 3 bags/priority queues for a controlled, deterministic
 * selection policy
 * pending inputs
 * pending premises
 * activations
 */
public class FocusExec extends Exec implements Runnable {


    public int subCycles = 1;
    final int subCycleConcepts = 1;
    final int subCyclePremises = subCycleConcepts * 3;
    final int subCycleTasks = subCyclePremises * 3;

    final int MAX_PREMISES = subCyclePremises * 2;
    final int MAX_TASKS = subCycleTasks * 4;
    final int MAX_CONCEPTS = 16;

    final Random random = new XorShift128PlusRandom(1);

    final CurveBag<Premise> premises = new ConcurrentCurveBag<>(Param.premiseMerge /* TODO make separate premise merge param */,
            new ConcurrentHashMap<>(), null, MAX_PREMISES);

    final CurveBag<Task> tasks = new ConcurrentCurveBag<>(Param.taskMerge, new ConcurrentHashMap<>(),
            null, MAX_TASKS) {
        @Override
        public void onRemove(@NotNull Task value) {
            ignore(value);
        }

        @Override
        public void onReject(@NotNull Task value) {
            ignore(value);
        }
    };



    public final Bag concepts =
            new ConcurrentCurveBag<>(Param.conceptActivate,
                    //new ConcurrentHashMap<>(),
                    new ConcurrentHashMapUnsafe<>(),
                    random, MAX_CONCEPTS);
    //new DefaultHijackBag(Param.conceptMerge, MAX_CONCEPTS, 3);


    final static Logger logger = LoggerFactory.getLogger(FocusExec.class);

    /**
     * temporary buffer for tasks about to be executed
     */
    private final FasterList<ITask> next = new FasterList(1024);

    @Nullable
    private NARService trigger;

    public FocusExec() {


    }

    @Override
    protected synchronized void clear() {
        next.clear();
        premises.clear();
        tasks.clear();
        concepts.clear();
    }

    @Override
    public synchronized void start(NAR nar) {
        super.start(nar);

        trigger = newTrigger();
    }

    @Override
    public synchronized void stop() {
        if (trigger != null) {
            nar.remove(trigger.term());
            trigger = null;
        }
        super.stop();
    }


    @Nullable
    protected NARService newTrigger() {
        return new MyTrigger();
    }

    /**
     * run an iteration
     */
    @Override
    public void run() {

        if (Param.TRACE) {
            System.out.println("tasks=" + tasks.size() + " concepts=" + concepts.size() + " premises=" + premises.size());
            if (!concepts.isEmpty())
                concepts.print();
            if (tasks.isEmpty())
                logger.warn("no tasks");
            if (concepts.isEmpty())
                logger.warn("no concepts");
        }


        for (int i = 0; i < subCycles; i++) {

            //if (tasks.capacity() <= tasks.size())

            final int[] maxTasks = {subCycleTasks};

//            System.out.println(nar.time());
//            tasks.print();
//            System.out.println();

            tasks.commit(null).sample((x) -> {
                NALTask tt = (NALTask) x;
                next.add(tt);
                boolean save =
                        //tt.isInput();
                        false;
                return --maxTasks[0] > 0 ?
                        (save ? Bag.BagSample.Next : Bag.BagSample.Remove)
                        :
                        (save ? Bag.BagSample.Stop : Bag.BagSample.RemoveAndStop);
            });

            execute(next);

            concepts.commit().sample(subCycleConcepts, (Predicate<ITask>) (next::add));

            execute(next);

            //execute the next set of premises
            premises.commit().pop(subCyclePremises, this::execute);

        }
    }


    public void execute(List<ITask> next) {
        if (!next.isEmpty()) {
            next.forEach(this::execute);
            next.clear();
        }
    }

    protected void execute(ITask x) {
        try {

            Iterable<? extends ITask> y = x.run(nar);
            if (y != null) {
                y.forEach(this::add);
            }

        } catch (UnsupportedOperationException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            if (Param.DEBUG) {
                throw e;
            } else {
                logger.error("exe {} {}", x, e /*(Param.DEBUG) ? e : e.getMessage()*/);
                x.delete();
            }
        }
    }


    @Override
    public int concurrency() {
        return 1;
    }

    @Override
    public Stream<ITask> stream() {
        return Stream.concat(Stream.concat(concepts.stream(),
                premises.stream()
        ), tasks.stream()).filter(Objects::nonNull);
    }


    @Override
    public void add(@NotNull ITask x) {
        if (x instanceof Task) {
            if (x.isInput()) {
                execute(x); //execute immediately
            } else {
                tasks.putAsync((Task) x); //buffer
            }
        } else if (x instanceof Premise) {

            premises.putAsync((Premise)x);

        } else if (x instanceof Activate) {
            concepts.putAsync(x);
        } else
            throw new UnsupportedOperationException("what is " + x);
    }

    private class MyTrigger extends CycleService {
        public MyTrigger() {
            super(FocusExec.this.nar);
        }

        @Override
        public void run(NAR nar) {
            FocusExec.this.run();
        }
    }
}
