/*
 * Task.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.task;

import nars.*;
import nars.budget.Budget;
import nars.budget.Budgeted;
import nars.concept.Concept;
import nars.nal.Tense;
import nars.term.*;
import nars.truth.ProjectedTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.Truthed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static nars.Global.dereference;
import static nars.nal.LocalRules.solutionBudget;
import static nars.nal.Tense.TIMELESS;
import static nars.truth.TruthFunctions.eternalize;
import static nars.truth.TruthFunctions.temporalProjectionOld;

/**
 * A task to be processed, consists of a Sentence and a BudgetValue.
 * A task references its parent and an optional causal factor (usually an Operation instance).  These are implemented as WeakReference to allow forgetting via the
 * garbage collection process.  Otherwise, Task ancestry would grow unbounded,
 * violating the assumption of insufficient resources (AIKR).
 * <p>
 * TODO decide if the Sentence fields need to be Reference<> also
 */
public interface Task extends Budgeted, Truthed, Comparable, Stamp, Termed, Tasked, Supplier<Task> {

    static void explanation(@NotNull Task task, int indent, @NotNull StringBuilder sb) {
        //TODO StringBuilder

        for (int i = 0; i < indent; i++)
            sb.append("  ");


        task.appendTo(sb, null, true);

//        List l = task.getLog();
//        if (l!=null)
//            sb.append(" log=").append(l);

//        if (task.getBestSolution() != null) {
//            if (!task.term().equals(task.getBestSolution().term())) {
//                sb.append(" solution=");
//                task.getBestSolution().appendTo(sb);
//            }
//        }

        Task pt = task.getParentTask();
        Task pb = task.getParentBelief();
        sb.append('\n');

        sb.append("  ");
        if (pt != null) {
            //sb.append("  PARENT ");
            explanation(pt, indent+1, sb);
        }
        if (pb != null) {
            //sb.append("  BELIEF ");
            explanation(pb, indent+1, sb);
        }
    }

//    static Set<Truthed> getSentences(Iterable<Task> tasks) {
//
//        int size;
//
//        size = tasks instanceof Collection ? ((Collection) tasks).size() : 2;
//
//        Set<Truthed> s = Global.newHashSet(size);
//        for (Task t : tasks)
//            s.add(t);
//        return s;
//    }

    /** performs some (but not exhaustive) tests on a term to determine some cases where it is invalid as a sentence content
     * returns the compound valid for a Task if so,
     * otherwise returns null
     * */
    static boolean validTaskTerm(Term t) {

        if (!(t instanceof Compound))//(t instanceof CyclesInterval) || (t instanceof Variable)
            return false;

        Compound st = (Compound) t;
        if (t.op().isStatement()) {

            /* A statement sentence is not allowed to have a independent variable as subj or pred"); */
            if (subjectOrPredicateIsIndependentVar(st))
                return false;

            if (Global.DEBUG_PARANOID) {
                //should be checked on statement construction
                //if it occurrs here, that did not happen somewhere prior
                if (Statement.invalidStatement(st.term(0), st.term(1)))
                    throw new RuntimeException("statement invalidity should be tested before created: " + st);
            }

        }

        return true;
    }

    static float prioritySum(@NotNull Iterable<? extends Budgeted > dd) {
        float f = 0;
        for (Budgeted  x : dd)
            f += x.pri();
        return f;
    }

    static boolean subjectOrPredicateIsIndependentVar(@NotNull Compound t) {
        if (!t.hasVarIndep()) return false;

        return (t.term(0).op() == Op.VAR_INDEP) || (t.term(1).op() == Op.VAR_INDEP);
    }


    @Override
    @NotNull
    default Task task() { return this; }


    /**
     * Get the parent task of a task.
     * It is not guaranteed to remain because it is
     * stored as a Soft or Weak reference so that
     * task ancestry does not grow uncontrollably;
     *
     * instead, we rely on the JVM garbage collector
     * to serve as an enforcer of AIKR
     *
     * @return The task from which the task is derived, or
     * null if it has been forgotten
     */
    @Nullable
    default Task getParentTask() {
        return dereference(getParentTaskRef());
    }

    @Nullable Reference<Task> getParentTaskRef();


    @Nullable
    Task getParentBelief();

    @Nullable Reference<Task> getParentBeliefRef();

    /**
     * Check whether different aspects of sentence are equivalent to another one
     *
     * @param that The other judgment
     * @return Whether the two are equivalent
     */
    boolean equivalentTo(Task that, boolean punctuation, boolean term, boolean truth, boolean stamp, boolean creationTime);

    /** called when a Concept processes this Task */
    void onConcept(Concept c);

    @Nullable
    default Task answer(@NotNull Compound newTerm, @NotNull Task question, @NotNull Memory memory) {
        return term().equals(newTerm) && occurrence() == question.occurrence() ? this : answerProjected(newTerm, question, memory);
    }

    @Nullable
    default Task answerProjected(@NotNull Compound newTerm, @NotNull Task question, @NotNull Memory memory) {

        long now = memory.time();

        float termRelevance = Terms.termRelevance(newTerm, question.term());
        if (termRelevance == 0)
            return null;

        //TODO avoid creating new Truth instances
        Truth solTruth = projectTruth(question.occurrence(), now, true);
        if (solTruth == null)
            return null;

        //if truth instanceof ProjectedTruth, use its attached occ time (possibly eternal or temporal), otherwise assume it is this task's occurence time
        long solutionOcc = solTruth instanceof ProjectedTruth ?
                ((ProjectedTruth)solTruth).when : occurrence();

        if (solTruth.conf() < conf()) return this;

        solTruth = solTruth.withConfMult(termRelevance);
                //* BeliefTable.relevance(this, solutionOcc, memory.duration()));
                //solTruth.withConf( w2c(solTruth.conf())* termRelevance );

        if (solTruth.conf() < conf()) return this;

        Budget solutionBudget = solutionBudget(question, this, solTruth, memory);
        if (solutionBudget == null)
            return null;



        Task solution;
        //if ((!truth().equals(solTruth)) || (!newTerm.equals(term())) || (solutionOcc!= occCurrent)) {
            solution = new MutableTask(newTerm, punc())
                    .truth(solTruth)
                    .time(now, solutionOcc)
                    .parent(question, this)
                    .budget(solutionBudget)
                    .state(state())
                    //.setEvidence(evidence())
                    .log("Projected Solution")
                    //.log("Projected from " + this)
                    ;

        //} else {
        //    solution = this;
        //}



        ////TODO avoid adding repeat & equal Solution instances
        //solution.log(new Solution(question));

        return solution;
    }

    char punc();

    @Nullable
    @Override
    long[] evidence();

    @Override
    long creation();

    @NotNull
    @Override
    Task setCreationTime(long c);

    /**
     * Recognize a Question
     *
     * @return Whether the object is a Question
     */
    default boolean isQuestion() {
        return (punc() == Symbols.QUESTION);
    }

    /**
     * Recognize a Judgment
     *
     * @return Whether the object is a Judgment
     */
    default boolean isJudgment() {
        return (punc() == Symbols.BELIEF);
    }

    default boolean isGoal() {
        return (punc() == Symbols.GOAL);
    }

    default boolean isQuest() {
        return (punc() == Symbols.QUEST);
    }

    default boolean isCommand()  {
        return (punc() == Symbols.COMMAND);
    }

    default boolean hasQueryVar() {
        return term().hasVarQuery();
    }



    @Nullable
    default StringBuilder appendTo(StringBuilder sb) {
        return appendTo(sb, null);
    }

    @NotNull
    default Task name() {
        return this;
    }



    @Nullable
    default CharSequence toString(Memory memory, boolean showStamp) {
        return appendTo(new StringBuilder(), memory, showStamp);
    }

    @NotNull
    @Override default Task get() { return this ;}

    @NotNull
    default Concept concept(@NotNull NAR n) {
        return n.concept(term());
    }

    @NotNull @Override
    Compound term();

    @Nullable @Override
    Truth truth();

    default boolean isQuestOrQuestion() {
        return isQuestion() || isQuest();
    }

    default boolean isJudgmentOrGoal() {
        return isJudgment() || isGoal();
    }

    /** allows for budget feedback that occurrs on revision */
    default boolean onRevision(Task conclusion) {
        return true;
    }

//    default float getConfidenceIfTruthOr(float v) {
//        Truth t = truth();
//        if (t == null) return v;
//        return t.conf();
//    }

    @NotNull
    default Task projectTask(long when, long now) {
        Truth adjustedTruth = projectTruth(when, now, false);
        long occ = occurrence();
        long projOcc = (adjustedTruth instanceof ProjectedTruth) ? ((ProjectedTruth)adjustedTruth).when : occ;
        return /*occ == projOcc &&*/ adjustedTruth.equals(truth()) ? this :
                MutableTask.clone(this, adjustedTruth, now, projOcc);

    }

    /** performs the procedure that should happen when this task is invoked due to goal desire */
    boolean execute(NAR nar);

    void delete();


//    /** get the absolute time of an event subterm, if present, TIMELESS otherwise */
//    default long subtermTimeAbs(Term x) {
//        long t = subtermTime(x);
//        if (t == TIMELESS) return TIMELESS;
//        return t + occurrence();
//    }

//    /** relevant time of an event subterm (or self), if present, TIMELESS otherwise */
//    default long subtermTime(Term x) {
//        return term().subtermTime(x, x.t());
//    }


//    default float projectionConfidence(long when, long now) {
//        //TODO avoid creating Truth Values by calculating the confidence directly. then use this in projection's original usage as well
//
//        float factor = TruthFunctions.temporalProjection(getOccurrenceTime(), when, now);
//
//        return factor * getConfidence();
//
//        //return projection(when, now).getConfidence();
//    }


    enum TaskState {
        Anticipated,
        Executed
    }

    @Nullable
    TaskState state();

    final class Solution extends AtomicReference<Task> {
        Solution(Task referent) {
            super(referent);
        }

        @NotNull
        @Override
        public String toString() {
            return "Solved: " + get();
        }
    }




    @NotNull
    default StringBuilder toString(/**@Nullable*/Memory memory) {
        return appendTo(null, memory);
    }

    @NotNull
    default StringBuilder appendTo(@Nullable StringBuilder sb, /**@Nullable*/Memory memory) {
        if (sb == null) sb = new StringBuilder();
        return appendTo(sb, memory, false);
    }

    @NotNull
    @Deprecated default String toStringWithoutBudget() {
        return toStringWithoutBudget(null);
    }

    @NotNull
    @Deprecated default String toStringWithoutBudget(Memory memory) {
        StringBuilder b = new StringBuilder();
        appendTo(b, memory, true, false,
                false, //budget
                false//log
        );
        return b.toString();
    }

    @Nullable
    @Deprecated
    default StringBuilder appendTo(StringBuilder buffer, /**@Nullable*/ Memory memory, boolean showStamp) {
        boolean notCommand = punc()!=Symbols.COMMAND;
        return appendTo(buffer, memory, true, showStamp && notCommand,
                notCommand, //budget
                showStamp //log
        );
    }

    @Nullable
    default StringBuilder appendTo(@Nullable StringBuilder buffer, /**@Nullable*/@Nullable Memory memory, boolean term, boolean showStamp, boolean showBudget, boolean showLog) {



        Compound t = term();
        String contentName = t.toString();

        CharSequence tenseString;
        if (memory!=null) {
            tenseString = getTense(memory.time(), memory.duration());
        }
        else {
            //TODO dont bother craeting new StringBuilder and calculating the entire length etc.. just append it to a reusable StringReader?
            appendOccurrenceTime(
                    (StringBuilder)(tenseString = new StringBuilder()));
        }


        CharSequence stampString = showStamp ? stampAsStringBuilder() : null;

        int stringLength = contentName.length() + tenseString.length() + 1 + 1;

        if (truth() != null)
            stringLength += 11;

        if (showStamp)
            stringLength += stampString.length()+1;

        /*if (showBudget)*/
        //"$0.8069;0.0117;0.6643$ "
        stringLength += 1 + 6 + 1 + 6 + 1 + 6 + 1  + 1;

        String finalLog;
        if (showLog) {
            Object ll = getLogLast();

            finalLog = (ll!=null ? ll.toString() : null);
            if (finalLog!=null)
                stringLength += finalLog.length()+1;
            else
                showLog = false;
        }
        else
            finalLog = null;


        if (buffer == null)
            buffer = new StringBuilder(stringLength);
        else
            buffer.ensureCapacity(stringLength);


        if (showBudget) {
            budget().toBudgetStringExternal(buffer).append(' ');
        }

        buffer.append(contentName).append(punc());

        if (tenseString.length() > 0)
            buffer.append(' ').append(tenseString);

        if (truth()!= null) {
            buffer.append(' ');
            truth().appendString(buffer, 2);
        }

        if (showStamp)
            buffer.append(' ').append(stampString);

        if (showLog) {
            buffer.append(' ').append(finalLog);
        }

        return buffer;
    }

    @Nullable
    default Object getLogLast() {
        List<String> log = log();
        if (log ==null || log.isEmpty()) return null;
        return log.get(log.size()-1);
    }


    default boolean hasParent(Task t) {
        if (getParentTask() == null)
            return false;
        Task p = getParentTask();
        do {
            Task n = p.getParentTask();
            if (n == null) break;
            if (n.equals(t))
                return true;
            p = n;
        } while (true);
        return false;
    }

    @Nullable
    default Task getRootTask() {
        if (getParentTask() == null) {
            return null;
        }
        Task p = getParentTask();
        do {
            Task n = p.getParentTask();
            if (n == null) break;
            p = n;
        } while (true);
        return p;
    }


    @NotNull
    default String explanation() {
        StringBuilder sb = new StringBuilder();
        return explanation(sb).toString();
    }

    @NotNull
    default StringBuilder explanation(@NotNull StringBuilder temporary) {
        temporary.setLength(0);
        explanation(this, 0, temporary);
        return temporary;
    }

    default Truth getDesire() {
        return truth();
    }



    /** append a log entry; returns this task */
    @NotNull
    Task log(Object entry);

    /** append log entries; returns this task */
    @NotNull
    Task log(List entries);

    /** get the recorded log entries */
    @Nullable
    List log();


    //TODO make a Source.{ INPUT, SINGLE, DOUBLE } enum

    /** is double-premise */
    boolean isDouble();

    /** is single premise */
    boolean isSingle();

    /**
     * Check if a Task is a direct input,
     * or if its origin has been forgotten or never known
     */
    default boolean isInput() {
        return getParentTask() == null;
        //return evidence().length <= 1;
    }


    /**
     * a task is considered amnesiac (origin not rememebered) if its parent task has been forgotten (garbage collected via a soft/weakref)
     */
    default boolean isAmnesiac() {
        return !isInput() && getParentTask() == null;
    }


    /** if unnormalized, returns a normalized version of the task,
     *  null if not normalizable
     */
    @Nullable
    Task normalize(Memory memory);


    default void ensureValidParentTaskRef() {
        if ((getParentTaskRef() != null && getParentTask() == null))
            throw new RuntimeException("parentTask must be null itself, or reference a non-null Task");
    }


    void setTruth(Truth t);


//
//    /** normalize a collection of tasks to each other
//     * so that the aggregate budget sums to a provided
//     * normalization amount.
//     * @param derived
//     * @param premisePriority the total value that the derivation group should reach, effectively a final scalar factor determined by premise parent and possibly existing belief tasks
//     * @return the input collection, unmodified (elements may be adjusted individually)
//     */
//    static void normalizeCombined(@NotNull Iterable<Task> derived, float premisePriority) {
//
//
//        float totalDerivedPriority = prioritySum(derived);
//        float factor = Math.min(
//                    premisePriority/totalDerivedPriority,
//                    1.0f //limit to only diminish
//                );
//
//        if (!Float.isFinite(factor))
//            throw new RuntimeException("NaN");
//
//        derived.forEach(t -> t.budget().priMult(factor));
//    }
//
//    static void normalize(@NotNull Iterable<Task> derived, float premisePriority) {
//        derived.forEach(t -> t.budget().priMult(premisePriority));
//    }
//    static void inputNormalized(@NotNull Iterable<Task> derived, float premisePriority, @NotNull Consumer<Task> target) {
//        derived.forEach(t -> {
//            t.budget().priMult(premisePriority);
//            target.accept(t);
//        });
//    }

    @NotNull
    static Task command(@NotNull Compound op) {
        //TODO use lightweight CommandTask impl without all the logic metadata
        return new MutableTask(op, Symbols.COMMAND);
    }

    default boolean isEternal() {
        return occurrence()== Tense.ETERNAL;
    }


    @NotNull
    default StringBuilder appendOccurrenceTime(@NotNull StringBuilder sb) {
        long oc = occurrence();
        long ct = creation();

        /*if (oc == Stamp.TIMELESS)
            throw new RuntimeException("invalid occurrence time");*/
        if (ct == Tense.ETERNAL)
            throw new RuntimeException("invalid creation time");

        //however, timeless creation time means it has not been perceived yet

        if (oc == Tense.ETERNAL) {
            if (ct == TIMELESS) {
                sb.append(":-:");
            } else {
                sb.append(':').append(Long.toString(ct)).append(':');
            }

        } else if (oc == TIMELESS) {
            sb.append("N/A");

        } else {
            int estTimeLength = 8; /* # digits */
            sb.ensureCapacity(estTimeLength);

            sb.append(Long.toString(ct));

            long OCrelativeToCT = (oc - ct);
            if (OCrelativeToCT >= 0)
                sb.append('+'); //+ sign if positive or zero, negative sign will be added automatically in converting the int to string:
            sb.append(OCrelativeToCT);

        }

        return sb;
    }

    default String getTense(long currentTime, int duration) {

        long ot = occurrence();

        if (Tense.isEternal(ot)) {
            return "";
        }

        switch (Tense.order(currentTime, ot, duration)) {
            case 1:
                return Symbols.TENSE_FUTURE;
            case -1:
                return Symbols.TENSE_PAST;
            default:
                return Symbols.TENSE_PRESENT;
        }
    }

    @NotNull
    default CharSequence stampAsStringBuilder() {

        long[] ev = evidence();
        int len = ev != null ? ev.length : 0;
        int estimatedInitialSize = 8 + (len * 3);

        StringBuilder buffer = new StringBuilder(estimatedInitialSize);
        buffer.append(Symbols.STAMP_OPENER);

        if (creation() == TIMELESS) {
            buffer.append('?');
        } else if (!Tense.isEternal(occurrence())) {
            appendOccurrenceTime(buffer);
        } else {
            buffer.append(creation());
        }
        buffer.append(Symbols.STAMP_STARTER).append(' ');
        for (int i = 0; i < len; i++) {

            buffer.append(Long.toString(ev[i], 36));
            if (i < (len - 1)) {
                buffer.append(Symbols.STAMP_SEPARATOR);
            }
        }

        buffer.append(Symbols.STAMP_CLOSER); //.append(' ');

        //this is for estimating an initial size of the stringbuffer
        //System.out.println(baseLength + " " + derivationChain.size() + " " + buffer.baseLength());

        return buffer;


    }


    /** creates a new child task (has this task as its parent) */
    @NotNull
    default MutableTask spawn(@NotNull Compound content, char punc) {
        return new MutableTask(content, punc);
    }

    default long occurrence() {
        return Tense.ETERNAL;
    }

    default long start() { return occurrence(); }
    default long end() {
        return start() + duration();
    }
    default int duration() {
        return 0;
    }



//    default Truth projection(long targetTime, long now) {
//        return projection(targetTime, now, true);
//    }

    //projects the truth to a certain time, covering all 4 cases as discussed in
    //https://groups.google.com/forum/#!searchin/open-nars/task$20eteneral/open-nars/8KnAbKzjp4E/rBc-6V5pem8J
    default Truth projectTruth(long targetTime, long now, boolean eternalizeIfWeaklyTemporal) {

        Truth currentTruth = truth();

        if ((targetTime == Tense.ETERNAL)) {

            return isEternal() ? currentTruth :
                    eternalize(currentTruth);

        } else {

            long occ = occurrence();
            if (occ == targetTime)
                return currentTruth;

            float conf = currentTruth.conf();

            float nextConf;
            long nextOcc = targetTime;

            float projConf = nextConf =
                    conf * temporalProjectionOld( targetTime, occ, now);

            if (eternalizeIfWeaklyTemporal) {
                float eternConf = eternalize(conf);

                if (projConf < eternConf) {
                    nextConf = eternConf;
                    nextOcc = Tense.ETERNAL;
                }
            }

            return new ProjectedTruth(currentTruth.freq(), nextConf, nextOcc);
        }
    }

//    final class ExpectationComparator implements Comparator<Task>, Serializable {
//        static final Comparator the = new ExpectationComparator();
//        @Override public int compare(@NotNull Task b, @NotNull Task a) {
//            return Float.compare(a.expectation(), b.expectation());
//        }
//    }
//
//    final class ConfidenceComparator implements Comparator<Task>, Serializable {
//        static final Comparator the = new ExpectationComparator();
//        @Override public int compare(@NotNull Task b, @NotNull Task a) {
//            return Float.compare(a.conf(), b.conf());
//        }
//    }
}
