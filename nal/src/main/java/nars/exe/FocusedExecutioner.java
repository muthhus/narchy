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
import nars.control.Derivation;
import nars.control.Premise;
import nars.derive.PrediTerm;
import nars.task.ITask;
import nars.task.NALTask;
import nars.term.ProxyTerm;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * uses 3 bags/priority queues for a controlled, deterministic
 * selection policy
 * pending inputs
 * pending premises
 * activations
 */
public class FocusedExecutioner extends Executioner {

    private PrediTerm<Derivation> deriver;
    private final Function<NAR, PrediTerm<Derivation>> deriverBuilder;

    public int subCycles = 2;
    final int subCycleConcepts = 3;
    final int subCycleTasks = 16;
    final int subCyclePremises = 4;

    final int MAX_PREMISES = 64;
    final int MAX_TASKS = 64;
    final int MAX_CONCEPTS = 64;

    final Random random = new XorShift128PlusRandom(1);

    final CurveBag<ITask> premises = new ConcurrentCurveBag<>(Param.premiseMerge /* TODO make separate premise merge param */,
            new ConcurrentHashMap<>(), random, MAX_PREMISES);

    final CurveBag<ITask> tasks = new ConcurrentCurveBag<>(Param.taskMerge, new ConcurrentHashMap<>(),
            random, MAX_TASKS);

    public final CurveBag<Activate> concepts = new ConcurrentCurveBag<>(Param.conceptMerge, new ConcurrentHashMap<>(),
            random, MAX_CONCEPTS);


    final static Logger logger = LoggerFactory.getLogger(FocusedExecutioner.class);

    /**
     * temporary buffer for tasks about to be executed
     */
    private final FasterList<ITask> next = new FasterList(1024);

    public FocusedExecutioner(Function<NAR,PrediTerm<Derivation>> deriverBuilder) {
        this.deriverBuilder = deriverBuilder;
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
        deriver = deriverBuilder.apply(nar);
    }

    @Override
    public void cycle() {


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

            tasks.commit();
            premises.commit();
            concepts.commit();

            //if (tasks.capacity() <= tasks.size())

            final int[] maxTasks = {subCycleTasks};

//            System.out.println(nar.time());
//            tasks.print();
//            System.out.println();

            tasks.sample((x) -> {
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


            concepts.sample(subCycleConcepts, (Predicate<ITask>) (next::add));

            execute(next);

            premises.pop(subCyclePremises, this::execute);

//            System.out.println("PREMISES");
//            next.forEach(System.out::println);
//            System.out.println("/PREMISES");

        }
    }

    protected void execute(Premise p) {
        p.run(nar.derivation(deriver), Math.round(nar.matchTTL.intValue() * (0.5f + 0.5f * p.priElseZero())));
    }

    public void execute(List<ITask> next) {
        if (!next.isEmpty()) {
            next.forEach(this::execute);
            next.clear();
        }
    }

    protected void execute(ITask x) {
        try {
            if (x.isDeleted())
                return;

            if (x instanceof Premise) {
                execute((Premise) x);
            } else if (x instanceof Activate) {
                ((Activate) x).hypothesize(nar).forEach(premises::putAsync);
            } else {
                x.run(nar);
            }
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
    public void forEach(Consumer<ITask> each) {
        concepts.forEachKey(each);
        tasks.forEachKey(each);
        premises.forEachKey(each);
    }

    @Override
    public void runLater(Runnable cmd) {
        cmd.run();
    }

    @Override
    public void runLaterAndWait(Runnable cmd) {
        cmd.run();
    }

    @Override
    public void run(@NotNull ITask x) {
        if (x instanceof Task) {
            if (x.isInput())
                execute(x); //execute immediately
            else
                tasks.putAsync(x); //buffer

        } else if (x instanceof Premise) {
            premises.putAsync(x);
        } else if (x instanceof Activate) {
            concepts.putAsync((Activate) x);
        } else
            throw new UnsupportedOperationException("what is " + x);
    }

}
