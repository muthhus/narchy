package nars.op;

import nars.$;
import nars.NAR;
import nars.Task;
import nars.bag.impl.CurveBag;
import nars.budget.merge.BudgetMerge;
import nars.concept.Concept;
import nars.op.time.BagBuffer;
import nars.task.GeneratedTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.truth.Truth;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

import static nars.util.Texts.n2;


/**
 * generalized variable introduction implemented as input preprocessor stage
 */
public class VarIntroducer extends BagBuffer<Task> {

    private static final Logger logger = LoggerFactory.getLogger(VarIntroducer.class);

    final static String tag = VarIntroducer.class.getSimpleName();

    final int introductionThreshold = 0;

    private final NAR nar;
    private final int maxTasksPerFrame;


    public VarIntroducer(NAR n, int capacity, int maxTasksPerFrame) {
        super(new CurveBag(
                new CurveBag.NormalizedSampler(CurveBag.power2BagCurve, n.random),
                BudgetMerge.plusBlend,
                new ConcurrentHashMap<>()
        ));
        this.nar = n;
        setCapacity(capacity);
        this.maxTasksPerFrame = maxTasksPerFrame;
        n.onTask(task -> {
            if (task instanceof VarIntroducedTask)
                return; //HACK dont process its own feedback
            put(task);
        });
        n.onFrame(this::next);
    }

    /** next iteration, each frame */
    protected void next(NAR n) {
        int tRemain = maxTasksPerFrame;
        int tRemainStart = tRemain;
        long start = System.currentTimeMillis();

        bag.commit(); //prepare items for sampling

        while (tRemain > 0) {
            Task t = take();
            if (t == null)
                break; //bag should be empty here

            process(t);
            tRemain--;
        }



        int processed = tRemainStart - tRemain;
        if (processed > 0) {

            bag.commit(); //clear space until next cycle

            long end = System.currentTimeMillis();
            float time = (end-start)/1000f;

            logger.info("buffer size=" + bag.size() + "/cap=" + bag.capacity() + " -=> " + processed + " in " + n2(time) + " sec");
        }
    }


    public void process(Task task) {
        Compound c = task.term();

        //size limitations
        if ((c.size()) < 2 || (c.volume() + introductionThreshold > nar.compoundVolumeMax.intValue()))
            return;

        try {

            Compound a = c, b;

            int i = 0;
            do {
                b = c;
                c = introduceNextVariable(c, i++);
            } while (b!=c);

            if (a!=c) {
                //introduction changed something
                Task newTask = inputCloned(task, c);

//            System.out.println(a + " ====> " + c);
//            System.out.println("\t" + task + " ====> " + newTask);
//            System.out.println();
            }

        } catch (Exception e) {
            //if (Param.DEBUG)
            logger.error("{}", e.toString());
            e.printStackTrace();
        }
    }


    private Compound introduceNextVariable(Compound c, int iteration) {


        Term target = Terms.substMaximal(c, this::canIntroduce, 2, 3);
        if (target != null) {
            Term var = //$.varIndep("c"); //use indep if the introduction spans BOTH subj and predicate of any statement (even recursive)
                    $.varDep("c" + iteration); //otherwise use dep

            Term newContent = $.terms.replace(c, target, var);
            if ((newContent instanceof Compound) && !newContent.equals(c))
                return (Compound) newContent; //success
        }

        return c;
    }


    private boolean canIntroduce(Term subterm) {
        return !subterm.op().var;
    }

    @Nullable protected Task inputCloned(@NotNull Task original, @NotNull Term newContent) {

        Compound c = nar.normalize((Compound) newContent);
        if (c != null && !c.equals(original.term())) {

            Task derived = new VarIntroducedTask(c, original.punc(), original.truth())
                    .time(original.creation(), original.occurrence())
                    .evidence(original.evidence())
                    .budget(original.budget())
                    .log(tag);

            Concept dc = nar.input(derived);
            if (dc!=null) {
                //input successful
                dc.crossLink(derived, original, derived.isBeliefOrGoal() ? derived.conf() : derived.qua(), nar);
                return derived;
            }

        }

        return null;
    }

    public static final class VarIntroducedTask extends GeneratedTask {

        public VarIntroducedTask(Compound c, char punc, @Nullable Truth truth) {
            super(c, punc, truth);
        }
    }

}
