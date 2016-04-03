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
import nars.nal.meta.PremiseEval;
import nars.nal.op.Derive;
import nars.task.DerivedTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Termed;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.nal.Tense.DTERNAL;
import static nars.nal.Tense.ETERNAL;
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
    //public final BLink<? extends Concept> conceptLink;
    public final BLink<? extends Termed> termLink;
    @Nullable private final Task belief;

    /** lazily cached value :=
     *      -1: unknown
     *      0: parents have no evidential overlap
     *      1: parents have overlapping evidence
     */
    private transient byte overlap = -1;
    /** lazily cached value :=
     *      -1: unknown
     *      0: not cyclic
     *      1: cyclic
     */
    private transient byte cyclic = -1;

    public ConceptProcess(NAR nar,
                          BLink<? extends Task> taskLink,
                          BLink<? extends Termed> termLink, @Nullable Task belief) {
        this.nar = nar;

        this.taskLink = taskLink;
        //assert(!task().isDeleted());

        //this.conceptLink = conceptLink;
        this.termLink = termLink;

        this.belief = belief;
    }



    @Override public final boolean overlap() {
        int cc = this.overlap;
        if (cc != -1) {
            return cc > 0; //cached value
        } else {
            Task b = this.belief;
            boolean o = (b != null) && Stamp.overlapping(task(), b);
            this.overlap = (byte)(o ? 1 : 0);
            return o;
        }
    }

    @Override public boolean cyclic() {
        int cc = this.cyclic;
        if (cc != -1) {
            return cc > 0; //cached value
        } else {
            //Task b = this.belief;
            boolean o = task().cyclic();
            this.cyclic = (byte)(o ? 1 : 0);
            return o;
        }
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

    @NotNull
    @Override
    public final Termed beliefTerm() {
        Task x = belief();
        return x == null ? termLink.get() : x;
    }

    @Nullable
    @Override
    public final Task belief() {
        return belief;
    }


    @NotNull
    @Override
    public String toString() {
        return new StringBuilder().append(
                getClass().getSimpleName())
                .append('[')
                //.append(conceptLink).append(',')
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
        final float min = Global.matchTermutationsMin, max = Global.matchTermutationsMax;
        return (int) Math.ceil(task().pri() * (max - min) + min);
    }




    /** part 2 */
    public void derive(@NotNull Termed<Compound> c, @Nullable Truth truth, @NotNull Budget budget, long now, long occ, @NotNull PremiseEval p, @NotNull Derive d) {

        char punct = p.punct.get();

        boolean single;
        switch (punct) {
            case Symbols.BELIEF: single = d.beliefSingle; break;
            case Symbols.GOAL: single = d.goalSingle; break;
            default:
                single = false;
        }

        Task derived = newDerivedTask(c, punct)
                .truth(truth)
                .time(now, occ)
                .parent(task(), !single ? belief() : null)
                .budget(budget) // copied in, not shared
                //.anticipate(derivedTemporal && d.anticipate)
                .log( Global.DEBUG ? d.rule : "Derived");

        accept(derived);

        //ETERNALIZE:

        if ((occ != ETERNAL) && (truth != null) && d.eternalize) {

            accept(newDerivedTask(c, punct)
                    .truth(
                        truth.freq(),
                        eternalize(truth.conf())
                    )

                    .time(now, ETERNAL)

                    //.parent(derived)  //this is lighter weight and potentially easier on GC than: parent(task, belief) MAYBE WRONG wrt cyclicity
                    .parent(task(), !single ? belief() : null)

                    .budgetCompoundForward(budget, this)

                    .log("Immediaternalized") //Immediate Eternalization

            );

        }

    }

    @NotNull
    public DerivedTask newDerivedTask(@NotNull Termed<Compound> c, char punct) {
        return new DerivedTask(c, punct, this);
    }



    /** when a derivation is accepted, this is called  */
    abstract protected void accept(Task derivation);



    public final boolean hasTemporality() {
        if (task().term().dt()!= DTERNAL) return true;
        @Nullable Task b = belief();
        if (b == null) return false;
        return b.term().dt()!= DTERNAL;
    }


}
