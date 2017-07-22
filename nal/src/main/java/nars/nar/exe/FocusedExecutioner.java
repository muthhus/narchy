package nars.nar.exe;

import jcog.bag.Bag;
import jcog.bag.impl.CurveBag;
import jcog.bag.impl.PriArrayBag;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import nars.$;
import nars.Param;
import nars.Task;
import nars.control.Activate;
import nars.control.Premise;
import nars.task.ITask;
import nars.task.NALTask;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * uses 3 bags/priority queues for a controlled, deterministic
 * selection policy
 * pending inputs
 * pending premises
 * activations
 */
public class FocusedExecutioner extends Executioner {

    final int MAX_PREMISES = 64;
    final int MAX_TASKS = 64;
    final int MAX_CONCEPTS = 64;

    final PriArrayBag<ITask> premises = new PriArrayBag<>(MAX_PREMISES, Param.taskMerge /* TODO make separate premise merge param */, new ConcurrentHashMap<>());

    final PriArrayBag<ITask> tasks = new CurveBag<>(MAX_TASKS, Param.taskMerge, new ConcurrentHashMap<>());

    final PriArrayBag<ITask> concepts = new CurveBag<>(MAX_CONCEPTS, Param.conceptMerge, new ConcurrentHashMap<>());

    int subcycles = 1;
    int subCycleTasks = 8;
    int subCycleConcepts = 2;
    int subCyclePremises = 4;

    final static Logger logger = LoggerFactory.getLogger(FocusedExecutioner.class);

    /** temporary buffer for tasks about to be executed */
    private final List<ITask> next = $.newArrayList(1024);

    @Override
    public void cycle() {

        tasks.commit();
        premises.commit();
        concepts.commit();

        if (Param.TRACE) {
            System.out.println("tasks=" + tasks.size() + " concepts=" + concepts.size() + " premises=" + premises.size());
            if (!concepts.isEmpty())
                concepts.print();
            if (tasks.isEmpty())
                logger.warn("no tasks");
            if (concepts.isEmpty())
                logger.warn("no concepts");
        }




        Consumer<? super PriReference<ITask>> queueTask = x -> next.add(x.get());

        for (int i = 0; i < subcycles; i++) {

            //if (tasks.capacity() <= tasks.size())

            final int[] maxTasks = {subCycleTasks};
            tasks.sample((x) -> {
                NALTask tt = (NALTask) x.get();
                next.add(tt);
                boolean save = false; // tt.isInput();
                return --maxTasks[0] > 0 ?
                        (save ? Bag.BagSample.Next : Bag.BagSample.Remove)
                        :
                        (save ? Bag.BagSample.Stop : Bag.BagSample.RemoveAndStop);
            });

            execute(next);

            concepts.sample(subCycleConcepts, (Consumer) queueTask);

            execute(next);

            premises.pop(subCyclePremises, queueTask);

            execute(next);
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
            x.run(nar);
        } catch (Throwable e) {
            logger.error("exe {} {}", x, e /*(Param.DEBUG) ? e : e.getMessage()*/);
            x.delete();
            return;
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
        //TODO premises as ITask's?
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
    public void run(@NotNull ITask input) {
        PLink p = new PLink(input, input.priElseZero());
        if (input instanceof Task) {
            tasks.putAsync(p);
        } else if (input instanceof Premise) {
            premises.putAsync(p);
        } else if (input instanceof Activate) {
            concepts.putAsync(p);
        } else
            throw new UnsupportedOperationException("what is " + input);
    }

}
