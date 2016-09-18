package nars.op;

import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TruthDelta;
import nars.nal.Stamp;
import nars.task.GeneratedTask;
import nars.task.MutableTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * a generalized variable introduction model that can transform tasks
 */
public abstract class VarIntroduction implements BiConsumer<Task,NAR> {

    @Deprecated final static String tag = VarIntroduction.class.getSimpleName();

    final int introductionThreshold = 0;
    final int maxIterations;

    public VarIntroduction(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    @Override public void accept(Task task, NAR nar) {
        Compound c = task.term();

        //size limitations
        if ((c.volume()) < 2 || (c.volume() + introductionThreshold > nar.compoundVolumeMax.intValue()))
            return;


        Compound a = c, b;

        int i = 0;
        do {
            b = c;
            c = introduceNextVariable(nar, c, i++);
        } while ((i < maxIterations) && (b != c));

        if (a != c) {
            //introduction changed something
            input(nar, task, c);

//            System.out.println(a + " ====> " + c);
//            System.out.println("\t" + task + " ====> " + newTask);
//            System.out.println();
        }


    }


    @NotNull private Compound introduceNextVariable(NAR nar, @NotNull Compound input, int iteration) {


        Term[] selections = nextSelection(input);
        if (selections != null) {

            for (Term s : selections) {
                Term[] dd = next(input, s, iteration);
                if (dd != null) {
                    for (Term d : dd) {
                        Term newContent = nar.index.replace(input, s, d);
                        if ((newContent instanceof Compound) && !newContent.equals(input))
                            return (Compound) newContent; //success
                    }
                }
            }
        }

        return input;
    }

    @Nullable
    abstract protected Term[] nextSelection(Compound input);


    /** provides the next terms that will be substituted in separate permutations; return null to prevent introduction */
    abstract protected Term[] next(Compound input, Term selection, int iteration);
    /*{
        return $.varQuery("c" + iteration);
    }*/



    @Nullable
    protected void input(NAR nar, @NotNull Task original, @NotNull Term newContent) {

        Compound c = nar.normalize((Compound) newContent);
        if (c != null && !c.equals(original.term())) {

            Task derived = clone(original, c);

            nar.input(derived);

        }

    }

    protected Task clone(@NotNull Task original, Compound c) {
        return new VarIntroducedTask(c, original)
            .time(original.creation(), original.occurrence())
            .evidence(Stamp.cyclic(original.evidence()))
            .budgetSafe(original.budget())
            .log(tag);
    }

    public VarIntroduction each(NAR nar) {
        nar.onTask(t -> {
            accept(t, nar);
        });
        return this;
    }


    private static class VarIntroducedTask extends GeneratedTask {

        @Nullable private Task original;

        public VarIntroducedTask(@NotNull Compound c, @NotNull Task original) {
            super(c, original.punc(), original.truth());
            this.original = original;
        }

        /** if input was successful, crosslink to the original */
        @Override public void feedback(TruthDelta delta, float deltaConfidence, float deltaSatisfaction, NAR nar) {
            //if the original was deleted already before this feedback was applied, delete this task too
            @Nullable Task orig = this.original;

            if (orig == null || orig.isDeleted()) {
                delete();
                return;
            }

            if (orig !=null && deltaConfidence==deltaConfidence /* wasn't deleted, even for questions */) {
                concept(nar).crossLink(this, this.original, isBeliefOrGoal() ? conf() : qua(), nar);
            }

            super.feedback(delta, deltaConfidence, deltaSatisfaction, nar);
        }

        @Override
        public boolean delete() {
            if (super.delete()) {
                if (!Param.DEBUG) original = null; //unlink
                return true;
            }
            return false;
        }

//        @Override
//        public @NotNull Task log(@Nullable List historyToCopy) {
//            return original!= null ? nuloriginal.log(historyToCopy) : null;
//        }
    }
}
