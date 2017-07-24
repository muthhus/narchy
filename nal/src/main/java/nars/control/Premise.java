/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.control;

import jcog.Util;
import jcog.pri.Pri;
import jcog.pri.PriReference;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.concept.BaseConcept;
import nars.concept.Concept;
import nars.table.BeliefTable;
import nars.task.ITask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.UnifySubst;
import nars.util.BudgetFunctions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.ETERNAL;

/**
 * NOTE: this currently isnt input to the NAR like ITask's are even though it inherits
 * from that superclass. this is temporary until the Premise behavior is determined
 * to be either reified or virtual (executed within a conceptfire execution only)
 * <p>
 * Defines the conditions used in an instance of a derivation
 * Contains the information necessary for generating derivation Tasks via reasoning rules.
 * <p>
 * It is meant to be disposable and should not be kept referenced longer than necessary
 * to avoid GC loops, so it may need to be weakly referenced.
 */
public class Premise extends Pri implements ITask {

    public final PriReference<Task> taskLink;
    public final PriReference<Term> termLink;
    private final int hash;

    public Premise(@Nullable PriReference<Task> tasklink, @Nullable PriReference<Term> termlink) {
        super(Param.tasktermLinkCombine.apply(tasklink.priElseZero(), termlink.priElseZero()));
        this.taskLink = tasklink;
        this.termLink = termlink;
        this.hash = Util.hashCombine(tasklink.hashCode(), termlink.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Premise)) return false;
        Premise x = (Premise) obj;
        if (hash != x.hash) return false;
        return taskLink.equals(((Premise) obj).taskLink) && termLink.equals(((Premise) obj).termLink);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return taskLink + "+" + termLink;
    }

    /**
     * resolve the most relevant belief of a given term/concept
     * <p>
     * patham9 project-eternalize
     * patham9 depending on 4 cases
     * patham9 https://github.com/opennars/opennars2/blob/a143162a559e55c456381a95530d00fee57037c4/src/nal/deriver/projection_eternalization.clj
     * sseehh__ ok ill add that in a bit
     * patham9 you need  project-eternalize-to
     * sseehh__ btw i disabled immediate eternalization entirely
     * patham9 so https://github.com/opennars/opennars2/blob/a143162a559e55c456381a95530d00fee57037c4/src/nal/deriver/projection_eternalization.clj#L31
     * patham9 especially try to understand the "temporal temporal" case
     * patham9 its using the result of higher confidence
     * <p>
     * returns ttl used, -1 if failed before starting
     */
    public int run(Derivation d, int ttlMax) {
        d.nar.emotion.conceptFirePremises.increment();

        //nar.emotion.count("Premise_run");

        PriReference<Task> taskLink = this.taskLink;
        final Task task = taskLink.get();
        if (task == null)
            return 0;

        //float taskPri = task.priElseZero();


        NAR nar = d.nar;
        int dur = d.dur;
        long now = d.time;


        Term beliefTerm = termLink.get();
        Task belief = null;


        Concept taskConcept = task.concept(nar, true);
        assert (taskConcept != null) : task + " could not be conceptualized";

        Concept _beliefConcept = nar.conceptualize(beliefTerm);
        boolean beliefIsTask = _beliefConcept != null && taskConcept.equals(_beliefConcept);


        //Terms.equalAtemporally(task.term(), (beliefTerm));

        boolean reUnified = false;
        if (beliefTerm.varQuery() > 0 && !beliefIsTask) {

            int[] matchTTL = {Math.round(ttlMax * Param.BELIEF_MATCH_TTL_FRACTION)};

            Term unified = unify(beliefTerm, task.term(), nar, matchTTL);
            if (unified != null) {
                beliefTerm = unified;
                reUnified = true;
            }

            assert (matchTTL[0] <= 0);
            ttlMax += matchTTL[0]; //changed if consumed in match (this value will be negative
        }


        //QUESTION ANSWERING and TERMLINK -> TEMPORALIZED BELIEF TERM projection
        if (_beliefConcept instanceof BaseConcept) { //beliefs/goals will only be in TaskConcepts

            BaseConcept beliefConcept = (BaseConcept) _beliefConcept;



            Task match;

            if (task.isQuestOrQuestion() && (reUnified || beliefIsTask)) {
                final BeliefTable answerTable =
                        (task.isGoal() || task.isQuest()) ?
                                beliefConcept.goals() :
                                beliefConcept.beliefs();

//                            //see if belief unifies with task (in reverse of previous unify)
//                            if (questionTerm.varQuery() == 0 || (unify((Compound)beliefConcept.term(), questionTerm, nar) == null)) {
//
//                            } else {
//
//                            }
                long when = whenAnswer(task, now);
                match = answerTable.answer(when, now, dur, task,  beliefTerm, beliefConcept, nar);
                if (match != null) {
                    @Nullable Task answered = task.onAnswered(match, nar);
                    if (answered != null) {

                        float effectiveConf = answered.conf(answered.nearestTimeTo(task.mid()), dur);

                        //transfer budget from question to answer
                        //float qBefore = taskBudget.priSafe(0);
                        //float aBefore = answered.priSafe(0);
                        BudgetFunctions.fund(taskLink, answered,
                                /*Util.sqr*/effectiveConf, false);

                        nar.value(answered.cause(), effectiveConf);
                    }
                }
            } else {
                long when = whenMatch(task, now);
                match = beliefConcept.beliefs().match(when, task, beliefTerm, true, nar);
            }

            if (match != null && match.isBelief()) {
                belief = match;
            }
        }


        if (belief != null) {
            beliefTerm = belief.term(); //use the belief's actual possibly-temporalized term
        }

        if (belief != null && belief.equals(task)) //do not repeat the same task for belief
            belief = null; //force structural transform; also prevents potential inductive feedback loop

        d.run(this, task, belief, beliefTerm, ttlMax);

//        long ds = d.transformsCache.estimatedSize();
//        if (ds >0)
//            System.out.println(ds + " " + d.transformsCache.stats());

        int ttlAfter = d.ttl();

        return ttlMax - ttlAfter;

    }

    /**
     * temporal focus control: determines when a matching belief or answer should be projected to
     */
    static long whenMatch(Task task, long now) {
        if (task.isEternal()) {
            return ETERNAL;
        } else //if (task.isInput()) {
            return task.nearestTimeTo(now);
//        } else {
//            if (task.isBelief()) {
//                return now +
//                        nar.dur() *
//                            nar.random().nextInt(2*Param.PREDICTION_HORIZON)-Param.PREDICTION_HORIZON; //predictive belief
//            } else {
//                return Math.max(now, task.start()); //the corresponding belief for a goal or question task
//            }
//        }

        //now;
        //now + dur;
    }

    protected static long whenAnswer(Task task, long now) {
        return task.nearestTimeTo(now);
    }


    /**
     * unify any (and only) query variables which may be present in
     * the 'a' term with any non-query terms in the 'q' term
     * returns non-null if unification succeeded and resulted in a transformed 'a' term
     * sets a negative number in the ttl array, which is to be added to the callee's
     * ttl.  if zero, then no TTL was consumed
     */
    @Nullable
    private static Compound unify(@NotNull Term q, @NotNull Term a, NAR nar, int[] ttl) {

        final int startTTL = ttl[0];
        ttl[0] = 0;

        if (q.op() != a.op() /*|| q.size() != a.size()*/)
            return null; //fast-fail: no chance

        final Compound[] result = {null};
        UnifySubst u = new UnifySubst(Op.VAR_QUERY, nar, (aa) -> {


            if (!aa.equals(a)) {

                aa = aa.eval(nar.terms);

                result[0] = ((Compound) aa);
                return false; //only this match
            }


            return true; //keep trying

        }, startTTL);
        u.unifyAll(q, a);

        ttl[0] = -(startTTL - u.ttl); //how much consumed

        return result[0];

//        if (Terms.equal(q, a, false, true /* no need to unneg, task content is already non-negated */))
//            return q;
//        else
//            return null;
    }

    @Nullable
    @Override
    public ITask[] run(@NotNull NAR n) {
        run(n.derivation(), Math.round(n.matchTTL.intValue() * (1f + priElseZero())));
        return null;
    }
}
