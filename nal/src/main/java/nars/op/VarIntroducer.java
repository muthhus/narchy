package nars.op;

import nars.$;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.task.GeneratedTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

import static nars.term.Terms.compoundOrNull;


/**
 * generalized variable introduction implemented as input preprocessor stage
 */
public class VarIntroducer implements Consumer<Task> {

    private static final Logger logger = LoggerFactory.getLogger(VarIntroducer.class);

    final static String tag = VarIntroducer.class.getSimpleName();

    final int introductionThreshold = 0;

    private final NAR nar;


    public VarIntroducer(NAR n) {
        this.nar = n;
        n.onTask(this);
    }


    @Override public void accept(Task task) {
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
        if (c != null) {

            Task derived = new GeneratedTask(c, original.punc(), original.truth())
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


}
