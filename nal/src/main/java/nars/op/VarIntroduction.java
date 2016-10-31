package nars.op;

import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.nal.Stamp;
import nars.op.mental.Abbreviation;
import nars.task.GeneratedTask;
import nars.task.MutableTask;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.TruthDelta;
import nars.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Consumer;

import static nars.op.DepIndepVarIntroduction.ConjOrStatementBits;

/**
 * a generalized variable introduction model that can transform tasks
 */
public abstract class VarIntroduction {

    @Deprecated final static String tag = VarIntroduction.class.getSimpleName();

    final Random rng;
    final int maxIterations;

    public VarIntroduction(int maxIterations, Random rng) {
        this.maxIterations = maxIterations;
        this.rng = rng;
    }

    @NotNull public void accept(@NotNull Compound c, @NotNull Consumer<Compound> each) {

        if (!c.hasAny(ConjOrStatementBits) || c.volume() < 2)
            return; //earliest failure test


        Term[] selections = select(c);
        if (selections != null && selections.length > 0) {

            int max = maxIterations;

            Util.shuffle(selections, rng);
            for (Term s : selections) {

                Term[] dd = next(c, s);

                if (dd != null) {
                    int replacements = dd.length;
                    if (replacements > 0) {

                        Term d = dd[rng.nextInt(replacements)]; //choose one randomly
                        //for (Term d : dd) {
                        Term newContent = $.terms.replace(c, s, d);

                        if ((newContent instanceof Compound) && !newContent.equals(c)) {
                            each.accept((Compound) newContent);
                            if (--max <= 0)
                                return;
                        }
                    }
                }
            }
        }
    }

//    public void accept(@NotNull Task input) {
//        Compound c = input.term();
//        accept(c, newContent -> input(input, newContent));
//    }


    @Nullable
    abstract protected Term[] select(Compound input);


    /** provides the next terms that will be substituted in separate permutations; return null to prevent introduction */
    abstract protected Term[] next(Compound input, Term selection);
    /*{
        return $.varQuery("c" + iteration);
    }*/



//    @Nullable
//    protected void input(@NotNull Task original, @NotNull Term newContent) {
//
//        Compound c = nar.normalize((Compound) newContent);
//        if (c != null && !c.equals(original.term())) {
//
//            Task derived = clone(original, c);
//
//            nar.inputLater(derived);
//
//        }
//
//    }

//    @NotNull protected Task clone(@NotNull Task original, @NotNull Compound c) {
//        MutableTask t = new VarIntroducedTask(c, original)
//            .time(original.creation(), original.occurrence())
//            .evidence(Stamp.cyclic(original.evidence()))
//            .budgetSafe(original.budget());
//
////            if (Param.DEBUG)
////                t.log(tag + ":\n" + (original.log() != null ? Joiner.on("\t\n").join(original.log()) : ""));
////            else
//        t.log(tag);
//
//
//        return t;
//    }

//    @NotNull
//    public VarIntroduction each(@NotNull NAR nar) {
//        nar.onTask(this::accept);
//        return this;
//    }


//    public static final class VarIntroducedTask extends GeneratedTask {
//
//        @Nullable private volatile transient Task original;
//
//        public VarIntroducedTask(@NotNull Compound c, @NotNull Task original) {
//            super(c, original.punc(), original.truth());
//            this.original = original;
//
//        }
//
//        /** if input was successful, crosslink to the original */
//        @Override public void feedback(TruthDelta delta, float deltaConfidence, float deltaSatisfaction, @NotNull NAR nar) {
//            //if the original was deleted already before this feedback was applied, delete this task too
//            @Nullable Task orig = this.original;
//
//            if (orig == null || orig.isDeleted()) {
//                delete();
//                return;
//            }
//
//            if (deltaConfidence==deltaConfidence /* wasn't deleted, even for questions */) {
//                @Nullable Concept thisConcept = concept(nar);
//                Concept other = thisConcept.crossLink(this, orig, isBeliefOrGoal() ? conf() : qua(), nar);
//
//                if (original instanceof Abbreviation.AbbreviationTask) {
//                    //share abbreviation meta; prevents some cases of recursive abbreviation
//                    thisConcept.put(Abbreviation.class, other.get(Abbreviation.class));
//                }
//            }
//
//            super.feedback(delta, deltaConfidence, deltaSatisfaction, nar);
//        }
//
//        @Override
//        public boolean delete() {
//            if (super.delete()) {
//                if (!Param.DEBUG) original = null; //unlink
//                return true;
//            }
//            return false;
//        }
//
////        @Override
////        public @NotNull Task log(@Nullable List historyToCopy) {
////            return original!= null ? nuloriginal.log(historyToCopy) : null;
////        }
//    }
}
