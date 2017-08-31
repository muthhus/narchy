package nars;

import jcog.Texts;
import jcog.math.Interval;
import nars.concept.Concept;
import nars.op.Operation;
import nars.task.*;
import nars.task.util.InvalidTaskException;
import nars.term.Term;
import nars.term.Termed;
import nars.time.Tense;
import nars.truth.*;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.function.BiFunction;

import static nars.Op.*;
import static nars.op.DepIndepVarIntroduction.validIndepVarSuperterm;
import static nars.term.Terms.normalizedOrNull;
import static nars.time.Tense.ETERNAL;
import static nars.truth.TruthFunctions.c2w;
import static nars.truth.TruthFunctions.w2c;

/**
 * NAL Task to be processed, consists of a Sentence, stamp, time, and budget.
 */
public interface Task extends Tasked, Truthed, Stamp, Termed, ITask {


    static boolean equal(@NotNull Task a, @NotNull Task b) {

        @NotNull long[] evidence = a.stamp();

        if ((!Arrays.equals(evidence, b.stamp())))
            return false;

        if (evidence.length > 1) {
            if (!Objects.equals(a.truth(), b.truth()))
                return false;

            if ((a.start() != b.start()) || (a.end() != b.end()))
                return false;
        }

        if (a.punc() != b.punc())
            return false;

        return a.term().equals(b.term());
    }

    static void proof(@NotNull Task task, int indent, @NotNull StringBuilder sb) {
        //TODO StringBuilder

        for (int i = 0; i < indent; i++)
            sb.append("  ");
        task.appendTo(sb, null, true);
        sb.append("\n  ");


        if (task instanceof DerivedTask) {
            Task pt = ((DerivedTask) task).getParentTask();
            if (pt != null) {
                //sb.append("  PARENT ");
                proof(pt, indent + 1, sb);
            }

            Task pb = ((DerivedTask) task).getParentBelief();
            if (pb != null) {
                //sb.append("  BELIEF ");
                proof(pb, indent + 1, sb);
            }
        }
    }

    @Nullable
    static boolean taskContentValid(@NotNull Term t, byte punc, @Nullable NAR nar, boolean safe) {

        if (t.op() == NEG)
            //must be applied before instantiating Task
            return fail(t, "negation operator invalid for task term", safe);

        if (!t.hasAny(Op.ATOM.bit | Op.INT.bit | Op.VAR_PATTERN.bit))
            return fail(t, "filter terms which have been completely variable-ized", safe); //filter any terms that have been completely variable introduced

        if (punc != COMMAND) {
            if (!t.isNormalized())
                return fail(t, "task term not a normalized Compound", safe);


            if ((punc == Op.BELIEF || punc == Op.GOAL) && (t.hasVarQuery())) {
                return fail(t, "belief or goal with query variable", safe);
            }

            if (nar != null) {
                int maxVol = nar.termVolumeMax.intValue();
                if (t.volume() > maxVol)
                    return fail(t, "task term exceeds maximum volume", safe);

                int nalLevel = nar.nal();
                if (!t.levelValid(nalLevel))
                    return fail(t, "task term exceeds maximum NAL level", safe);
            }

//        if (t.op().temporal && t.dt() == XTERNAL) {
//            return fail(t, "top-level temporal term with dt=XTERNAL", safe);
//        }

            if (!(t.op().conceptualizable)) {
                return fail(t, "op not conceptualizable", safe);
            }
            if (Param.DEBUG && !t.conceptual().op().conceptualizable) {
                return fail(t, "term not conceptualizable", safe);
            }

            return (t.size() == 0) || validTaskCompound(t, punc, safe);
        }

        return true;
    }

    /**
     * call this directly instead of taskContentValid if the level, volume, and normalization have already been tested.
     * these can all be tested prenormalization, because normalization will not affect the result
     */
    static boolean validTaskCompound(@NotNull Term t, byte punc, boolean safe) {
        /* A statement sentence is not allowed to have a independent variable as subj or pred"); */

//        if (t.varDep()==1) {
//            return fail(t, "singular dependent variable", safe);
//        }

        switch (t.varIndep()) {
            case 0:
                break;  //OK
            case 1:
                return fail(t, "singular independent variable", safe);
            default:
                if (!t.hasAny(Op.StatementBits)) {
                    return fail(t, "InDep variables must be subterms of statements", safe);
                } else {
                    //TODO use a byte[] path thing to reduce duplicate work performed in indepValid findPaths
                    if (t.hasAny(Op.VAR_INDEP)) {
                        UnifiedSet unique = new UnifiedSet(1); //likely only one var indep repeated twice
                        if (!t.ANDrecurse(
                                v -> (v.op() != VAR_INDEP) || !unique.add(v) || indepValid(t, v))) {
                            return fail(t, "unbalanced InDep variable pairing", safe);
                        }
                    }
                }
        }


        Op op = t.op();
        if ((op == Op.IMPL) && (punc == Op.GOAL || punc == Op.QUEST))
            return fail(t, "Goal/Quest task term may not be Implication or Equivalence", safe);

        return true;
    }

    static boolean indepValid(@NotNull Term comp, @NotNull Term selected) {

        List<byte[]> pp = comp.pathsTo(selected);

        int pSize = pp.size();
        if (pSize == 0)
            return true; //a compound which didnt contain it

        byte[][] paths = pp.toArray(new byte[pSize][]);

        @Nullable ObjectByteHashMap<Term> m = new ObjectByteHashMap<>(pSize);
        for (int occurrence = 0; occurrence < pSize; occurrence++) {
            byte[] p = paths[occurrence];
            Term t = null; //root
            int pathLength = p.length;
            for (int i = -1; i < pathLength - 1 /* dont include the selected term itself */; i++) {
                t = (i == -1) ? comp : t.sub(p[i]);
                Op o = t.op();

                if (validIndepVarSuperterm(o)) {
                    byte inside = (byte) (1 << p[i + 1]);
                    m.updateValue(t, inside, (previous) -> (byte) ((previous) | inside));
                }
            }
        }

        return m.anySatisfy(b -> b == 0b11);

    }

    static boolean fail(@Nullable Term t, String reason, boolean safe) {
        if (safe)
            return false;
        else
            throw new InvalidTaskException(t, reason);
    }

    static long nearestStartOrEnd(long a, long b, long x, long y) {
        long u = nearestBetween(a, b, x);
        long v = nearestBetween(a, b, y);
        if (Math.min(Math.abs(u - a), Math.abs(u - b)) <
                Math.min(Math.abs(v - a), Math.abs(v - b))) {
            return u;
        } else {
            return v;
        }
    }

    static long nearestBetween(long s, long e, long when) {
        assert (when != ETERNAL);

        if (s == ETERNAL) {
            return when;
        } else if (e == s) {
            return s; //point
        } else if (when >= s && when <= e) {
            return when; //internal
        } else if (when < s) {
            return s; //at or beyond the start
        } else {
            return e; //at or beyond the end
        }

    }

    @Deprecated
    @Nullable
    static NALTask clone(@NotNull Task x, @NotNull Term newContent) {


        boolean negated = (newContent.op() == NEG);
        if (negated) {
            newContent = newContent.unneg();
        }

        if (!Task.taskContentValid(newContent, x.punc(), null, true)) {
            return null;
        }

        NALTask y = new NALTask(newContent, x.punc(),
                x.isBeliefOrGoal() ? (DiscreteTruth) x.truth().negIf(negated) : null,
                x.creation(),
                x.start(), x.end(),
                x.stamp());
        y.setPri(x);
        y.meta = x.meta();
        return y;
    }

//    @Nullable
//    static boolean taskStatementValid(@NotNull Compound t, boolean safe) {
//        return taskStatementValid(t, (byte) 0, safe); //ignore the punctuation-specific conditions
//    }

    static @Nullable Task execute(Task cmd, NAR nar) {


        Term inputTerm = cmd.term();
        if (inputTerm.hasAll(Op.EvalBits) && inputTerm.op() == INH) {
            Term func = inputTerm.sub(1);
            if (func != Null && func.op() == ATOM) {
                Term args = inputTerm.sub(0);
                if (args != Null && args.op() == PROD) {
                    Concept funcConcept = nar.concept(func);
                    if (funcConcept instanceof Operation) {
                        Operation o = (Operation) funcConcept;

                        Task result = o.run(cmd, nar);
                        if (result != null && result != cmd) {
                            //return input(result); //recurse
                            return execute(result, nar);
                        }

                        //                            } else {
//
//                                if (!cmd.isEternal() && cmd.start() > time() + time.dur()) {
//                                    inputAt(cmd.start(), cmd); //schedule for execution later
//                                    return null;
//                                } else {
//                                    if (executable(cmd)) {
//                                        Task result = o.run(cmd, this);
//                                        if (result != cmd) { //instance equality, not actual equality in case it wants to change this
//                                            if (result == null) {
//                                                return null; //finished
//                                            } else {
//                                                //input(result); //recurse until its stable
//                                                return result;
//                                            }
//                                        }
//                                    }
//                                }
//                            }


                    }
                }
            }
        }

//        if (isCommand())
//        nar.eventTask.emit(cmd);
        return cmd;

    }

    @Nullable
    static Task tryTask(@NotNull Term t, byte punc, Truth tr, BiFunction<Term, Truth, ? extends Task> res) {
        ObjectBooleanPair<Term> x = tryContent(t, punc, true);
        if (x != null) {
            return res.apply(x.getOne(), tr != null ? tr.negIf(x.getTwo()) : null);
        }
        return null;
    }

    /**
     * attempts to prepare a term for use as a Task content.
     *
     * @return null if unsuccessful, otherwise the resulting compound term and a
     * boolean indicating whether a truth negation occurred,
     * necessitating an inversion of truth when constructing a Task with the input term
     */
    @Nullable
    static ObjectBooleanPair<Term> tryContent(@NotNull Term t, byte punc, boolean safe) {

        Op o = t.op();

        boolean negated = false;
        if (o == NEG) {
            t = t.unneg();
            o = t.op();
            negated = !negated;
        }

        if (!o.conceptualizable)
            return null;

        if ((t = normalizedOrNull(t)) == null)
            return null;

        if (Task.taskContentValid(t, punc, null, safe))
            return PrimitiveTuples.pair(t, negated);
        else
            return null;

    }

    @Override
    long creation();

    @Override
    Term term();

    /**
     * occurrence starting time
     */
    @Override
    long start();

    /**
     * occurrence ending time
     */
    @Override
    long end();

    /**
     * amount of evidence measured at a given time with a given duration window
     *
     * @param when time
     * @param dur  duration period across which evidence can decay before and after its defined start/stop time
     * @return value >= 0 indicating the evidence
     */
    default float evi(long when, final int dur) {

        Truth t = truth();
        long a = start();


        if (a == ETERNAL)
            return t.evi();
        else if (when == ETERNAL)
            return t.eviEternalized();
        else {

            float cw = t.evi();
            long z = end();
            //assert (z >= a) : this + " has mismatched start/end times";

            if ((when >= a) && (when <= z)) {

                //full confidence

            } else {
                //nearest endpoint of the interval

                long touched =
                        //nearestTimeTo(when);
                        (a + z)/2; //midpoint: to be fair to other more precisely endured tasks

                long dist = Math.abs(when - touched);
                assert (dist > 0) : "what time is " + a + ".." + z + " supposed to mean relative to " + when;


                //experimental: decay slower according to complexity, ie. more complex learned rules will persist longer
                float durAdjusted = dur;// * (volume());

//                long r = z - a;
//                cw = r != 0 ?
//                        (cw / (1f + ((float) r) / dur)) //dilute the endured task in proportion to how many durations it consumes beyond point-like (=0)
//                        :
//                        cw;

                assert (dur > 0);
                cw = TruthPolation.evidenceDecay(cw, durAdjusted, dist); //decay
                //cw = 0; //immediate cut-off


                if (eternalizable()) {
                    float et = t.eviEternalized();
                    if (et > cw)
                        cw = et;
                }

            }

            return cw;

        }

    }

    default boolean eternalizable() {


        return false;
        //return true;
        //return term().vars() > 0;
        //return term().varIndep() > 0;
        //return term().varIndep() > 0 || term().op() == IMPL; //isAny(Op.IMPL.bit | Op.EQUI.bit);
        //return true;
        //return op().temporal;


        //Op op = term.op();
        //return op ==IMPL || op ==EQUI || term.vars() > 0;
        //return op.statement || term.vars() > 0;
    }

    @Override
    @NotNull
    default Task task() {
        return this;
    }

    default boolean isQuestion() {
        return (punc() == QUESTION);
    }

    default boolean isBelief() {
        return (punc() == BELIEF);
    }

//    /**
//     * called if this task is entered into a concept's belief tables
//     * TODO what about for questions/quests
//     */
//    void feedback(TruthDelta delta, float deltaConfidence, float deltaSatisfaction, NAR nar);
//

    default boolean isGoal() {
        return (punc() == GOAL);
    }

    default boolean isQuest() {
        return (punc() == QUEST);
    }

//    /** allows for budget feedback that occurrs on revision */
//    default boolean onRevision(Task conclusion) {
//        return true;
//    }

    default boolean isCommand() {
        return (punc() == COMMAND);
    }

    @Nullable
    default Appendable appendTo(Appendable sb) throws IOException {
        sb.append(appendTo(null, null));
        return sb;
    }

    @Nullable
    default Appendable toString(NAR memory, boolean showStamp) {
        return appendTo(new StringBuilder(), memory, showStamp);
    }

    @Nullable
    default Concept concept(@NotNull NAR n, boolean conceptualize) {
        Term t = term();
        Concept c = conceptualize ? n.conceptualize(t) : n.concept(t);
        if (c != null) {
            return c;
        }

        return null;
//        if (!(c instanceof TaskConcept)) {
//            throw new InvalidTaskException
//                    //System.err.println
//                    (this, "unconceptualized");
//        }
    }

// TODO
//    default long nearestTimeTo(Task otherTask) {
//        long s = start();
//        if (s == ETERNAL)
//            return ETERNAL;
//        long os = otherTask.start();
//        if (os == ETERNAL)
//            return s;
//
//
//        if (s == ETERNAL)
//            return ETERNAL;
//        return nearestStartOrEnd(s, end(), when);
//    }

    default boolean isQuestOrQuestion() {
        byte c = punc();
        return c == Op.QUESTION || c == Op.QUEST;
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

    default boolean isBeliefOrGoal() {
        byte c = punc();
        return c == Op.BELIEF || c == Op.GOAL;
    }

    /**
     * for question tasks: when an answer appears.
     * <p>
     * <p>
     * return the input task, or a modification of it to use a customized matched premise belief. or null to
     * to cancel any matched premise belief.
     */
    @Nullable
    default Task onAnswered(@NotNull Task answer, @NotNull NAR nar) {
//        if (isInput()) {
//            Concept concept = concept(nar, true);
//            if (concept != null) {
//                ArrayBag<Task, PriReference<Task>> answers = (ArrayBag<Task, PriReference<Task>>) concept.computeIfAbsent(Op.QUESTION, (q) ->
//                        new AnswerBag(nar, this, Param.MAX_INPUT_ANSWERS));
//                answers.commit();
//
//                float confEffective = answer.conf();//nearestTimeTo(nar.time()), nar.dur());
//                answers.put(new PLink<>(answer, confEffective * 1f));
//            }
//
//        }

        return answer;
    }

    /**
     * to the interval [x,y]
     */
    default long nearestTimeBetween(final long x, final long y) {
        long a = this.start();
        if (a == ETERNAL)
            return (x+y)/2; //use midpoint of the two if this task is eternal

        long b = this.end();
        if (x == y) {
            return nearestBetween(a, b, x);
        } else if (a == b) {
            return nearestBetween(x, y, a);
        } else if (x <= a && y >= b) {
            return (a + b) / 2; //midpoint of this within the range
        } else if ((x > a) && (y < b)) {
            return (x + y) / 2; //midpoint of the contained range
        } else {
            return nearestStartOrEnd(a, b, x, y); //overlap or no overlap
        }
    }

    default long nearestTimeTo(long when) {
        return nearestBetween(start(), end(), when);
    }

    @NotNull
    default Appendable toString(/**@Nullable*/NAR memory) {
        return appendTo(null, memory);

    }

    default @Nullable StringBuilder appendTo(@Nullable StringBuilder sb, /**@Nullable*/NAR memory) {
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

    @NotNull
    @Deprecated
    default StringBuilder appendTo(StringBuilder buffer, /**@Nullable*/NAR memory, boolean showStamp) {
        boolean notCommand = punc() != Op.COMMAND;
        return appendTo(buffer, memory, true, showStamp && notCommand,
                notCommand, //budget
                showStamp //log
        );
    }

    @NotNull
    default StringBuilder appendTo(@Nullable StringBuilder buffer, /**@Nullable*/@Nullable NAR memory, boolean term, boolean showStamp, boolean showBudget, boolean showLog) {

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
//        if (memory != null) {
//            tenseString = getTense(memory.time());
//        } else {
        //TODO dont bother craeting new StringBuilder and calculating the entire length etc.. just append it to a reusable StringReader?
        appendOccurrenceTime(
                (StringBuilder) (tenseString = new StringBuilder()));
//        }


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
            toBudgetStringExternal(buffer).append(' ');
        }

        buffer.append(contentName).append((char) punc());

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
    @Override
    default boolean isInput() {
        return stamp().length <= 1;
        //return evidence().length <= 1;
        //return (getParentTask() == null);
        //return (evidence().length <= 1) && ;
    }

    default boolean isEternal() {
        return start() == ETERNAL;
    }

    @Deprecated
    default String getTense(long currentTime) {

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

    default boolean cyclic() {
        return Stamp.isCyclic(stamp());
    }

    @Override
    default int dt() {
        return term().dt();
    }

    default float conf(long when, int dur) {
        float cw = evi(when, dur);
        return cw == cw ? w2c(cw) : Float.NaN;
    }

    @Nullable
    default Truth truth(long targetStart, long targetEnd, int dur, float minConf) {
        long t;
        if (targetStart == targetEnd) {
            t = targetStart;
        } else {
            t = distanceTo(targetStart) < distanceTo(targetEnd) ? targetStart : targetEnd;
        }
        return truth(t, dur, minConf);
    }

    default float evi(long targetStart, long targetEnd, final int dur) {
        long t;
        if (targetStart == targetEnd)
            t = targetStart;
        else
            t = distanceTo(targetStart) < distanceTo(targetEnd) ? targetStart : targetEnd;
        return evi(t, dur);
    }

    @Nullable
    default Truth truth(long when, int dur, float minConf) {
        float eve = evi(when, dur);
        if (eve == eve && eve >= c2w(minConf)) {



            {
                return new PreciseTruth(freq(), eve, false);

                //quantum entropy uncertainty:
//                float ff = freq();
//                ff = (float) Util.unitize(
//                        (ThreadLocalRandom.current().nextFloat() - 0.5f) *
//                                2f * Math.pow((1f-conf),4) + ff);
//                return $.t(ff, conf);
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
     * Compound
     * Punc
     * Freq (blank space if quest/question)
     * Conf (blank space if quest/question)
     * Start
     * End
     */
    default void appendTSV(Appendable a) throws IOException {

        char sep = '\t'; //','

        a
                .append(term().toString()).append(sep).append("\"").append(String.valueOf(punc())).append("\"").append(sep)
                .append(truth() != null ? Texts.n2(truth().freq()) : " ").append(sep)
                .append(truth() != null ? Texts.n2(truth().conf()) : " ").append(sep)
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
            meta(String.class, (exist = $.newArrayList(1)));
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

    @NotNull
    default StringBuilder proof(@NotNull StringBuilder temporary) {
        temporary.setLength(0);
        proof(this, 0, temporary);
        return temporary;
    }

    /**
     * auto budget by truth (if belief/goal, or punctuation if question/quest)
     */
    default Task budget(NAR nar) {
        return budget(1f, nar);
    }

    //    /**
//     * returns the time separating this task from a target time. if the target occurrs
//     * during this task, the distance is zero. if this task is eternal, then ETERNAL is returned
//     */
//    default long timeDistance(long t) {
//        long s = start();
//        if (s == ETERNAL) return ETERNAL;
//        long e = end();
//        if ((t >= s) || (t <= e)) return 0;
//        else if (t > e) return t - e;
//        else return s - t;
//    }

    default Task budget(float factor, NAR nar) {
        setPri(factor * nar.priDefault(punc()));
        return this;
    }

    default boolean during(long a, long b) {
        return distanceTo(a, b) == 0;
    }

    default boolean during(long when) {
        long start = start();
        if (start != ETERNAL) {
            if (start == when)
                return true;
            if (when >= start) {
                if (when <= end())
                    return true;
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    default @Nullable Iterable<? extends ITask> run(@NotNull NAR n) {

        n.emotion.busy(priElseZero(), this.volume());

        boolean evaluate = true; //!isCommand();

        if (evaluate) {

            Term x = term();

            Term y = x.eval(n.terms);

            if (!x.equals(y)) {
                @Nullable ObjectBooleanPair<Term> yy = tryContent(y, punc(), true);
                if (yy != null) {

                    /* the evaluated result here acts as a memoization of possibly many results
                       depending on whether the functor is purely static in which case
                       it would be the only one.
                     */
                    NALTask evaluated = clone(this, yy.getOne().negIf(yy.getTwo()));
                    if (evaluated != null) {
                        return evaluated.run(n);
                    }
                }


                return Collections.singleton(Operation.logTask(n.time(), $.p(x, y)));
            }
        }

        boolean cmd = isCommand();
        if (cmd || (isGoal() && !isEternal())) {
            Task exe = execute(this, n);
            if (exe!=this)
                return null; //done
        }

//        if (n.time instanceof FrameTime) {
//            //HACK for unique serial number w/ frameclock
//            ((FrameTime) n.time).validate(this.stamp());
//        }

        if (!cmd) {
            Concept c = concept(n, true);
            if (c != null) {
                c.process(this, n);
            }
        }

        return null;
    }

    /**
     * projected truth value
     */
    @Nullable
    default Truth truth(long when, int dur) {
        float e = evi(when, dur);
        if (e <= Float.MIN_NORMAL)
            return null;
        return new PreciseTruth(freq(), e, false);
    }

    /**
     * amount of time this spans for
     */
    default long range() {
        long s = start();
        return s != ETERNAL ? end() - s : 0;
    }

    default boolean isAfter(long when) {
        long x = nearestTimeTo(when);
        return x == ETERNAL || x > when;
    }

    default boolean isBefore(long when) {
        long x = nearestTimeTo(when);
        return x == ETERNAL || x < when;
    }
//
//    default boolean isPresentOf(long when) {
//        long x = nearestTimeTo(when);
//        return x == ETERNAL || x == when;
//    }
//
//    default boolean isPresentOf(long when, int dur) {
//        long x = nearestTimeTo(when);
//        return x == ETERNAL || Math.abs(x - when) <= dur;
//    }

    /**
     * TODO cause should be merged if possible when merging tasks in belief table or otherwise
     */
    short[] cause();

    default long distanceTo(long when) {
        return Math.abs(when - nearestTimeTo(when));
    }

    /**
     * TODO see if this can be made faster
     */
    default long distanceTo(long start, long end) {

        if (start == end) {
            return distanceTo(start);
        } else {
            long s = this.start();
            if (s == ETERNAL) return 0;

            assert (start != ETERNAL);

            long e = this.end();

            if (Interval.intersectLength(s, e, start, end) >= 0)
                return 0; //intersects
            else {
                return Interval.unionLength(s, e, start, end) - (end - start) - (e - s);
            }
        }
    }
}