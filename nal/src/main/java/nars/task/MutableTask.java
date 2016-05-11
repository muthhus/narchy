package nars.task;

import nars.*;
import nars.budget.Budget;
import nars.budget.BudgetFunctions;
import nars.budget.merge.BudgetMerge;
import nars.budget.Budgeted;
import nars.concept.ConceptProcess;
import nars.nal.Tense;
import nars.term.Compound;
import nars.term.Termed;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.util.Objects;

import static nars.Global.dereference;
import static nars.Global.reference;

/**
 * Mutable task with additional fluent api utility methods
 */
public class MutableTask extends AbstractTask {

    public MutableTask(@NotNull Termed<Compound> t, char punct, float freq, @NotNull NAR nar) throws Narsese.NarseseException {
        this(t, punct, new DefaultTruth(freq, nar.confidenceDefault(punct)));
    }

    public MutableTask(@NotNull String compoundTermString, char punct, @Nullable Truth truth) throws Narsese.NarseseException {
        this((Compound)$.$(compoundTermString), punct, truth);
    }


    public MutableTask(@NotNull Termed<Compound> term, char punct, @Nullable Truth truth) {
        super(term.term(), punct, truth,
            /* budget: */ 0, Float.NaN, Float.NaN);
    }

//    @NotNull
//    public static MutableTask clone(@NotNull Task t, @NotNull Compound newTerm) {
//        return new MutableTask(t, newTerm);
//    }


//    public MutableTask(@NotNull Task taskToClone, @NotNull Compound newTerm) {
//        this(taskToClone);
//        term(newTerm);
//        punc(taskToClone.punc());
//
//    }


    @NotNull
    public static /* TODO ProjectedTask */ Task project(@NotNull Task t, long now, long occ) {
        Truth newTruth = t.projectTruth(now, occ, false);
        if (t.truth().equals(newTruth) && t.occurrence()==occ)
            return t;
        return new MutableTask(t, newTruth, now, occ);
    }

    public MutableTask(@NotNull Task taskToClone, @NotNull Truth newTruth, long now, long occ) {
        super(taskToClone);
        truth(newTruth);
        time(now, occ);
        parent(taskToClone);
    }

    public MutableTask(@NotNull Task taskToClone, @NotNull Task otherTask, long now, long occ, long[] newEvidence, @NotNull BudgetMerge budgetMerge) {
        this(taskToClone, otherTask, now, occ, newEvidence, taskToClone.truth(), budgetMerge);
    }

    public MutableTask(@NotNull Task taskToClone, @NotNull Task otherTask, long now, long occ, long[] newEvidence, Truth newTruth, @NotNull BudgetMerge budgetMerge) {
        this(taskToClone, taskToClone, otherTask, now, occ, newEvidence, newTruth, budgetMerge);
    }

    /** used by QuestionTable */
    public MutableTask(@NotNull Termed<Compound> newTerm, @NotNull Task taskToClone, @NotNull Task otherTask, long now, long occ, long[] newEvidence, Truth newTruth, @NotNull BudgetMerge budgetMerge) {
        this(newTerm, taskToClone.punc(), newTruth);

        this.parentBelief = Global.reference(otherTask);
        setEvidence(newEvidence);

        time(now, occ);

        budget(taskToClone.budget());
        budgetMerge.merge(budget(), otherTask.budget(), 1f);
    }

//    public MutableTask(@NotNull Termed<Compound> content, char punc) {
//        this(content);
//        punctuation(punc);
//    }


    @NotNull
    public MutableTask truth(@Nullable Truth tv) {
//        if (tv == null)
//            setTruth(null);
//        else
//            setTruth(new DefaultTruth(tv));
        setTruth(tv);
        return this;
    }



    @NotNull
    @Override
    public final MutableTask budget(float p, float d, float q) {
        super.budget(p, d, q);
        return this;
    }

    @NotNull
    @Override
    public final MutableTask budget(@Nullable Budgeted source) {
        super.budget(source);
        return this;
    }

    /**
     * uses default budget generation and multiplies it by gain factors
     */
    @NotNull
    public MutableTask budgetScaled(float priorityFactor, float durFactor) {
        priMult(priorityFactor);
        mulDurability(durFactor);
        return this;
    }

    @NotNull
    protected final MutableTask term(@NotNull Termed<Compound> t) {
        setTerm(t);
        return this;
    }

    @NotNull
    public final MutableTask truth(float freq, float conf) {
        //if (truth == null)
            setTruth(new DefaultTruth(freq, conf));
        //else
            //this.truth.set(freq, conf);
        return this;
    }


    @NotNull
    public MutableTask belief() {
        punc(Symbols.BELIEF);
        return this;
    }

    @NotNull
    public MutableTask question() {
        punc(Symbols.QUESTION);
        return this;
    }

    @NotNull
    public MutableTask quest() {
        punc(Symbols.QUEST);
        return this;
    }

    @NotNull
    public MutableTask goal() {
        punc(Symbols.GOAL);
        return this;
    }

    @NotNull
    public MutableTask time(@NotNull Tense t, @NotNull Memory memory) {
        occurr(Tense.getRelativeOccurrence(t, memory));
        return this;
    }

    @NotNull
    public final MutableTask present(@NotNull Memory memory) {
        return present(memory.time());
    }

    @NotNull public final MutableTask present(long when) {
        return MutableTask.this.time(when, when);
    }

    @NotNull
    public MutableTask budget(float p, float d) {
        float q;
        Truth t = truth();
        if (!isQuestOrQuestion()) {
            if (t == null)
                throw new RuntimeException("Truth needs to be defined prior to budget to calculate truthToQuality");
            q = BudgetFunctions.truthToQuality(t);
        } else
            throw new RuntimeException("incorrect punctuation");

        budget(p, d, q);
        return this;
    }

    @NotNull
    public MutableTask punctuation(char punctuation) {
        punc(punctuation);
        return this;
    }

    @NotNull
    public MutableTask time(long creationTime, long occurrenceTime) {
        setCreationTime(creationTime);
        setOccurrenceTime(occurrenceTime);
        return this;
    }


    @NotNull
    public MutableTask because(Object reason) {
        log(reason);
        return this;
    }


    @NotNull
    public final MutableTask parent(@NotNull Task parentTask, @Nullable Task parentBelief) {
        /*if (parentTask == null)
            throw new RuntimeException("parent task being set to null");*/

        ensureParentNonLoop(parentTask, parentBelief);

        this.parentTask = (/*(parentTask != null) &&*/ !parentTask.isCommand()) ? reference(parentTask) : null;

        this.parentBelief = ((parentBelief != null) && !parentBelief.isCommand()) ? reference(parentBelief) : null;

        updateEvidence();

        return this;
    }

    private void ensureParentNonLoop(@NotNull Task parentTask, @Nullable Task parentBelief) {
        if (Global.DEBUG) {
            if (parentTask!=null && Objects.equals(this,parentTask))
                throw new RuntimeException("parentTask loop");
            if (parentBelief!=null && Objects.equals(this,parentBelief))
                throw new RuntimeException("parentBelief loop");
        }
    }

    @NotNull
    public MutableTask parent(Reference<Task> rt, Reference<Task> rb) {

        Task pt = dereference(rt);
        this.parentTask = ((pt != null) && !pt.isCommand()) ? rt : null ;

        Task pb = dereference(rb);
        this.parentBelief = ((pb != null) && !pb.isCommand()) ? rb : null;

        ensureParentNonLoop(pt, pb);

        updateEvidence();

        return this;
    }


    @NotNull
    public final MutableTask occurr(long occurrenceTime) {
        setOccurrenceTime(occurrenceTime);
        return this;
    }

    @NotNull
    public final MutableTask parent(@NotNull Task task) {
        return parent(task, null);
    }

    /**
     //     * sets an amount of cycles to shift the final applied occurence time
     //     */
//    public TaskSeed<T> occurrDelta(long occurenceTime) {
//        this.occDelta = occurenceTime;
//        return this;
//    }

    @NotNull
    public MutableTask eternal() {
        setEternal();
        return this;
    }


//    /** flag used for anticipatable derivation */
//    @NotNull
//    public MutableTask anticipate(boolean a) {
//        if (state==TaskState.Executed)
//            throw new RuntimeException("can not anticipate already executed task");
//
//        if (a) state = (TaskState.Anticipated);
//        return this;
//    }

    @NotNull
    public MutableTask budgetCompoundForward(@NotNull Budget input, @NotNull ConceptProcess premise) {
        budget(input);
        BudgetFunctions.compoundForward(
                budget(), truth(),
                term(), premise);
        return this;
    }


    @NotNull
    protected final MutableTask punc(char punctuation) {
        if (this.punctuation!=punctuation) {
            this.punctuation = punctuation;
            invalidate();
        }
        return this;
    }

    @NotNull
    public final MutableTask evidence(long[] evi) {
        setEvidence(evi);
        return this;
    }
}
