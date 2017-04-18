package nars;

import jcog.Texts;
import jcog.bag.impl.ArrayBag;
import jcog.map.SynchronizedHashMap;
import jcog.pri.*;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.op.Command;
import nars.task.ImmutableTask;
import nars.task.Tasked;
import nars.task.TruthPolation;
import nars.task.util.InvalidTaskException;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.var.Variable;
import nars.time.Tense;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.TruthDelta;
import nars.truth.Truthed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static nars.Op.*;
import static nars.term.Terms.compoundOrNull;
import static nars.time.Tense.ETERNAL;
import static nars.truth.TruthFunctions.w2c;

/**
 * A task to be processed, consists of a Sentence and a BudgetValue.
 * A task references its parent and an optional causal factor (usually an Operation instance).  These are implemented as WeakReference to allow forgetting via the
 * garbage collection process.  Otherwise, Task ancestry would grow unbounded,
 * violating the assumption of insufficient resources (AIKR).
 * <p>
 * TODO decide if the Sentence fields need to be Reference<> also
 */
public interface Task extends Tasked, Truthed, Stamp, Termed<Compound>, Priority, Prioritized {


    byte punc();


    @Override
    long creation();

    @NotNull
    @Override
    Compound term();

    /** occurrence starting time */
    @Override
    long start();

    /** occurrence ending time */
    @Override
    long end();

    /**
     * amount of evidence aka evidence weight
     * @param when time
     * @param dur duration period across which evidence can decay before and after its defined start/stop time
     * @return value >= 0 indicating the evidence
     */
    default float evi(long when, int dur) {

        Truth t = truth();
        long a = start();
        float cw = t.evi();

        if (a == ETERNAL)
            return cw;
        else if (when == ETERNAL)
            return t.eternalizedEvi();
        else {
            long z = end();

            if ((when >= a) && (when <= z)) {

                //full confidence

            } else {
                //nearest endpoint of the interval
                if (dur > 0)
                    cw = TruthPolation.evidenceDecay(cw, dur, Math.min(Math.abs(a - when), Math.abs(z - when)));
                else
                    cw = 0;

                if (eternalizable(term())) {
                    float et = t.eternalizedEvi();
                    if (et > cw)
                        cw = et;
                }
            }

            return cw;

        }

    }

    static boolean eternalizable(Term term) {

        return term.varIndep() > 0;
        //return term.vars() > 0;
        //return false;
        //return true;
        //return op().temporal;


        //Op op = term.op();
        //return op ==IMPL || op ==EQUI || term.vars() > 0;
        //return op.statement || term.vars() > 0;
    }



    static boolean equivalentTo(@NotNull Task a, @NotNull Task b, boolean punctuation, boolean term, boolean truth, boolean stamp, boolean occurrenceTime) {

        @NotNull long[] evidence = a.stamp();

        if (stamp && (!Arrays.equals(evidence, b.stamp())))
            return false;

        if (evidence.length > 1) {
            if (occurrenceTime && (a.start() != b.start()) || (a.end() != b.end()))
                return false;

            if (truth && !Objects.equals(a.truth(), b.truth()))
                return false;
        }

        if (term && !a.term().equals(b.term()))
            return false;

        if (punctuation && (a.punc() != b.punc()))
            return false;

        return true;
    }


    static void proof(@NotNull Task task, int indent, @NotNull StringBuilder sb) {
        //TODO StringBuilder

            for (int i = 0; i < indent; i++)
                sb.append("  ");
            task.appendTo(sb, null, true);
            sb.append("\n  ");



        Task pt = task.getParentTask();
        if (pt != null) {
            //sb.append("  PARENT ");
            proof(pt, indent + 1, sb);
        }

        Task pb = task.getParentBelief();
        if (pb != null) {
            //sb.append("  BELIEF ");
            proof(pb, indent + 1, sb);
        }
    }

    @Nullable
    static boolean taskContentValid(@NotNull Compound t, byte punc, @Nullable NAR nar, boolean safe) {
        if (!t.isNormalized())
            return test(t, "Task Term is null or not a normalized Compound", safe);

        if (nar!=null) {
            int maxVol = nar.termVolumeMax.intValue();
            if (t.volume() > maxVol)
                return test(t, "Term exceeds maximum volume", safe);

            int nalLevel = nar.level();
            if (!t.levelValid(nalLevel))
                return test(t, "Term exceeds maximum NAL level", safe);
        }

        //if (Param.DEBUG) {
        if (t.containsTerm(True) || t.containsTerm(False))
            throw new InvalidTaskException(t, "term contains True or False");
        //}

        if ((punc == Op.BELIEF || punc == Op.GOAL) && (t.hasVarQuery())) {
            return test(t, "Belief or goal with query variable", safe);
        }

        return taskStatementValid(t, punc, safe);
    }

    /** call this directly instead of taskContentValid if the level, volume, and normalization have already been tested.
     * these can all be tested prenormalization, because normalization will not affect the result
     * */
    @Nullable static boolean taskStatementValid(@NotNull Compound t, byte punc, boolean safe) {
        /* A statement sentence is not allowed to have a independent variable as subj or pred"); */
        Op op = t.op();

        switch (t.varIndep()) {
            case 0:
                break;  //OK
            case 1:
                return test(t, "singular independent variable must be balanced elsewhere", safe);
            default:
                if (!t.hasAny(Op.StatementBits))
                    return test(t, "Independent variables require statements super-terms", safe);
                else if (op.statement && t.hasVarIndep()) {
                    Term subj = t.term(0);
                    if (subj.op()==VAR_INDEP)
                        return test(t, "Statement Task's subject is VAR_INDEP", safe);
                    if (subj.varIndep() == 0)
                        return test(t, "Statement Task's subject has no VAR_INDEP", safe);
                    Term pred = t.term(1);
                    if (pred.op()==VAR_INDEP)
                        return test(t, "Statement Task's predicate is VAR_INDEP", safe);
                    if (pred.varIndep() == 0)
                        return test(t, "Statement Task's predicate has no VAR_INDEP", safe);
                }

                //TODO more thorough test for invalid independent-variable containing compounds
                // DepIndepVarIntroduction does this, adapt/share code from there

                break;
        }


        if ((punc == Op.GOAL || punc == Op.QUEST) && (op == Op.IMPL || op == Op.EQUI))
            return test(t, "Goal/Quest task term may not be Implication or Equivalence", safe);

        return true;
    }

    @Nullable
    static boolean test(@Nullable Term t, String reason, boolean safe) {
        if (safe)
            return false;
        else
            throw new InvalidTaskException(t, reason);
    }


    @Override
    @NotNull
    default Task task() {
        return this;
    }



    default boolean isQuestion() { return (punc() == QUESTION);     }
    default boolean isBelief() {
        return (punc() == BELIEF);
    }
    default boolean isGoal() {
        return (punc() == GOAL);
    }
    default boolean isQuest() {
        return (punc() == QUEST);
    }

    default boolean isCommand() {
        return (punc() == COMMAND);
    }

    @Nullable
    default Appendable appendTo(Appendable sb) throws IOException {
        sb.append(appendTo(null, null));
        return sb;
    }

    @Nullable
    default Appendable toString(NAR memory, boolean showStamp) throws IOException {
        return appendTo(new StringBuilder(), memory, showStamp);
    }

    @Nullable
    default TaskConcept concept(@NotNull NAR n) {
        Concept c = n.concept(term(), true);
        if (!(c instanceof TaskConcept)) {
            //if (Param.DEBUG_EXTRA)
                throw new RuntimeException
                //System.err.println
                        ("should conceptualize to TaskConcept: " + c);
            //else
            //return null;
        }
        return (TaskConcept) c;
    }

    /**
     * called if this task is entered into a concept's belief tables
     * TODO what about for questions/quests
     */
    void feedback(TruthDelta delta, float deltaConfidence, float deltaSatisfaction, NAR nar);



    default boolean isQuestOrQuestion() {
        byte c = punc();
        return c == Op.QUESTION || c == Op.QUEST;
    }

    default boolean isBeliefOrGoal() {
        byte c = punc();
        return c == Op.BELIEF || c == Op.GOAL;
    }

//    /** allows for budget feedback that occurrs on revision */
//    default boolean onRevision(Task conclusion) {
//        return true;
//    }

    /**
     * for question tasks: when an answer appears.
     * <p>
     * <p>
     * return the input task, or a modification of it to use a customized matched premise belief. or null to
     * to cancel any matched premise belief.
     */
    @Nullable default Task onAnswered(@NotNull Task answer, @NotNull NAR nar) {
        if (Param.ANSWER_REPORTING && isInput()) {
            ArrayBag<Task> answers = concept(nar).computeIfAbsent(Op.QUESTION, () ->
                    new ArrayBag<>(PriMerge.max,
                            new SynchronizedHashMap<>()).capacity(Param.MAX_INPUT_ANSWERS)
            );
            float confEffective = answer.conf(nearestStartOrEnd(nar.time()), nar.dur());

            if (!answers.contains(answer)) {

                answers.commit();

                PLink<Task> insertion = answers.put(new RawPLink<>(answer, confEffective * 1f));

                if (insertion != null) {
                    Command.log(nar, this.toString() + "  " + answer.toString());
                }
            }
        }

        return answer;
    }

    default long nearestStartOrEnd(long when) {
        long s = start();
        if (s == ETERNAL)
            return ETERNAL;
        long e = end();

        if (when >= s && when <= e) {
            return when; //internal
        } else if (Math.abs(when-s) <= Math.abs(when-e)) {
            return s; //closer or beyond the start
        } else {
            return e; //closer or beyond the end
        }
    }

    /** time difference to neareset 'temporal tangent' */
    default long nearestStartOrEndTime(long when) {
        long n = nearestStartOrEnd(when);
        return Math.abs(when - n);
    }


//    @NotNull
//    default Task projectTask(long when, long now) {
//        Truth adjustedTruth = projectTruth(when, now, false);
//        long occ = occurrence();
//        long projOcc = (adjustedTruth instanceof ProjectedTruth) ? ((ProjectedTruth)adjustedTruth).when : occ;
//        return /*occ == projOcc &&*/ adjustedTruth.equals(truth()) ? this :
//                MutableTask.project(this, adjustedTruth, now, projOcc);
//
//    }


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


//    final class Solution extends AtomicReference<Task> {
//        Solution(Task referent) {
//            super(referent);
//        }
//
//        @NotNull
//        @Override
//        public String toString() {
//            return "Solved: " + get();
//        }
//    }

    @NotNull
    default Appendable toString(/**@Nullable*/NAR memory) {
            return appendTo(null, memory);

    }

    default @Nullable StringBuilder appendTo(@Nullable StringBuilder sb, /**@Nullable*/NAR memory)  {
        return appendTo(sb, memory, false);
    }

    @NotNull
    @Deprecated
    default String toStringWithoutBudget() {
        return toStringWithoutBudget(null);
    }

    @NotNull
    @Deprecated
    default String toStringWithoutBudget(NAR memory) {
        StringBuilder b = new StringBuilder();
            appendTo(b, memory, true, false,
                    false, //budget
                    false//log
            );
            return b.toString();

    }

    @Nullable
    @Deprecated
    default StringBuilder appendTo(StringBuilder buffer, /**@Nullable*/NAR memory, boolean showStamp)  {
        boolean notCommand = punc() != Op.COMMAND;
        return appendTo(buffer, memory, true, showStamp && notCommand,
                notCommand, //budget
                showStamp //log
        );
    }

    @Nullable
    default StringBuilder appendTo(@Nullable StringBuilder buffer, /**@Nullable*/@Nullable NAR memory, boolean term, boolean showStamp, boolean showBudget, boolean showLog)  {

        String contentName;
        if (term) {
            try {
                contentName = term().toString();
            } catch (Throwable t) {
                contentName = t.toString();
            }

        } else {
            contentName = "";
        }

        CharSequence tenseString;
        if (memory != null) {
            tenseString = getTense(memory.time());
        } else {
            //TODO dont bother craeting new StringBuilder and calculating the entire length etc.. just append it to a reusable StringReader?
            appendOccurrenceTime(
                    (StringBuilder) (tenseString = new StringBuilder()));
        }


        CharSequence stampString = showStamp ? stampAsStringBuilder() : null;

        int stringLength = contentName.length() + tenseString.length() + 1 + 1;

        Truth tt = truth();
        if (tt != null)
            stringLength += 11;

        if (showStamp)
            stringLength += stampString.length() + 1;

        /*if (showBudget)*/
        //"$0.8069;0.0117;0.6643$ "
        stringLength += 1 + 6 + 1 + 6 + 1 + 6 + 1 + 1;

        String finalLog;
        if (showLog) {
            Object ll = lastLogged();

            finalLog = (ll != null ? ll.toString() : null);
            if (finalLog != null)
                stringLength += finalLog.length() + 1;
            else
                showLog = false;
        } else
            finalLog = null;


        if (buffer == null)
            buffer = new StringBuilder(stringLength);
        else {
            buffer.ensureCapacity(stringLength);
        }


        if (showBudget) {
            priority().toBudgetStringExternal(buffer).append(' ');
        }

        buffer.append(contentName).append((char)punc());

        if (tenseString.length() > 0)
            buffer.append(' ').append(tenseString);

        if (tt != null) {
            buffer.append(' ');
            tt.appendString(buffer, 2);
        }

        if (showStamp) {
            buffer.append(' ').append(stampString);
        }

        if (showLog) {
            buffer.append(' ').append(finalLog);
        }

        return buffer;
    }







    /**
     * Check if a Task is a direct input,
     * or if its origin has been forgotten or never known
     */
    default boolean isInput() {
        return stamp().length <= 1;
        //return evidence().length <= 1;
        //return (getParentTask() == null);
        //return (evidence().length <= 1) && ;
    }


    default boolean isEternal() {
        return start() == ETERNAL;
    }


    @Deprecated default String getTense(long currentTime) {

        long ot = start();

        if (ot == ETERNAL) {
            return "";
        }

        switch (Tense.order(currentTime, ot, 1)) {
            case 1:
                return Op.TENSE_FUTURE;
            case -1:
                return Op.TENSE_PAST;
            default:
                return Op.TENSE_PRESENT;
        }
    }


    @Nullable
    default Term term(int i) {
        return term().term(i);
    }

    @Nullable
    default Task getParentTask() {
        return null;
    }

    @Nullable
    default Task getParentBelief() {
        return null;
    }

    default boolean cyclic() {
        return Stamp.isCyclic(stamp());
    }

    default int dt() {
        return term().dt();
    }

    default float conf(long when, int dur) {
        float cw = evi(when, dur);
        return cw == cw ? w2c(cw) : Float.NaN;
    }

    @Nullable
    default Truth truth(long when, int dur, float minConf) {
        float cw = evi(when, dur);
        if (cw == cw && cw > 0) {

            float conf = w2c(cw);
            if (conf > minConf) {
                return $.t(freq(), conf);
            }
        }
        return null;
    }




    default long mid() {
        long s = start();
        return (s != ETERNAL) ? ((s + end()) / 2L) : ETERNAL;
    }


    /**
     * prints this task as a TSV/CSV line.  fields:
     *      Compound
     *      Punc
     *      Freq (blank space if quest/question)
     *      Conf (blank space if quest/question)
     *      Start
     *      End
     */
    default void appendTSV(Appendable a) throws IOException {

        char sep = '\t'; //','

        a
                .append(term().toString()).append(sep)
                .append("\"" + punc() + "\"").append(sep)
                .append(truth()!=null ? Texts.n2(truth().freq()) : " ").append(sep)
                .append(truth()!=null ? Texts.n2(truth().conf()) : " ").append(sep)
                .append(!isEternal() ? Long.toString(start()) : " ").append(sep)
                .append(!isEternal() ? Long.toString(end()) : " ").append(sep)
                .append(proof().replace("\n", "  ")).append(sep)
                .append('\n');

    }

    /**
     * append an entry to this task's log history
     * useful for debugging but can also be applied to meta-analysis
     * ex: an entry might be a String describing a change in the story/history
     * of the Task and the reason for it.
     */
    @NotNull
    default Task log(@Nullable Object entry) {
        if (!(entry == null || !Param.DEBUG_TASK_LOG))
            getOrCreateLog().add(entry);
        return this;
    }

    @Nullable
    static ImmutableTask clone(@NotNull Task x, long created, long start, long end) {
        Priority b = x.priority().clone(); //snapshot its budget
        if (b.isDeleted())
            return null;

        ImmutableTask y = new ImmutableTask(x.term(), x.punc(), x.truth(), created, start, end, x.stamp());
        y.copyFrom(b);
        y.meta = x.meta();
        return y;
    }

    @Nullable
    static ImmutableTask clone(@NotNull Task x, @NotNull Compound newContent) {
//        if (!y.isNormalized()) {
//            y = (Compound) nar.normalize(y);
//            if (y == null)
//                return null;
//        }

        Priority b = x.priority().clone(); //snapshot its budget
        if (b.isDeleted())
            return null;

        boolean negated = (newContent.op() == NEG);
        if (negated)
            newContent = compoundOrNull(newContent.unneg());

        if (newContent == null)
            return null;

        ImmutableTask y = new ImmutableTask(newContent, x.punc(), x.truth().negIf(negated), x.creation(), x.start(), x.end(), x.stamp());
        y.copyFrom(b);
        y.meta = x.meta();
        return y;

    }

    Map meta();

    <X> X meta(Object key);

    void meta(Object key, Object value);

    @Nullable
    default List log() {
        return meta(String.class);
    }

    @NotNull
    default List getOrCreateLog() {

        List exist = log();
        if (exist == null) {
            meta(String.class,  (exist = $.newArrayList(1)) );
        }
        return exist;
    }



    @Nullable
    default Object lastLogged() {
        List log = log();
        return log == null || log.isEmpty() ? null : log.get(log.size() - 1);
    }


    @NotNull
    default String proof() {
        StringBuilder sb = new StringBuilder(512);
        return proof(sb).toString();
    }

    @NotNull
    default StringBuilder proof(@NotNull StringBuilder temporary) {
        temporary.setLength(0);
        proof(this, 0, temporary);
        return temporary;
    }


    /** auto budget by truth (if belief/goal, or punctuation if question/quest) */
    default Task budget(NAR nar) {
        return budget(1f, nar);
    }

    default Task budget(float factor, NAR nar) {
        setPriority(factor * nar.priorityDefault(punc()));
        return this;
    }

    //    default Task eternalized() {
//        if (isEternal()) {
//            return this;
//        } else {
//            Truth t = truth();
//            ImmutableTask y = ImmutableTask.Eternal(term(), punc(), t!=null ? t.eternalized() : null, creation(), stamp());
//            y.budgetSafe(budget());
//            return y;
//        }
//    }

    default boolean contains(long when) {
        long start = start();

        if (start == when) return true;

        if (start != ETERNAL) {
            if (when >= start) {
                if (when <= end())
                    return true;
            }
        }
        return false;
    }

    /** prepares a term for use as a Task's content */
    @Nullable static Compound content(@Nullable final Term r, NAR nar)  {
        if (r == null)
            return null;

        //unnegate and check for an apparent atomic term which may need decompressed in order to be the task's content
        boolean negated;
        Term s = r;
        if (r.op() == NEG) {
            s = r.unneg();
            if (s instanceof Variable)
                return null; //throw new InvalidTaskException(r, "unwrapped variable"); //should have been prevented earlier

            negated = true;
            if (s instanceof Compound) {
                return (Compound) r; //its normal compound inside the negation, handle it in Task constructor
            }
        } else if (r instanceof Compound) {
            return (Compound) r; //do not uncompress any further
        } else if (r instanceof Variable) {
            return null;
        } else {
            negated = false;
        }

        if (!(s instanceof Compound)) {
            Compound t = compoundOrNull(nar.post(s));
            if (t == null)
                return null; //throw new InvalidTaskException(r, "undecompressible");
            else
                return (Compound) $.negIf(t, negated); //done
//            else
//            else if (s.op()==NEG)
//                return (Compound) $.negIf(post(s.unneg(), nar));
//            else
//                return (Compound) $.negIf(s, negated);
        }
        //its a normal negated compound, which will be unnegated in task constructor
        return (Compound) s;
    }

    /** returns the time separating this task from a target time. if the target occurrs
     *  during this task, the distance is zero. if this task is eternal, then ETERNAL is returned
     */
    default long timeDistance(long t) {
        long s = start();
        if (s == ETERNAL) return ETERNAL;
        long e = end();
        if ((t >= s) || (t <= e)) return 0;
        else if (t > e) return t - e;
        else return s - t;
    }

}
