package nars.task;

import jcog.Util;
import jcog.data.array.LongArrays;
import nars.*;
import nars.budget.Budget;
import nars.budget.BudgetFunctions;
import nars.budget.Budgeted;
import nars.budget.RawBudget;
import nars.concept.Concept;
import nars.index.term.TermIndex;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.time.Tense;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import nars.truth.Truthed;
import nars.util.task.InvalidTaskException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import static nars.$.t;
import static nars.Op.*;
import static nars.term.Terms.compoundOrNull;
import static nars.time.Tense.ETERNAL;

/**
 * Default Task implementation
 * TODO move all mutable methods to TaskBuilder and call this ImTaskBuilder
 *
 * NOTE:
     if evidence length == 1 (input) then do not include
     truth or occurrence time as part of the hash, equality, and
     comparison tests.

     this allows an input task to modify itself in these two
     fields without changing its hash and equality consistency.

     once input, input tasks will have unique serial numbers anyway
 */
public class TaskBuilder extends RawBudget implements Termed, Truthed, Function<NAR,Task> {

    @NotNull
    private Compound term;

    protected byte punc;

    @Nullable
    private Truth truth;

    @Nullable
    private long[] evidence = LongArrays.EMPTY_ARRAY;

    private long creation = Tense.TIMELESS;
    private long start = ETERNAL, end = ETERNAL;

//    /** Array of tasks from which the Task is derived, or null if input
//     *
//     * These are not guaranteed to remain because it is
//     * stored as a Soft or Weak reference so that
//     * task ancestry does not grow uncontrollably;
//     *
//     * instead, we rely on the JVM garbage collector
//     * to serve as an enforcer of AIKR
//     *
//     * @return The task from which the task is derived, or
//     * null if it has been forgotten
//     */
//    @Nullable protected transient Reference<Task>[] parents;

//    /** Belief from which the Task is derived, or null if derived from a theorem     */
//    @Nullable protected transient Reference<Task> parentBelief;

    private transient int hash;
    @Nullable
    private List log;


    public TaskBuilder(@NotNull Compound t, byte punct, float freq, @NotNull NAR nar) {
        this(t, punct, $.t(freq, nar.confidenceDefault(punct)));
    }

    //    public MutableTask(@NotNull String compoundTermString, byte punct, float freq, float conf) throws Narsese.NarseseException {
//        this($.$(compoundTermString), punct, t(freq, conf));
//    }
    public TaskBuilder(@NotNull Compound t, byte punct, float freq, float conf) {
        this(t, punct, t(freq, conf));
    }

    public TaskBuilder(@NotNull String compoundTermString, byte punct, @Nullable Truth truth) throws Narsese.NarseseException {
        this($.$(compoundTermString), punct, truth);
    }


    public TaskBuilder(@NotNull Compound term, byte punct, @Nullable Truth truth) {
        this(term, punct, truth,
            /* budget: */ 0, Float.NaN);
    }

    public TaskBuilder(@NotNull Compound term, byte punctuation /* TODO byte */, @Nullable Truth truth, float p, float q) {
        super();
        priority = p; //direct set
        quality = q; //direct set

        this.punc = (byte) punctuation;

        //unwrap top-level negation
        Compound tt = term.term();
        if (tt.op() == Op.NEG) {
            Term nt = tt.term(0);
            if (nt instanceof Compound) {
                tt = (Compound) nt;

                if (punctuation == Op.BELIEF || punctuation == Op.GOAL)
                    truth = truth.negated();
            } else {
                throw new InvalidTaskException(this, "Top-level negation not wrapping a Compound");
            }
        }

        this.truth = truth;
        this.term = tt;
    }




    public boolean isInput() {
        return evidence().length <= 1;
    }

    public Task apply(@NotNull NAR n) throws Concept.InvalidConceptException, InvalidTaskException {

        if (isDeleted())
            throw new InvalidTaskException(this, "Deleted");

        Compound t = term;

        if (!t.levelValid( n.level() ))
            throw new InvalidTaskException(this, "Unsupported NAL level");

        byte punc = punc();
        if (punc == 0)
            throw new InvalidTaskException(this, "Unspecified punctuation");


        Compound cntt = eval(n.concepts, t);
        if (cntt == null)
            throw new InvalidTaskException(t, "Failed normalization");

        if (!Task.taskContentValid(cntt, punc, n.level(), n.termVolumeMax.intValue(), !Param.DEBUG))
            throw new InvalidTaskException(cntt, "Invalid content");

        if (cntt!=t) {
            this.term = cntt;
            invalidate();
        }

        //noinspection IfStatementWithTooManyBranches
        switch (punc()) {
            case BELIEF:
            case GOAL:
                if (truth == null) {
                    //apply the default truth value for specified punctuation
                    setTruth(n.truthDefault(punc));
                } else {

                    float confLimit = 1f - Param.TRUTH_EPSILON;
                    if (!isInput() && conf() > confLimit) {
                        //clip maximum confidence in case a derivation of an axiomatic belief reaches conf=~1.0 also
                        setTruth(t(freq(), confLimit));
                    }
                }

                break;
            case QUEST:
            case QUESTION:
                if (truth!=null)
                    throw new RuntimeException("quests and questions must have null truth");
                break;
            case COMMAND:
                break;

            default:
                throw new UnsupportedOperationException("invalid punctuation: " + punc);

        }


        // assign a unique stamp if none specified (input)
        if (evidence.length == 0)
            setEvidence(n.time.nextStamp());


        // if a task has an unperceived creationTime,
        // set it to the memory's current time here,
        // and adjust occurenceTime if it's not eternal

        if (creation() <= Tense.TIMELESS) {
            long now = n.time();
            long oc = start();
            if (oc != ETERNAL)
                oc += now;

            this.creation = now;
            setStart(oc);
            setEnd(oc);
        }


        //if quality is not specified (NaN), then this means to assign the default budgeting according to the task's punctuation
        float q = qua();
        if (q!=q) {

            setPriority(n.priorityDefault(punc));

            if (isBeliefOrGoal()) {
                setQua(BudgetFunctions.truthToQuality(truth()));
            } else if (!isCommand()) {
                setQua(n.qualityDefault(punc));
            }
        }


//        if (dur!=dur) {
//            //assign default duration from NAR
//            dur = n.time.dur();
//        }

            //shift the occurrence time if input and dt < 0 and non-eternal HACK dont use log it may be removed without warning
//        if (isInput()) {
//            long exOcc = occurrence();
//            if (exOcc != ETERNAL) {
//                int termDur = ntt.dt();
//                if (termDur != DTERNAL && termDur < 0) {
//                    setOccurrence(exOcc - termDur);
//                }
//            }
//        }


        return new ImmutableTask(term, punc, truth, creation, start, end, evidence);

    }

    @Nullable protected Compound eval(@NotNull TermIndex index, @NotNull Compound t) {
        return compoundOrNull(index.eval(t));
    }


    //    /** if validated and entered into the system. can be overridden in subclasses to handle this event
//     *  isnt called for Command tasks currently; they will be executed right away anyway
//     * */
//    protected void onInput(@NotNull Memory m) {
//
//    }



    @NotNull @Override
    public final Compound term() {
        return term;
    }



    public boolean isBeliefOrGoal() {
        return punc==Op.BELIEF || punc==Op.GOAL;
    }
    public boolean isCommand() {
        return punc==Op.COMMAND;
    }

    @Nullable @Override
    public final Truth truth() {
        return truth;
    }

    protected final void setTruth(@Nullable Truth t) {

        if (t == null && isBeliefOrGoal())
            throw new InvalidTaskException(this, "null truth for belief or goal");

        if (!Objects.equals(truth, t)) {
            truth = t;
            invalidate();
        }
    }




//    @Override
//    public final boolean isAnticipated() {
//        return isBelief() && !isEternal() &&
//                (/*state() == TaskState.Anticipated ||*/ isInput());
//    }

    /** the evidence should be sorted and de-duplicaed prior to calling this */
    @NotNull protected TaskBuilder setEvidence(@Nullable long... evidentialSet) {

        if (this.evidence !=evidentialSet) {
            this.evidence = evidentialSet;
            invalidate();
        }
        return this;
    }

    public final byte punc() {
        return punc;
    }

    @NotNull
    public final long[] evidence() {
        return this.evidence;
    }

    public final long creation() {
        return creation;
    }

    public final long start() {
        return start;
    }


//    @Override
//    public int compareTo(@NotNull Task obj) {
//
//        if (this == obj)
//            return 0;
//
//        Task o = (Task)obj;
//
//        int c = Util.compare(evidence, o.evidence());
//        if (c != 0)
//            return c;
//
//        if (evidence.length > 1) {
//            Truth tr = this.truth;
//
//            if (tr != null) {
//                @Nullable Truth otruth = o.truth();
//                if (otruth == null)
//                    return 1;
//                int tu = Truth.compare(tr, otruth);
//                if (tu != 0) return tu;
//            }
//
//
//            int to = Long.compare(start, o.start());
//            if (to != 0) return to;
//        }
//
//
//        int tc = term.compareTo(o.term());
//        if (tc != 0) return tc;
//
//        return Character.compare(punc(), o.punc())
//                ;
//    }

    @NotNull
    public final TaskBuilder setCreationTime(long creationTime) {
        if ((this.creation <= Tense.TIMELESS) && (start > Tense.TIMELESS)) {
            //use the occurrence time as the delta, now that this has a "finite" creationTime
            long when = start + creationTime;
            setStart(when);
            setEnd(when);
        }
        //if (this.creationTime != creationTime) {
        this.creation = creationTime;
            //does not need invalidated since creation time is not part of hash
        //}
        return this;
    }

    protected final void invalidate() {
        hash = 0;
    }

    /** TODO for external use in TaskBuilder instances only */
    public final void setStart(long o) {
//        if ((o == Integer.MIN_VALUE || o == Integer.MAX_VALUE) && Param.DEBUG) {
//            System.err.println("Likely an invalid occurrence time being set");
//        }
        if (o != start) {

            this.start = o;
            invalidate();
        }
    }

    /** TODO for external use in TaskBuilder instances only */
    public final void setEnd(long o) {
//        if ((o == Integer.MIN_VALUE || o == Integer.MAX_VALUE) && Param.DEBUG) {
//            System.err.println("Likely an invalid occurrence time being set");
//        }
        if (o != end) {
            if (start == ETERNAL && o!=ETERNAL)
                throw new RuntimeException("can not set end time for eternal task");
            if (o < start)
                throw new RuntimeException("end must be equal to or greater than start");

            this.end = o;
            invalidate();
        }
    }



    @Override
    public final int hashCode() {
        throw new UnsupportedOperationException();
//        int h = this.hash;
//        if (h == 0) {
//            return this.hash = rehash();
//        }
//        return h;
    }

    /**
     * To check whether two sentences are equal
     * Must be consistent with the values calculated in getHash()
     *
     * @param that The other sentence
     * @return Whether the two sentences have the same content
     */
    @Override
    public final boolean equals(@Nullable Object that) {
        throw new UnsupportedOperationException();
    }






    /*
    @Override
    public void delete() {
        super.delete();
//        this.parentBelief = this.parentTask = this.bestSolution = null;
//        this.cause = null;
//        log.clear();
//        this.term = null;
//        this.truth = null;
//        this.hash = 0;
    }*/








    /** end occurrence */
    public final long end() {

        return end;

//        //return occurrence();
//        long p = start();
//        if (p == ETERNAL)
//            return ETERNAL;
//
//        long dt = 0;
//        if (op().temporal) {
//            dt=dt();
//            if (dt==DTERNAL)
//                dt = 0;
//        }
//        return p + dt;
    }


    @NotNull
    public final TaskBuilder present(@NotNull NAR nar) {
        return time(nar.time());
    }

    @NotNull
    public final TaskBuilder time(@NotNull NAR nar, int dt) {
        return time(nar.time() + dt);
    }

    @NotNull public final TaskBuilder time(long when) {
        return TaskBuilder.this.time(when, when);
    }

    @NotNull
    public TaskBuilder time(long creationTime, long start, long end) {
        setCreationTime(creationTime);
        setStart(start);
        setEnd(end);
        return this;
    }

    @NotNull
    public TaskBuilder time(long creationTime, long occurrenceTime) {
        setCreationTime(creationTime);
        setStart(occurrenceTime);
        setEnd(occurrenceTime);
        return this;
    }

    @NotNull
    public final TaskBuilder occurr(long occurrenceTime) {
        setStart(occurrenceTime);
        setEnd(occurrenceTime);
        return this;
    }

    @NotNull
    public TaskBuilder eternal() {
        setStart(ETERNAL);
        setEnd(ETERNAL);
        return this;
    }

    @NotNull
    public final TaskBuilder evidence(long... evi) {
        setEvidence(evi);
        return this;
    }

    public final TaskBuilder evidence(@NotNull Task evidenceToCopy) {
        return evidence(evidenceToCopy.evidence());
    }



    @NotNull
    public final TaskBuilder budget(@NotNull Budget bb) {
        setBudget(bb);
        return this;
    }


    public TaskBuilder log(String s) {
        if (log == null)
            log = $.newArrayList(1);
        log.add(s);
        return this;
    }
}
