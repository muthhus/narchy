package nars.task;

import nars.*;
import nars.budget.Budget;
import nars.budget.BudgetFunctions;
import nars.term.Compound;
import nars.term.Termed;
import nars.time.Tense;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static nars.$.t;
import static nars.Op.NEG;
import static nars.term.Terms.compoundOrNull;

/**
 * Mutable task with additional fluent api utility methods
 */
public class MutableTask extends AbstractTask {

    @Nullable
    private List log;

    public MutableTask(@NotNull Termed<Compound> t, char punct, float freq, @NotNull NAR nar) {
        this(t, punct, new DefaultTruth(freq, nar.confidenceDefault(punct)));
    }

//    public MutableTask(@NotNull String compoundTermString, char punct, float freq, float conf) throws Narsese.NarseseException {
//        this($.$(compoundTermString), punct, t(freq, conf));
//    }
    public MutableTask(@NotNull Termed<Compound> t, char punct, float freq, float conf) {
        this(t, punct, t(freq, conf));
    }

    public MutableTask(@NotNull String compoundTermString, char punct, @Nullable Truth truth) throws Narsese.NarseseException {
        this($.$(compoundTermString), punct, truth);
    }


    public MutableTask(@NotNull Termed<Compound> term, char punct, @Nullable Truth truth) {
        super(term, punct, truth,
            /* budget: */ 0, Float.NaN);
    }


    @NotNull
    public final MutableTask present(@NotNull NAR nar) {
        return time(nar.time());
    }
    @NotNull
    public final MutableTask time(@NotNull NAR nar, int dt) {
        return time(nar.time() + dt);
    }

    @NotNull public final MutableTask time(long when) {
        return MutableTask.this.time(when, when);
    }

    @NotNull
    public MutableTask budgetByTruth(float p) {
        return budgetByTruth(p, null);
    }

    @NotNull
    public MutableTask budgetByTruth(float p, @Nullable NAR nar) {
        float q;
        Truth t = truth();
        if (t!=null) {
            q = BudgetFunctions.truthToQuality(t);
        } else {
            if (nar!=null) {
                q = nar.qualityDefault(punc());
            } else
                throw new RuntimeException("missing truth");
        }

        setBudget(p, q);
        return this;
    }


    @NotNull
    public MutableTask time(long creationTime, long start, long end) {
        setCreationTime(creationTime);
        setStart(start);
        setEnd(end);
        return this;
    }

    @NotNull
    public MutableTask time(long creationTime, long occurrenceTime) {
        setCreationTime(creationTime);
        setStart(occurrenceTime);
        setEnd(occurrenceTime);
        return this;
    }



    @NotNull
    public final MutableTask occurr(long occurrenceTime) {
        setStart(occurrenceTime);
        setEnd(occurrenceTime);
        return this;
    }


    @NotNull
    public MutableTask eternal() {
        setStart(Tense.ETERNAL);
        setEnd(Tense.ETERNAL);
        return this;
    }




    @NotNull
    public final MutableTask evidence(long... evi) {
        setEvidence(evi);
        return this;
    }

    public final MutableTask evidence(@NotNull Task evidenceToCopy) {
        return evidence(evidenceToCopy.evidence());
    }

//    @NotNull
//    public Task log(@Nullable List historyToCopy) {
//        if (!Param.DEBUG_TASK_LOG)
//            return this;
//
//        if ((historyToCopy != null) && (!historyToCopy.isEmpty())) {
//            getOrCreateLog().addAll(historyToCopy);
//        }
//        return this;
//    }

    /**
     * append an entry to this task's log history
     * useful for debugging but can also be applied to meta-analysis
     * ex: an entry might be a String describing a change in the story/history
     * of the Task and the reason for it.
     */
    @NotNull
    public final MutableTask log(@Nullable Object entry) {
        if (!(entry == null || !Param.DEBUG_TASK_LOG))
            getOrCreateLog().add(entry);
        return this;
    }

//    /** retrieve the log element at the specified index, or null if it doesnt exist */
//    @Override
//    public final Object log(int index) {
//        @Nullable List l = this.log;
//        return l != null ? (l.size() > index ? l.get(index) : null) : null;
//    }

    @Nullable
    @Override
    public final List log() {
        return (log);
    }


    @NotNull
    final List getOrCreateLog() {

        List exist = log();
        if (exist == null) {
            this.log = (exist = $.newArrayList(1));
        }
        return exist;
    }



    @Override
    public boolean delete() {
        if (super.delete()) {
            if (!Param.DEBUG)
                this.log = null; //.clear();
            return true;
        }
        return false;
    }

    @NotNull
    public final MutableTask budget(@NotNull Budget bb) {
        setBudget(bb);
        return this;
    }

    /** sets the budget even if 'b' has been deleted; priority will be zero in that case */
    @NotNull
    public final MutableTask budgetSafe(@NotNull Budget b) {
        budgetSafe(b.pri(), b.qua());
        return this;
    }



    /** if p is NaN (indicating deletion), p <== 0 */
    @NotNull public final MutableTask budgetSafe(float p, float q) {
        priority = p;
        quality = q;
//        if (p!=p)
//            p = 0;
//        setBudget(p, d, q);
        return this;
    }

//    /** if p is NaN (indicating deletion), p <== 0 */
//    @NotNull public final MutableTask budgetSafe(float p, NAR nar) {
//        priority = p;
//        quality = isQuestOrQuestion() ? nar.qualityDefault(punc()) : BudgetFunctions.truthToQuality(truth());
////        if (p!=p)
////            p = 0;
////        setBudget(p, d, q);
//        return this;
//    }

    @Nullable public static MutableTask clone(@NotNull Task xt, @NotNull Compound y) {
//        if (!y.isNormalized()) {
//            y = (Compound) nar.normalize(y);
//            if (y == null)
//                return null;
//        }

        Budget b = xt.budget().clone(); //snapshot its budget
        if (b.isDeleted()) {
            return null;
        } else {
            boolean negated = (y.op()==NEG);
            if (negated)
                y = compoundOrNull(y.unneg());

            if (y == null)
                return null;

            MutableTask yt = new MutableTask(y, xt.punc(), xt.truth().negIf(negated));
            yt.setBudget(b);
            yt.setEvidence(xt.evidence());
            yt.time(xt.creation(), xt.start(), xt.end());
            return yt;
        }
    }



//    /**
//     * append a log entry; returns this task
//     */
//    @NotNull
//    Task log(Object entry);
//
//    /**
//     * append log entries; returns this task
//     */
//    @NotNull
//    Task log(List entries);

}
