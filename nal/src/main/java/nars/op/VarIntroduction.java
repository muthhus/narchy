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

    @Deprecated final static String tag = MutaTaskBag.class.getSimpleName();

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
            input(nar, task, c);

//            System.out.println(a + " ====> " + c);
//            System.out.println("\t" + task + " ====> " + newTask);
//            System.out.println();
        }


    }


    private Compound introduceNextVariable(Compound input, int iteration) {


        Term[] selections = nextSelection(input);
        if (selections != null) {

            for (Term s : selections) {
                Term d = next(input, s, iteration);
                if (d != null) {
                    Term newContent = $.terms.replace(input, s, d);
                    if ((newContent instanceof Compound) && !newContent.equals(input))
                        return (Compound) newContent; //success
                }
            }
        }

        return input;
    }

    @Nullable
    abstract protected Term[] nextSelection(Compound input);


    /** provides the next term that will be substituted; return null to prevent introduction */
    @Nullable abstract protected Term next(Compound input, Term selection, int iteration);
    /*{
        return $.varQuery("c" + iteration);
    }*/



    @Nullable
    protected void input(NAR nar, @NotNull Task original, @NotNull Term newContent) {

        Compound c = nar.normalize((Compound) newContent);
        if (c != null && !c.equals(original.term())) {

            Task derived = clone(original, c);

            nar.runLater( ()-> {
                Concept dc = nar.input(derived);

                if (dc != null) {
                    //input successful
                    dc.crossLink(derived, original, derived.isBeliefOrGoal() ? derived.conf() : derived.qua(), nar);
                }
            });

        }

    }

    protected Task clone(@NotNull Task original, Compound c) {
        return new VarIntroducedTask(c, original.punc(), original.truth())
                        .time(original.creation(), original.occurrence())
                        .evidence(original.evidence())
                        .budgetSafe(original.budget())
                        .log(tag);
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
