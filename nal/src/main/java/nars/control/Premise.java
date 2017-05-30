/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.control;

import jcog.Util;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.control.BufferedDerivation;
import nars.derive.DefaultDeriver;
import nars.task.BinaryTask;
import nars.task.ITask;
import nars.term.Term;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Defines the conditions used in an instance of a derivation
 * Contains the information necessary for generating derivation Tasks via reasoning rules.
 * <p>
 * It is meant to be disposable and should not be kept referenced longer than necessary
 * to avoid GC loops, so it may need to be weakly referenced.
 */
public class Premise extends BinaryTask<Pair<Task,Term>,Task> {

    static final ThreadLocal<BufferedDerivation> derivation =
            ThreadLocal.withInitial(BufferedDerivation::new);

    public Premise(@NotNull Task taskLink,
                   @NotNull Term termLink,
                   @Nullable Task belief, float pri) {
        super(Tuples.pair(taskLink, termLink), belief, pri);
    }


    @NotNull
    public final Term beliefTerm() {
        Task belief = belief();
        return (belief != null) ? belief.term() : getOne().getTwo();
    }

    public final Task belief() {
        return getTwo();
    }

    public Task task() {
        return getOne().getOne();
    }

    @Override
    public ITask[] run(NAR n) {
        float p = this.pri;
        if (p!=p) return null;

        BufferedDerivation d = derivation.get();

        d.buffer.clear(); //should be clear but just in case

        d.restartA(n);
        d.restartB(task());
        d.restartC(this, Util.lerp(p, Param.UnificationTTLMax, Param.UnificationTTLMin));

        DefaultDeriver.the.test(d);

        ITask[] r = d.flush();

        delete(); //self-destruct

        return r;
    }



//    @Nullable
//    private static Task answer(@NotNull NAR nar, @NotNull Task question, @NotNull Task answer, @NotNull Concept answerConcept) {
//
//        long taskOcc = question.occurrence();
//
//        //project the belief to the question's time
//        if (taskOcc != ETERNAL) {
//            answer = answerConcept.merge(question, answer, taskOcc, nar);
//        }
//
//        if (answer != null) { //may have become null as a result of projection
//
//            //attempt to Unify any Query variables; answer if unifies
//            if (question.term().hasVarQuery()) {
//                matchQueryQuestion(nar, question, answer, answerConcept);
//            } else if (answerConcept instanceof Compound && Terms.equalAtemporally(question, answerConcept)) {
//                matchAnswer(nar, question, answer, answerConcept);
//            }
//
//
//        }
//
//        return answer;
//    }

    //static void matchAnswer(@NotNull NAR nar, @NotNull Task q, Task a, @NotNull Concept answerConcept) {
//        @Nullable Concept questionConcept = nar.concept(q);
//        if (questionConcept != null) {
//            List<Task> displ = $.newArrayList(0);
//            ((QuestionTable) questionConcept.tableFor(q.punc())).answer(a, answerConcept, nar, displ);
//            nar.tasks.remove(displ);
//        }
    //}

//    static void matchQueryQuestion(@NotNull NAR nar, @NotNull Task task, @NotNull Task belief, @NotNull Concept answerConcept) {
//        List<Termed> result = $.newArrayList(Param.QUERY_ANSWERS_PER_MATCH);
//        new UnifySubst(Op.VAR_QUERY, nar, result, Param.QUERY_ANSWERS_PER_MATCH)
//                .unifyAll(
//                        task.term(), belief.term()
//                );
//
//        if (!result.isEmpty()) {
//            matchAnswer(nar, task, belief, answerConcept);
//        }
//    }





//    @NotNull
//    @Override
//    public String toString() {
//        return task + " | " + term + " | " + (belief != null ? belief : "");
//    }


//    /** true if both task and (non-null) belief are temporal events */
//    public final boolean isEvent() {
//        /* TODO This part is used commonly, extract into its own precondition */
//        Task b = belief();
//        return (b!=null) && (!task().isEternal()) && (!b.isEternal());
//    }

//    @Nullable public final Concept concept(NAR n) {
//        return n.concept(conceptLink);
//    }

//    @Nullable public final BLink<? extends Termed> termlink(NAR nar) {
//        return termlink(nar.concept(concept));
//    }

//    @Nullable
//    public final BLink<? extends Termed> termlink(@NotNull Concept c) {
//        return c.termlinks().get(term);
//    }
//
////    @Nullable public final BLink<? extends Task> tasklink(NAR nar) {
////        return tasklink(nar.concept(concept));
////    }
//
//    @Nullable
//    public final BLink<? extends Task> tasklink(@NotNull Concept c) {
//        return c.tasklinks().get(task);
//    }

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof Premise)) return false;
//
//        Premise premise = (Premise) o;
//
//        if (!task.equals(premise.task)) return false;
//        if (!term.equals(premise.term)) return false;
//        return belief != null ? belief.equals(premise.belief) : premise.belief == null;
//
//    }

//    @Override
//    public int hashCode() {
//        int result = 1;
//        result = 31 * result + task.hashCode();
//        result = 31 * result + term.hashCode();
//        result = 31 * result + (belief != null ? belief.hashCode() : 0);
//        return result;
//    }

}
