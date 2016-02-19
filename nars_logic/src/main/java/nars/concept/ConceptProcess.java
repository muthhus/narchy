/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.concept;

import nars.Global;
import nars.NAR;
import nars.Premise;
import nars.Symbols;
import nars.bag.BLink;
import nars.budget.Budget;
import nars.nal.meta.PremiseMatch;
import nars.nal.op.Derive;
import nars.task.DerivedTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.nal.Tense.*;
import static nars.truth.TruthFunctions.eternalize;

/**
 * Firing a concept (reasoning event). Derives new Tasks via reasoning rules
 * <p>
 * Concept
 * Task
 * TermLinks
 */
abstract public class ConceptProcess implements Premise {


    public final NAR nar;
    public final BLink<? extends Task> taskLink;
    public final BLink<? extends Concept> conceptLink;
    public final BLink<? extends Termed> termLink;

    @Nullable
    private final Task belief;
    private final boolean cyclic;



    public ConceptProcess(NAR nar, BLink<? extends Concept> conceptLink,
                          BLink<? extends Task> taskLink,
                          BLink<? extends Termed> termLink, @Nullable Task belief) {
        this.nar = nar;

        this.taskLink = taskLink;
        this.conceptLink = conceptLink;
        this.termLink = termLink;

        this.belief = belief;
        this.cyclic = Stamp.overlapping(task(), belief);
    }


    @Override
    public final Task task() {
        return taskLink.get();
    }

//    /**
//     * @return the current termLink aka BeliefLink
//     */
//    @Override
//    public final BagBudget<Termed> getTermLink() {
//        return termLink;
//    }

    public Concept concept() {
        return conceptLink.get();
    }


//    protected void beforeFinish(final long now) {
//
//        Memory m = nar.memory();
//        m.logic.TASKLINK_FIRE.hit();
//        m.emotion.busy(getTask(), this);
//
//    }

//    @Override
//    final protected Collection<Task> afterDerive(Collection<Task> c) {
//
//        final long now = nar.time();
//
//        beforeFinish(now);
//
//        return c;
//    }

    public final Termed beliefTerm() {
        Task x = belief();
        return x == null ? termLink.get() :
                x.term();
    }

    @Nullable
    @Override
    public final Task belief() {
        return belief;
    }

    @Override
    public final boolean isCyclic() {
        return cyclic;
    }

    @NotNull
    @Override
    public String toString() {
        return new StringBuilder().append(
                getClass().getSimpleName())
                .append('[').append(conceptLink).append(',')
                .append(taskLink).append(',')
                .append(termLink).append(',')
                .append(belief())
                .append(']')
                .toString();
    }

    @Override
    public final NAR nar() {
        return nar;
    }

    public int getMaxMatches() {
        final float min = Global.MIN_TERMUTATIONS_PER_MATCH, max = Global.MAX_TERMUTATIONS_PER_MATCH;
        return (int) Math.ceil(task().pri() * (max - min) + min);
    }




    /** part 2 */
    public void derive(@NotNull Termed<Compound> c, @Nullable Truth truth, Budget budget, long now, long occ, @NotNull PremiseMatch p, @NotNull Derive d) {

        char punct = p.punct.get();

        Task belief = belief();


        boolean derivedTemporal = occ != ETERNAL;

        Task derived = newDerivedTask(c, punct)
                .truth(truth)
                .budget(budget) // copied in, not shared
                .time(now, occ)
                .parent(task(), belief /* null if single */)
                .anticipate(derivedTemporal && d.anticipate)
                .log( Global.DEBUG ? d.rule : "Derived");

        if (!complete(derived))
            return;

        //--------- TASK WAS DERIVED if it reaches here

        if (derivedTemporal && (truth != null) && d.eternalize) {

            complete(newDerivedTask(c, punct)
                    .truth(
                            truth.freq(),
                            eternalize(truth.conf())
                    )

                    .time(now, ETERNAL)

                    .budget(budget) // copied in, not shared
                    .budgetCompoundForward(this)

                    .parent(derived)  //this is lighter weight and potentially easier on GC than: parent(task, belief)

                    .log("Immediaternalized") //Immediate Eternalization

            );

        }

    }

    @NotNull
    public DerivedTask newDerivedTask(@NotNull Termed<Compound> c, char punct) {
        return new DerivedTask(c, punct, this);
    }

    private final boolean complete(Task derived) {

        //pre-normalize to avoid discovering invalidity after having consumed space while in the input queue
        derived = derived.normalize(memory());
        if (derived != null) {

            //if (Global.DEBUG) {
            if (task().equals(derived))
                return false;
                //throw new RuntimeException("derivation same as task");
            if (belief() != null && belief().equals(derived))
                return false;
                //throw new RuntimeException("derivation same as belief");
            //}

            accept(derived);
            return true;
        }
        return false;
    }


    /** when a derivation is accepted, this is called  */
    abstract protected void accept(Task derivation);

    /** after a derivation has completed, commit is called allowing it to process anything collected */
    abstract protected void commit();

    public final void run(@NotNull PremiseMatch matcher) {
        matcher.start(this);
        commit();
    }

    public boolean hasTemporality() {
        if (task().term().t()!=ITERNAL) return true;
        @Nullable Task b = belief();
        if (b == null) return false;
        return b.term().t()!=ITERNAL;
    }
}
