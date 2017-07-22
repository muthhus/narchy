package nars.nar.exe;

import com.google.common.collect.MinMaxPriorityQueue;
import jcog.bag.impl.ArrayBag;
import jcog.bag.impl.CurveBag;
import jcog.bag.impl.PriArrayBag;
import jcog.pri.PLink;
import jcog.pri.Pri;
import jcog.pri.PriReference;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.control.Activate;
import nars.control.Premise;
import nars.task.ITask;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** uses 3 bags/priority queues for a controlled, deterministic
 *  selection policy
 *       pending inputs
 *       pending premises
 *       activations
 *
 */
public class FocusedExecutioner extends Executioner {

    final int MAX_PREMISES = 64;
    final int MAX_TASKS = 64;
    final int MAX_CONCEPTS = 64;

    final PriArrayBag<ITask> premises = new PriArrayBag<>(MAX_PREMISES, Param.taskMerge /* TODO make separate premise merge param */, new ConcurrentHashMap<>());

    final PriArrayBag<ITask> tasks = new CurveBag<>(MAX_TASKS, Param.taskMerge, new ConcurrentHashMap<>());

    final PriArrayBag<ITask> concepts = new CurveBag<>(MAX_CONCEPTS, Param.conceptMerge, new ConcurrentHashMap<>());

    int subcycles = 1;

    @Override
    public void cycle(@NotNull NAR nar) {

        //System.out.println("tasks=" + tasks.size() + " concepts=" + concepts.size() + " premises=" + premises.size());

        tasks.commit();
        premises.commit();
        concepts.commit();

        List<ITask> next = $.newArrayList();
        for (int i = 0; i < subcycles; i++) {
            Consumer<? super PriReference<ITask>> queueTask = x -> next.add(x.get());

            //if (tasks.capacity() <= tasks.size())
                tasks.pop(1, queueTask);
            //else
                //tasks.sample(1, queueTask);

            concepts.sample(1, (Consumer) queueTask);

            premises.pop(1, queueTask);

            Consumer<Premise> runPremise = x -> x.run(nar.derivation(), nar.matchTTL.intValue());

            if (!next.isEmpty()) {
                next.forEach(t -> {
                    if (t instanceof Premise) {
                        runPremise.accept((Premise) t);
                    } else {
                        t.run(nar);
                    }
                });
                next.clear();
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
