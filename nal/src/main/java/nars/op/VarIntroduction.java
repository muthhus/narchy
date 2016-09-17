package nars.op;

import nars.$;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.task.GeneratedTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * a generalized variable introduction model that can transform tasks
 */
public abstract class VarIntroduction implements BiConsumer<Task,NAR> {
    final static String tag = MutaTaskBag.class.getSimpleName();
    final int introductionThreshold = 0;
    final int maxIterations;

    public VarIntroduction(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    @Override public final void accept(Task task, NAR nar) {
        Compound c = task.term();

        //size limitations
        if ((c.size()) < 2 || (c.volume() + introductionThreshold > nar.compoundVolumeMax.intValue()))
            return;


        Compound a = c, b;

        int i = 0;
        do {
            b = c;
            c = introduceNextVariable(c, i++);
        } while ((i < maxIterations) && (b != c));

        if (a != c) {
            //introduction changed something
            Task newTask = inputCloned(nar, task, c);

//            System.out.println(a + " ====> " + c);
//            System.out.println("\t" + task + " ====> " + newTask);
//            System.out.println();
        }


    }


    private Compound introduceNextVariable(Compound input, int iteration) {


        Term selection = Terms.substMaximal(input, this::canIntroduce, 2, 3);
        if (selection != null) {

            Term newContent = $.terms.replace(input, selection, next(input, selection, iteration));
            if ((newContent instanceof Compound) && !newContent.equals(input))
                return (Compound) newContent; //success
        }

        return input;
    }

    /** provides the next term that will be substituted */
    abstract protected Term next(Compound input, Term selection, int iteration);
    /*{
        return $.varQuery("c" + iteration);
    }*/


    private boolean canIntroduce(Term subterm) {
        return !subterm.op().var;
    }

    @Nullable
    protected Task inputCloned(NAR nar, @NotNull Task original, @NotNull Term newContent) {

        Compound c = nar.normalize((Compound) newContent);
        if (c != null && !c.equals(original.term())) {

            Task derived = new VarIntroducedTask(c, original.punc(), original.truth())
                    .time(original.creation(), original.occurrence())
                    .evidence(original.evidence())
                    .budget(original.budget())
                    .log(tag);

            Concept dc = nar.input(derived);
            if (dc != null) {
                //input successful
                dc.crossLink(derived, original, derived.isBeliefOrGoal() ? derived.conf() : derived.qua(), nar);
                return derived;
            }

        }

        return null;
    }

    public VarIntroduction each(NAR nar) {
        nar.onTask(t -> {
            accept(t, nar);
        });
        return this;
    }

    public static final class VarIntroducedTask extends GeneratedTask {

        public VarIntroducedTask(Compound c, char punc, @Nullable Truth truth) {
            super(c, punc, truth);
        }
    }

}
