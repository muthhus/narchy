/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.nal;

import nars.Global;
import nars.Premise;
import nars.budget.Budget;
import nars.concept.Concept;
import nars.link.BLink;
import nars.nal.meta.PremiseEval;
import nars.nal.op.Derive;
import nars.task.DerivedTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Firing a concept (reasoning event).  Derives new Tasks via reasoning rules.
 * It is meant to be disposable and should not be kept referenced longer than necessary
 * to avoid GC loops
 */
public class ConceptProcess implements Premise {

    public final BLink<? extends Task> taskLink;

    public final BLink<? extends Termed> termLink;

    @Nullable public final Task belief;

    public final BLink<? extends Concept> conceptLink;


    public ConceptProcess(BLink<? extends Concept> conceptLink,
                          BLink<? extends Task> taskLink,
                          BLink<? extends Termed> termLink, @Nullable Task belief) {

        this.taskLink = taskLink;

        this.conceptLink = conceptLink;

        this.termLink = termLink;

        this.belief = belief;
    }


    @Nullable
    @Override
    public final Task task() {
        return taskLink.get();
    }


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




    /** part 2 */
    @NotNull public final Task derive(@NotNull Termed<Compound> c, @Nullable Truth truth, @NotNull Budget budget, long now, long occ, @NotNull PremiseEval p, @NotNull Derive d) {


        return newDerivedTask(c, p.punct.get(), truth)
                .time(now, occ)
                .budget(budget) // copied in, not shared
                //.anticipate(derivedTemporal && d.anticipate)
                .log(Global.DEBUG ? d.rule : "Derived");


        //ETERNALIZE: (CURRENTLY DISABLED)

//        if ((occ != ETERNAL) && (truth != null) && d.eternalize  ) {


//            if (!derived.isDeleted()) {
//
//
//                nar.process(newDerivedTask(c, punct, new DefaultTruth(truth.freq(), eternalize(truth.conf())), parents)
//                        .time(now, ETERNAL)
//                        .budgetCompoundForward(budget, this)
//                        /*
//                TaskBudgeting.compoundForward(
//                        budget, truth(),
//                        term(), premise);*/
//                        .log("Immediaternalized") //Immediate Eternalization
//
//                );
//            }

//        }

    }


    @NotNull
    public DerivedTask newDerivedTask(@NotNull Termed<Compound> c, char punct, Truth truth) {
        return new DerivedTask.DefaultDerivedTask(c, punct, truth, this);
        //return new DerivedTask.CompetingDerivedTask(c, punct, truth, this);
    }





}
