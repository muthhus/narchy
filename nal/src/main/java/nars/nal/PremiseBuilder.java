package nars.nal;

import nars.*;
import nars.budget.Budget;
import nars.budget.RawBudget;
import nars.budget.merge.BudgetMerge;
import nars.budget.policy.TaskBudgeting;
import nars.concept.Concept;
import nars.concept.table.BeliefTable;
import nars.concept.table.QuestionTable;
import nars.index.TermIndex;
import nars.link.BLink;
import nars.nal.meta.PremiseEval;
import nars.task.AnswerTask;
import nars.task.RevisionTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Terms;
import nars.term.subst.UnifySubst;
import nars.truth.Truth;
import nars.truth.TruthFunctions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static nars.Op.CONJ;
import static nars.Op.NEG;
import static nars.concept.Activation.linkable;
import static nars.nal.Tense.ETERNAL;

/**
 * Entry point for using the NAL+ Internal Reasoner
 * <p>
 * TODO abstraction for dynamic link iterator / generator allowing a concept to
 * programmatically supply the tasklinks/termlinks it fires.  bag selection being
 * the default but overridable on a per-concept basis.
 * ex:
 * --ranges of values (numbers, strings, etc..)
 * --without and/or with memory of prior iterations from which to continue
 * (ex: database cursors)
 * <p>
 * may determine the iteration "power" according to
 * some budgeting feature (ex: Concept BLink)
 */
public enum PremiseBuilder {
    ;

    private static final Logger logger = LoggerFactory.getLogger(PremiseBuilder.class);

    /**
     * Main Entry point: begin matching the task half of a premise
     */
    @NotNull
    public static void run(@NotNull Concept c, @NotNull NAR nar, @NotNull List<BLink<Term>> termLinks, @NotNull BLink<Task> taskLink, @NotNull PremiseEval matcher, @NotNull Consumer<Premise> each) {

        long now = nar.time();

        float minDur = nar.durMin.floatValue();

        Task task = taskLink.get();
        if (task == null)
            return;

        Compound taskTerm = task.term();

        RawBudget pBudget = new RawBudget(); //recycled temporary budget for calculating premise budget

        for (int i = 0, termsArraySize = termLinks.size(); i < termsArraySize; i++) {

            if (taskLink.isDeleted() || task.isDeleted())
                break;

            BLink<Term> termLink = termLinks.get(i);
            Term term = termLink.get();
            /*if (term == null)
                continue;*/

            if (Terms.equalSubTermsInRespectToImageAndProduct(taskTerm, term))
                continue;

            if (budget(pBudget, taskLink, termLink, minDur)) {

                Premise p = newPremise(nar, c, now, task, term, pBudget);
                each.accept(p);


            }
        }

    }

    private static boolean budget(@NotNull RawBudget pBudget, @NotNull BLink<Task>taskLink, @NotNull BLink<Term> termLink, float minDur) {
        TaskBudgeting.premise(pBudget, taskLink, termLink);
        return pBudget.dur() >= minDur;
    }

    /**
     * resolves the most relevant belief of a given term/concept
     *
     *   patham9 project-eternalize
     patham9 depending on 4 cases
     patham9 https://github.com/opennars/opennars2/blob/a143162a559e55c456381a95530d00fee57037c4/src/nal/deriver/projection_eternalization.clj
     sseehh__ ok ill add that in a bit
     patham9 you need  project-eternalize-to
     sseehh__ btw i disabled immediate eternalization entirely
     patham9 so https://github.com/opennars/opennars2/blob/a143162a559e55c456381a95530d00fee57037c4/src/nal/deriver/projection_eternalization.clj#L31
     patham9 especially try to understand the "temporal temporal" case
     patham9 its using the result of higher confidence
     */
    static @NotNull Premise newPremise(@NotNull NAR nar, @NotNull Concept c, long now, @NotNull Task task, @NotNull Term termLinkTerm, Budget b) {


        Task belief = null;

        Term termLinkTermConceptTerm = $.unneg(termLinkTerm).term();
        if (termLinkTerm instanceof Compound && linkable(termLinkTermConceptTerm)) { //atomic concepts will have no beliefs to match

            Concept beliefConcept = nar.concept(termLinkTermConceptTerm);
            if (beliefConcept != null) {

                if ( task.isQuestOrQuestion()) {

                    //TODO is this correct handling for quests? this means a belief task may be a goal which may contradict deriver semantics
                    BeliefTable table = task.isQuest() ? beliefConcept.goals() : beliefConcept.beliefs();

                    Task solution = table.match(task, now);
                    if (solution!=null) {
                        try {
                            Task answered = answer(nar, task, solution, beliefConcept);
                            if (task.isQuestion())
                                belief = answered;
                            else
                                belief = currentBelief(task, beliefConcept, now, nar); //in case of quest, proceed with matching belief

                        } catch (TermIndex.InvalidConceptException e) {
                            logger.warn("{}", e.getMessage());
                        }

                    }


                } else {

                    belief = currentBelief(task, beliefConcept, now, nar);

                }
            }
        }

        Premise p = new Premise(c.term(), task, termLinkTerm, belief);
        p.budget(b);
        return p;
    }

    private static Task currentBelief(@NotNull Task task, Concept beliefConcept, long now, @NotNull NAR nar) {
        BeliefTable beliefs = beliefConcept.beliefs();

        Task x = beliefs.match(task,now);


        //experimental dynamic eval
        Compound term = (Compound) beliefConcept.term();
        int n = term.size();
        if (term.vars() == 0 && beliefConcept.op() == CONJ && n > 2) {
            long occThresh = 1;
            if (x == null || Math.abs(now - x.occurrence() ) >= occThresh) {

                boolean uncomputable = false;
                List<Truth> t = $.newArrayList(n);
                List<Task> e = $.newArrayList(n);
                Budget b = null;
                for (Term s : ((Compound) term).terms()) {
                    if (!(s instanceof Compound) || s.hasTemporal()) {
                        uncomputable = true; break;
                    }

                    boolean negated = s.op()==NEG;
                    if (negated)
                        s = $.unneg(s).term();

                    Concept p = nar.concept(s);
                    if (p == null || !p.hasBeliefs()) {
                        uncomputable = true; break;
                    }

                    @Nullable Truth nt = p.belief(now);
                    if (nt==null) {
                        uncomputable = true; break;
                    }
                    t.add($.negIf(nt,negated));

                    @Nullable Task bt = p.beliefs().top(now);
                    if (bt!=null) {
                        Budget btb = bt.budget();
                        if (b == null && !btb.isDeleted())
                            b = btb;
                        else
                            BudgetMerge.plusBlend.apply(b, btb, 1f);

                        e.add(bt); //HACK this doesnt include the non-top tasks which may contribute to the evaluated truth during truthpolation
                    }
                }

                if (!uncomputable) {
                    Truth y = TruthFunctions.intersection(t, nar.confMin.floatValue());
                    if (y != null) {

                        ///@NotNull Termed<Compound> newTerm = term.dt() != 0 ? $.parallel(term.terms()) : term;

                        RevisionTask xx = new RevisionTask(term, Symbols.BELIEF, y, now, now, Stamp.zip((Collection)e));
                        xx.budget(b);
                        xx.log("Dynamic");

                        nar.inputLater(xx);

                        //System.err.println(xx + "\tvs\t" + x);

                        x = xx;
                    }
                }
            }
        }

        return x;
    }


    @Nullable
    private static Task answer(@NotNull NAR nar, @NotNull Task taskLink, @NotNull Task solution, @NotNull Concept beliefConcept) {

        long taskOcc = taskLink.occurrence();

        //project the belief to the question's time
        if (taskOcc != ETERNAL) {
            solution = beliefConcept.merge(taskLink, solution, taskOcc, nar);
        }

        if (solution != null) { //may have become null as a result of projection

            //attempt to Unify any Query variables; answer if unifies
            if (taskLink.term().hasVarQuery()) {
                matchQueryQuestion(nar, taskLink, solution);
            } else if (beliefConcept instanceof Compound && Term.equalAtemporally(taskLink, beliefConcept)) {
                matchAnswer(nar, taskLink, solution);
            }



        }

        return solution;
    }


//    /**
//           (case [(get-eternal target-time) (get-eternal source-time)]
//                 [:eternal :eternal] t
//                 [:temporal :eternal] t
//                 [:eternal :temporal] (eternalize t)
//                 [:temporal :temporal] (let [t-eternal (eternalize t)
//                         t-project (project-to target-time t cur-time)]
//                         (if (> (confidence t-eternal)
//                             (confidence t-project))
//                             t-eternal
//                             t-project))))))
//     * @param target
//     * @param matched
//     * @return
//     */
//    private static Task project(Task target, Task matched, long now) {
//        boolean me = matched.isEternal();
//        if (me) {
//            return matched;
//        } else {
//            ProjectedTruth newTruth = matched.projectTruth(target.occurrence(), now, true);
//            return new MutableTask(matched, newTruth, now, newTruth.when);
//        }
//    }

    static void matchAnswer(@NotNull NAR nar, @NotNull Task q, Task a) {
        if (a instanceof AnswerTask)
            return; //already an answer

        @Nullable Concept c = nar.concept(q);
        if (c != null) {
            List<Task> displ = $.newArrayList(0);
            ((QuestionTable)c.tableFor(q.punc())).answer(a, nar, displ );
        }
    }

    static void matchQueryQuestion(@NotNull NAR nar, @NotNull Task task, @NotNull Task belief) {
        List<Termed> result = $.newArrayList(1);
        new UnifySubst(Op.VAR_QUERY, nar, result, Param.QUERY_ANSWERS_PER_MATCH).unifyAll(
                task.term(), belief.term()
        );
        if (!result.isEmpty()) {
            matchAnswer(nar, task, belief);
        }
    }



}
