package nars;

import jcog.Texts;
import jcog.bag.impl.ArrayBag;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.Priority;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.index.term.TermIndex;
import nars.op.Command;
import nars.task.*;
import nars.task.util.AnswerBag;
import nars.task.util.InvalidTaskException;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.var.Variable;
import nars.time.Tense;
import nars.truth.*;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static nars.Op.*;
import static nars.op.DepIndepVarIntroduction.validIndepVarSuperterm;
import static nars.term.Terms.compoundOrNull;
import static nars.term.Terms.normalizedOrNull;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.XTERNAL;
import static nars.truth.TruthFunctions.c2w;
import static nars.truth.TruthFunctions.w2c;

/**
 * NAL Task to be processed, consists of a Sentence, stamp, time, and budget.
 */
public interface Task extends Tasked, Truthed, Stamp, Termed<Compound>, ITask {


    @Override
    long creation();

    @NotNull
    @Override
    Compound term();

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
        float cw = t.evi();

        if (a == ETERNAL)
            return cw;
        else if (when == ETERNAL)
            return t.eviEternalized();
        else {
            long z = end();

            if ((when >= a) && (when <= z)) {

                //full confidence

            } else {
                //nearest endpoint of the interval
                assert (dur > 0);
                long dist = a != z ? Math.min(Math.abs(a - when), Math.abs(z - when)) : Math.abs(a - when);
                if (dist > 0) {
                    cw = TruthPolation.evidenceDecay(cw, dur, dist); //decay
                    //cw = 0; //immediate cut-off
                }

                if (eternalizable(term())) {
                    float et = t.eviEternalized();
                    if (et > cw)
                        cw = et;
                }

            }

            return cw;

        }

    }

    static boolean eternalizable(Term term) {

        return true;
        //return false;
        //return term.varIndep() > 0;
        //return term.vars() > 0;
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

        return !punctuation || a.punc() == b.punc();
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
    static boolean taskContentValid(@NotNull Compound t, byte punc, @Nullable NAR nar, boolean safe) {
        if (!t.isNormalized())
            return fail(t, "Task Term not a normalized Compound", safe);

        if (nar != null) {
            int maxVol = nar.termVolumeMax.intValue();
            if (t.volume() > maxVol)
                return fail(t, "Term exceeds maximum volume", safe);

            int nalLevel = nar.level();
            if (!t.levelValid(nalLevel))
                return fail(t, "Term exceeds maximum NAL level", safe);
        }

        if (t.op().temporal && t.dt() == XTERNAL) {
            return fail(t, "top-level temporal term with dt=XTERNAL", safe);
        }

        if (Param.DEBUG) {
            if (t.ORrecurse(Op::isAbsolute)) // t.contains(True) || t.contains(False))
                throw new InvalidTaskException(t, "term contains True or False");
        }

        if ((punc == Op.BELIEF || punc == Op.GOAL) && (t.hasVarQuery())) {
            return fail(t, "Belief or goal with query variable", safe);
        }


        return taskStatementValid(t, punc, safe);
    }

//    @Nullable
//    static boolean taskStatementValid(@NotNull Compound t, boolean safe) {
//        return taskStatementValid(t, (byte) 0, safe); //ignore the punctuation-specific conditions
//    }

    /**
     * call this directly instead of taskContentValid if the level, volume, and normalization have already been tested.
     * these can all be tested prenormalization, because normalization will not affect the result
     */
    @Nullable
    static boolean taskStatementValid(@NotNull Compound t, byte punc, boolean safe) {
        /* A statement sentence is not allowed to have a independent variable as subj or pred"); */
        Op op = t.op();

        switch (t.varIndep()) {
            case 0:
                break;  //OK
            case 1:
                return fail(t, "singular independent variable must be balanced elsewhere", safe);
            default:
                if (!t.hasAny(Op.StatementBits)) {
                    return fail(t, "Independent variables require statements super-terms", safe);
                } else {
                    //TODO use a byte[] path thing to reduce duplicate work performed in indepValid findPaths
                    if (t.hasAny(Op.VAR_INDEP)) {
                        UnifiedSet unique = new UnifiedSet(1); //likely only one var indep repeated twice
                        if (!t.ANDrecurse(
                                v -> (v.op() != VAR_INDEP) || !unique.add(v) || indepValid(t, v))) {
                            return fail(t, "Mismatched cross-statement pairing of InDep variables", safe);
                        }
                    }
                }
        }


        if ((punc == Op.GOAL || punc == Op.QUEST) && (op == Op.IMPL || op == Op.EQUI))
            return fail(t, "Goal/Quest task term may not be Implication or Equivalence", safe);

        return true;
    }


    static boolean indepValid(@NotNull Compound comp, @NotNull Term selected) {

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
                t = (i == -1) ? comp : ((Compound) t).sub(p[i]);
                Op o = t.op();

                if (validIndepVarSuperterm(o)) {
                    byte inside = (byte) (1 << p[i + 1]);
                    m.updateValue(t, inside, (previous) -> (byte) ((previous) | inside));
                }
            }
        }

        return m.anySatisfy(b -> b == 0b11);

    }


    @Nullable
    static boolean fail(@Nullable Term t, String reason, boolean safe) {
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


    default boolean isQuestion() {
        return (punc() == QUESTION);
    }

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
    default Appendable toString(NAR memory, boolean showStamp) {
        return appendTo(new StringBuilder(), memory, showStamp);
    }

    @Nullable
    default TaskConcept concept(@NotNull NAR n) {
        return concept(n, false);
    }

    @Nullable
    default TaskConcept concept(@NotNull NAR n, boolean conceptualize) {
        Concept c = conceptualize ? n.conceptualize(term()) : n.concept(term());
        if (c instanceof TaskConcept)
            return ((TaskConcept) c);
        else
            return null;
//        if (!(c instanceof TaskConcept)) {
//            throw new InvalidTaskException
//                    //System.err.println
//                    (this, "unconceptualized");
//        }
    }

//    /**
//     * called if this task is entered into a concept's belief tables
//     * TODO what about for questions/quests
//     */
//    void feedback(TruthDelta delta, float deltaConfidence, float deltaSatisfaction, NAR nar);
//

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
    @Nullable
    default Task onAnswered(@NotNull Task answer, @NotNull NAR nar) {
        if (isInput()) {
            TaskConcept concept = concept(nar);
            if (concept != null) {
                ArrayBag<Task, PriReference<Task>> answers = concept.computeIfAbsent(Op.QUESTION, () ->
                        new AnswerBag(nar, this, Param.MAX_INPUT_ANSWERS));
                answers.commit();

                float confEffective = answer.conf(nearestStartOrEnd(nar.time()), nar.dur());
                answers.put(new PLink<>(answer, confEffective * 1f));
            }

        }

        return answer;
    }


    /** to the interval [x,y] */
    default long nearestStartOrEnd(long x, long y) {
        long a = this.start();
        if (a == ETERNAL)
            return ETERNAL;
        long b = this.end();
        if (x == y) {
            return nearestStartOrEnd(a, b, x);
        } else {
            if (a == b) {
                return nearestStartOrEnd(x, y, a);
            } else {
                //HACK there is probably a better way
                long u = nearestStartOrEnd(a, b, x);
                long v = nearestStartOrEnd(a, b, y);
                if (Math.min(Math.abs(u-a),Math.abs(u-b)) <
                    Math.min(Math.abs(v-a),Math.abs(v-b))) {
                    return u;
                } else {
                    return v;
                }
            }
        }
    }

    default long nearestStartOrEnd(long when) {
        long s = start();
        if (s == ETERNAL)
            return ETERNAL;
        return nearestStartOrEnd(s, end(), when);
    }

    static long nearestStartOrEnd(long s, long e, long when) {
        assert (s != ETERNAL);

        if (e == s) {
            return s; //point
        } else if (when >= s && when <= e) {
            return when; //internal
        } else if (Math.abs(when - s) <= Math.abs(when - e)) {
            return s; //at or beyond the start
        } else {
            return e; //at or beyond the end
        }

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

    @Nullable
    @Deprecated
    default StringBuilder appendTo(StringBuilder buffer, /**@Nullable*/NAR memory, boolean showStamp) {
        boolean notCommand = punc() != Op.COMMAND;
        return appendTo(buffer, memory, true, showStamp && notCommand,
                notCommand, //budget
                showStamp //log
        );
    }

    @Nullable
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


    @Nullable
    default Term term(int i) {
        return term().sub(i);
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
        float eve = evi(when, dur);
        if (eve == eve && eve > 0) {


            float minEve = c2w(minConf);
            if (eve >=  minEve) {
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

    @Nullable
    static NALTask clone(@NotNull Task x, long created, long start, long end) {
        Priority b = x.priority().clonePri(); //snapshot its budget
        if (b.isDeleted())
            return null;

        NALTask y = new NALTask(x.term(), x.punc(), x.truth(), created, start, end, x.stamp());
        y.setPri(b);
        //        if (srcCopy == null) {
//            delete();
//        } else {
//            float p = srcCopy.priSafe(-1);
//            if (p < 0) {
//                delete();
//            } else {
//                setPriority(p);
//            }
//        }
//
//        return this;
        y.meta = x.meta();
        return y;
    }

    @Nullable
    static NALTask clone(@NotNull Task x, @NotNull Compound newContent) {

        boolean negated = (newContent.op() == NEG);
        if (negated) {
            newContent = compoundOrNull(newContent.unneg());
            if (newContent == null)
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

    default Task budget(float factor, NAR nar) {
        setPri(factor * nar.priorityDefault(punc()));
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

    /**
     * prepares a term for use as a Task's content
     */
    @Deprecated
    @Nullable
    static Compound content(@Nullable final Term r, NAR nar) {
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

    /**
     * returns the time separating this task from a target time. if the target occurrs
     * during this task, the distance is zero. if this task is eternal, then ETERNAL is returned
     */
    default long timeDistance(long t) {
        long s = start();
        if (s == ETERNAL) return ETERNAL;
        long e = end();
        if ((t >= s) || (t <= e)) return 0;
        else if (t > e) return t - e;
        else return s - t;
    }

    @Override
    default ITask[] run(@NotNull NAR n) {

        float inputPri = this.pri();
        if (inputPri != inputPri)
            return DeleteMe; //deleted

        n.emotion.busy(inputPri, this.volume());

        //elide if DerivedTask since it has already been evaluated
        boolean evaluate = !(this instanceof DerivedTask) || isCommand();

        if (evaluate) {
            Compound x = term();
            Compound y = compoundOrNull(
                    x.eval(n.terms)
            );

            if (y != x) {
                if (y == null)
                    throw new InvalidTaskException(this, "un-evaluable");


                if (!x.equals(y)) {
                    NALTask inputY = clone(this, y);
                    assert (inputY != null);

                    delete(); //transfer control to transformation result

                    return inputY.run(n);
                }
            }
        }

        if (isCommand()) {
            execute(this, n);
            return null; //done
        }

//        if (n.time instanceof FrameTime) {
//            //HACK for unique serial number w/ frameclock
//            ((FrameTime) n.time).validate(this.stamp());
//        }

        TaskConcept c = concept(n, true);
        if (c != null) {
            c.process(this, n);
        }

        return null;
    }


    static @Nullable Task execute(Task cmd, NAR nar) {


        Compound inputTerm = cmd.term();
        if (inputTerm.hasAll(Op.EvalBits) && inputTerm.op() == INH) {
            Term func = inputTerm.sub(1);
            if (func.op() == ATOM) {
                Term args = inputTerm.sub(0);
                if (args.op() == PROD) {
                    Concept funcConcept = nar.concept(func);
                    if (funcConcept instanceof Command) {
                        Command o = (Command) funcConcept;

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

        /*if (isCommand)*/
        nar.eventTaskProcess.emit(cmd);
        return null;

    }


    /**
     * projected truth value
     */
    @Nullable
    default Truth truth(long when, int dur) {
        float e = evi(when, dur);
        if (e <= 0)
            return null;
        return new PreciseTruth(freq(), e, false);
    }

    /**
     * attempts to prepare a term for use as a Task content.
     *
     * @return null if unsuccessful, otherwise the resulting compound term and a
     * boolean indicating whether a truth negation occurred,
     * necessitating an inversion of truth when constructing a Task with the input term
     */
    @Nullable
    static ObjectBooleanPair<Compound> tryContent(@NotNull Term t, byte punc, TermIndex index) {
        @Nullable Compound cc = compoundOrNull(t);
        if (cc == null)
            return null;

        boolean negated = false;

        if (cc.op() == NEG) {
            cc = compoundOrNull(cc.unneg());
            if (cc == null)
                return null;

            negated = !negated;
        }

        if ((cc = normalizedOrNull(cc, index)) == null)
            return null;

        if (Task.taskContentValid(cc, punc, null, true))
            return PrimitiveTuples.pair(cc, negated);
        else
            return null;

    }

    /**
     * amount of time this spans for
     */
    default long dtRange() {
        long s = start();
        return s != ETERNAL ? end() - s : 0;
    }

    default boolean isFutureOf(long when) {
        long x = nearestStartOrEnd(when);
        return x == ETERNAL || x > when;
    }

    default boolean isPastOf(long when) {
        long x = nearestStartOrEnd(when);
        return x == ETERNAL || x < when;
    }

    default boolean isPresentOf(long when) {
        long x = nearestStartOrEnd(when);
        return x == ETERNAL || x == when;
    }

    default boolean isPresentOf(long when, int dur) {
        long x = nearestStartOrEnd(when);
        return x == ETERNAL || Math.abs(x - when) <= dur;
    }

}