package nars.task;

import nars.Memory;
import nars.Symbols;
import nars.budget.Budget;
import nars.budget.BudgetFunctions;
import nars.concept.ConceptProcess;
import nars.nal.Tense;
import nars.term.Termed;
import nars.term.compound.Compound;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.ref.Reference;

import static nars.Global.dereference;
import static nars.Global.reference;

/**
 * Mutable task with additional fluent api utility methods
 */
public class MutableTask extends AbstractTask {

    public MutableTask() {
        /** budget triple - to be valid, at least the first 2 of these must be non-NaN (unless it is a question)  */
        super(null, (char) 0, null,
            /* budget: */ 0, Float.NaN, Float.NaN);

        setEternal();
        setOccurrenceTime(Tense.TIMELESS);
    }

    @NotNull
    public static MutableTask clone(@NotNull Task t, @NotNull Compound newTerm) {
        return new MutableTask(t, newTerm);
    }

    MutableTask(@NotNull Task taskToClone, @NotNull Compound newTerm) {
        super(taskToClone);
        term(newTerm);
    }

    public MutableTask(@NotNull Termed<Compound> term) {
        this();
        term(term);
    }

    public MutableTask(@NotNull Termed<Compound> content, char punc) {
        this(content);
        punctuation(punc);
    }


    @NotNull
    public MutableTask truth(@org.jetbrains.annotations.Nullable Truth tv) {
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
    public final MutableTask budget(@Nullable Budget source) {
        super.budget(source);
        return this;
    }

    /**
     * uses default budget generation and multiplies it by gain factors
     */
    @NotNull
    public MutableTask budgetScaled(float priorityFactor, float durFactor) {
        mulPriority(priorityFactor);
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
        return judgment();
    }

    @NotNull
    public MutableTask judgment() {
        setPunctuation(Symbols.JUDGMENT);
        return this;
    }

    @NotNull
    public MutableTask question() {
        setPunctuation(Symbols.QUESTION);
        return this;
    }

    @NotNull
    public MutableTask quest() {
        setPunctuation(Symbols.QUEST);
        return this;
    }

    @NotNull
    public MutableTask goal() {
        setPunctuation(Symbols.GOAL);
        return this;
    }

    @NotNull
    public MutableTask tense(@NotNull Tense t, @NotNull Memory memory) {
        occurr(Tense.getRelativeOccurrence(memory.time(), t, memory));
        return this;
    }

    @NotNull
    public final MutableTask present(@NotNull Memory memory) {
        return present(memory.time());
    }

    @NotNull public final MutableTask present(long when) {
        return time(when, when);
    }

    @NotNull
    public MutableTask budget(float p, float d) {
        float q;
        Truth t = getTruth();
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
        setPunctuation(punctuation);
        return this;
    }

    @NotNull
    public MutableTask time(long creationTime, long occurrenceTime) {
        setCreationTime(creationTime);
        occurr(occurrenceTime);
        return this;
    }


    @NotNull
    public MutableTask because(Object reason) {
        log(reason);
        return this;
    }


    public final MutableTask parent(@org.jetbrains.annotations.Nullable Task parentTask, Task parentBelief) {
        if (parentTask == null)
            throw new RuntimeException("parent task being set to null");



        //TODO avoid creating the ref's if not necessary?
        return parent(reference(parentTask), reference(parentBelief));
    }

    @NotNull
    public MutableTask parent(Reference<Task> rt, Reference<Task> rb) {

        Task pt = dereference(rt);
        Task pb = dereference(rb);

        if (pt!=null && pt.isCommand()) rt = null;
        if (pb!=null && pb.isCommand()) rb = null;

        this.parentTask = rt;
        this.parentBelief = rb;
        updateEvidence();

        return this;
    }


    @NotNull
    public final MutableTask occurr(long occurrenceTime) {
        setOccurrenceTime(occurrenceTime);
        return this;
    }

    public final MutableTask parent(Task task) {
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


    /** flag used for anticipatable derivation */
    @NotNull
    public MutableTask anticipate(boolean a) {
        if (state==TaskState.Executed)
            throw new RuntimeException("can not anticipate already executed task");

        if (a) state = (TaskState.Anticipated);
        return this;
    }

    @NotNull
    public MutableTask budgetCompoundForward(@NotNull ConceptProcess premise) {
        BudgetFunctions.compoundForward(
                getBudget(), getTruth(),
                term(), premise);
        return this;
    }
}
