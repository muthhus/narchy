package nars.op;

import jcog.list.FasterList;
import nars.$;
import nars.index.term.TermIndex;
import nars.term.Compound;
import nars.term.Term;
import org.apache.commons.collections4.map.Flat3Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import static nars.op.DepIndepVarIntroduction.ConjOrStatementBits;

/**
 * a generalized variable introduction model that can transform tasks
 */
public abstract class VarIntroduction {

    static final int maxSubstitutions = 1;

    public VarIntroduction() {

    }

    public void accept(@NotNull Compound c, @NotNull Consumer<Compound> each) {

        if (!c.hasAny(ConjOrStatementBits) || c.volume() < 2)
            return; //earliest failure test

        List<Term> selections = select(c);
        if (selections == null) return;
        int sels = selections.size();
        if (sels == 0) return;



        if (maxSubstitutions >= sels) {
            //
        } else {
            //choose randomly
            //assert(maxSubstitutions==1); //only 1 and all (above) at implemented right now
            selections = $.newArrayList(
                    selections.get(ThreadLocalRandom.current().nextInt(sels))
            );
        }


        Map<Term,Term> substs = new Flat3Map<>();
        int o = 0;
        boolean found = false;
        for (int i = 0, selectionsSize = selections.size(); i < selectionsSize; i++) {
            Term u = selections.get(i);
            Term v = next(c, u, o++);
            if (v != null) {
                substs.put(u, v);
                found = true;
            }
        }
        if (!found)
            return; //nothing found to introduce a variable for


        Term newContent = index().replace(c, substs);

        if ((newContent instanceof Compound) && !newContent.equals(c)) {
            each.accept((Compound) newContent);

        }

//        while (selections.hasNext()) {
//
//            Term s = selections.next();
//
//
//
//            if (dd != null) {
//                int replacements = dd.length;
//                if (replacements > 0) {
//
//                    if (replacements > 1)
//                        throw new UnsupportedOperationException("TODO"); //choose one randomly
//
//                    Term d = dd[0];
//
//
//                }
//            }
//        }
    }

    protected TermIndex index() {
        return $.terms;
    }


    @Nullable
    abstract protected FasterList<Term> select(Compound input);


    /** provides the next terms that will be substituted in separate permutations; return null to prevent introduction */
    abstract protected Term next(@NotNull Compound input, @NotNull Term selection, int order);
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
