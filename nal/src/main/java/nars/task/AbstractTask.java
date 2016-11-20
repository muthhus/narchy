package nars.task;

import nars.*;
import nars.budget.BudgetFunctions;
import nars.budget.RawBudget;
import nars.concept.util.InvalidConceptException;
import nars.task.util.InvalidTaskException;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.time.Tense;
import nars.truth.Truth;
import nars.truth.TruthDelta;
import nars.util.Util;
import nars.util.data.array.LongArrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import static nars.$.t;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;

/**
 * Default Task implementation
 * TODO move all mutable methods to MutableTask and call this ImmutableTask
 *
 * NOTE:
     if evidence length == 1 (input) then do not include
     truth or occurrence time as part of the hash, equality, and
     comparison tests.

     this allows an input task to modify itself in these two
     fields without changing its hash and equality consistency.

     once input, input tasks will have unique serial numbers anyway
 */
public abstract class AbstractTask extends RawBudget implements Task, Temporal {

    /** content term of this task */
    @NotNull
    private Compound term;

    protected char punc;

    @Nullable
    private Truth truth;

    @Nullable
    private long[] evidence = LongArrays.EMPTY_ARRAY;

    private long creation = Tense.TIMELESS;
    private long occurrence = ETERNAL;

    protected float dur = Float.NaN;

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





//    public AbstractTask(Compound term, char punctuation, Truth truth, Budget bv, Task parentTask, Task parentBelief, Task solution) {
//        this(term, punctuation, truth,
//                bv.getPriority(),
//                bv.getDurability(),
//                bv.getQuality(),
//                parentTask, parentBelief,
//                solution);
//    }

//    public AbstractTask(@NotNull Compound term, char punc, Truth truth, float p, float d, float q) {
//        this(term, punc, truth, p, d, q, (Task) null, null);
//    }

//    public AbstractTask(@NotNull Compound term, char punc, Truth truth, float p, float d, float q, Task parentTask, Task parentBelief) {
//        this(term, punc, truth,
//                p, d, q,
//                parentTask,
//                parentBelief
//        );
//    }

//    /** copy/clone constructor */
//    public AbstractTask(@NotNull Task task) {
//        this(task, task.punc(), task.truth(),
//                task.pri(), task.dur(), task.qua());
//        setEvidence(task.evidence());
//        setOccurrence(task.occurrence());
//    }


//    protected final void setTerm(@NotNull Termed<Compound> t) {
//        Termed existing = term;
//        term = t.term(); //use the provided instance even if equals
//        if (!existing.equals(t)) {
//            invalidate();
//        }
//    }


    public AbstractTask(@NotNull Termed<Compound> term, char punctuation, @Nullable Truth truth, float p, float d, float q) {
        super(p, q);

        this.punc = punctuation;

        //unwrap top-level negation
        if (term.op() == Op.NEG) {
            Term nt = term.term().term(0);
            if (nt instanceof Compound) {
                term = nt;

                if (isBeliefOrGoal())
                    truth = truth.negated();
            } else {
                throw new InvalidTaskException(this, "Top-level negation not wrapping a Compound");
            }
        }

        this.truth = truth;
        this.term = term.term();
    }




    @Override
    public void normalize(@NotNull NAR nar) throws InvalidConceptException, InvalidTaskException {

        if (isDeleted())
            throw new InvalidTaskException(this, "Deleted");

        Compound t = term;

        if (!t.levelValid( nar.level() ))
            throw new InvalidTaskException(this, "Unsupported NAL level");

        char punc = punc();
        if (punc == 0)
            throw new InvalidTaskException(this, "Unspecified punctuation");


        //this conflicts with weakref's
        /*if (!isCommand()) {
            ensureValidParentTaskRef();
        }*/

        //noinspection IfStatementWithTooManyBranches
        switch (punc()) {
            case Symbols.BELIEF:
            case Symbols.GOAL:
                if (truth == null) {
                    //apply the default truth value for specified punctuation
                    setTruth(nar.truthDefault(punc));
                } else {

                    float confLimit = 1f - Param.TRUTH_EPSILON;
                    if (!isInput() && conf() > confLimit) {
                        //clip maximum confidence in case a derivation of an axiomatic belief reaches conf=~1.0 also
                        setTruth(t(freq(), confLimit));
                    }
                }

                break;
            case Symbols.QUEST:
            case Symbols.QUESTION:
                if (truth!=null)
                    throw new RuntimeException("quests and questions must have null truth");
                break;
            case Symbols.COMMAND:
                break;

            default:
                throw new UnsupportedOperationException("invalid punctuation: " + punc);

        }

        Compound ntt = nar.normalize(t);
        if (ntt == null)
            throw new InvalidTaskException(t, "Failed normalization");

        if (!Task.taskContentValid(ntt, punc, nar, !Param.DEBUG))
            throw new InvalidTaskException(ntt, "Invalid content");

        if (ntt!=t) {
            this.term = ntt;
            invalidate();
        }

        // if a task has an unperceived creationTime,
        // set it to the memory's current time here,
        // and adjust occurenceTime if it's not eternal

        if (creation() <= Tense.TIMELESS) {
            long now = nar.time();
            long oc = occurrence();
            if (oc != ETERNAL)
                oc += now;

            this.creation = now;
            setOccurrence(oc);
        }


        float q = qua();
        if (q!=q /* fast NaN test */) {

            if (isBeliefOrGoal()) {
                setQuality(BudgetFunctions.truthToQuality(truth()));
            } else {
                setQuality(punc == Symbols.QUESTION ? nar.DEFAULT_QUESTION_QUALITY : nar.DEFAULT_QUEST_QUALITY);
            }
        }

        //finally, assign a unique stamp if none specified (input)
        if (evidence.length == 0)
            setEvidence(nar.time.nextStamp());

        if (dur!=dur) {
            //assign default duration from NAR
            dur = nar.time.dur();
        }

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



    }

    //    /** if validated and entered into the system. can be overridden in subclasses to handle this event
//     *  isnt called for Command tasks currently; they will be executed right away anyway
//     * */
//    protected void onInput(@NotNull Memory m) {
//
//    }



    /** includes: evidentialset, occurrencetime, truth, term, punctuation */
    private final int rehash() {

        @Nullable long[] e = this.evidence;

        int h = Util.hashCombine(
                term.hashCode(),
                punc,
                Arrays.hashCode(e)
        );


        if (e.length > 1) {

            Truth t = truth();

            h = Util.hashCombine(
                    h,
                    Long.hashCode(occurrence),
                    t!=null ? t.hashCode() : 1
            );

        }


        if (h == 0) h = 1; //reserve 0 for non-hashed

        return h;
    }

    @NotNull @Override
    public final Compound term() {
        return term;
    }


    @Override
    public void feedback(TruthDelta delta, float deltaConfidence, float deltaSatisfaction, NAR nar) {

    }

    @Nullable @Override
    public final Truth truth() {
        return truth;
    }

    @Override
    public final void setTruth(@Nullable Truth t) {

        if (t == null && isBeliefOrGoal())
            throw new InvalidTaskException(this, "null truth for belief or goal");

        if (!Objects.equals(truth, t)) {
            truth = t;
            invalidate();
        }
    }

    /**
     * Recognize a Question
     * @return Whether the object is a Question
     */
    @Override public final boolean isQuestion() {
        return (punc == Symbols.QUESTION);
    }

    /**
     * Recognize a Belief (aka Judgment)
     * @return Whether the object is a Judgment
     */
    @Override public final boolean isBelief() {
        return (punc == Symbols.BELIEF);
    }

    @Override public final boolean isGoal() {
        return (punc == Symbols.GOAL);
    }

    @Override public final boolean isQuest() {
        return (punc == Symbols.QUEST);
    }


    @Override
    public final boolean isAnticipated() {
        return isBelief() && !isEternal() &&
                (/*state() == TaskState.Anticipated ||*/ isInput());
    }

    /** the evidence should be sorted and de-duplicaed prior to calling this */
    @NotNull protected Task setEvidence(@Nullable long... evidentialSet) {

        if (this.evidence !=evidentialSet) {
            this.evidence = evidentialSet;
            invalidate();
        }
        return this;
    }



    @Override
    public final char punc() {
        return punc;
    }

    @NotNull
    @Override
    public final long[] evidence() {
        return this.evidence;
    }

    @Override
    public final long creation() {
        return creation;
    }

    @Override
    public final long occurrence() {
        return occurrence;
    }


    @Override
    public int compareTo(@NotNull Task obj) {

        if (this == obj)
            return 0;

        Task o = (Task)obj;

        int c = Util.compare(evidence, o.evidence());
        if (c != 0)
            return c;

        if (evidence.length > 1) {
            Truth tr = this.truth;

            if (tr != null) {
                @Nullable Truth otruth = o.truth();
                if (otruth == null)
                    return 1;
                int tu = Truth.compare(tr, otruth);
                if (tu != 0) return tu;
            }


            int to = Long.compare(occurrence, o.occurrence());
            if (to != 0) return to;
        }


        int tc = term.compareTo(o.term());
        if (tc != 0) return tc;

        return Character.compare(punc(), o.punc())
                ;

    }

    @NotNull
    @Override
    public final Task setCreationTime(long creationTime) {
        if ((this.creation <= Tense.TIMELESS) && (occurrence > Tense.TIMELESS)) {
            //use the occurrence time as the delta, now that this has a "finite" creationTime
            setOccurrence(occurrence + creationTime);
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

    /** TODO for external use in MutableTask instances only */
    public final void setOccurrence(long o) {
        if ((o == Integer.MIN_VALUE || o == Integer.MAX_VALUE) && Param.DEBUG) {
            System.err.println("Likely an invalid occurrence time being set");
        }
        if (o != occurrence) {
            this.occurrence = o;
            invalidate();
        }
    }

    @Override
    public final int hashCode() {
        int h = this.hash;
        if (h == 0) {
            return this.hash = rehash();
        }
        return h;
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

        return this == that ||
                (that!=null  &&
                    hashCode() == that.hashCode() &&
                    that instanceof Task &&
                    equivalentTo((Task) that, true, true, true, true, true));

    }

    @Override
    public final boolean equivalentTo(@NotNull Task that, boolean punctuation, boolean term, boolean truth, boolean stamp, boolean occurrenceTime) {

        if (stamp && (!Arrays.equals(this.evidence, that.evidence())))
            return false;

        if (term && !this.term.equals(that.term()))
            return false;

        if (evidence.length > 1) {
            if (occurrenceTime && (this.occurrence != that.occurrence()))
                return false;

            if (truth && !Objects.equals(this.truth, that.truth()))
                return false;
        }

        if (punctuation && (this.punc != that.punc()))
            return false;

        return true;
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





    @NotNull
    @Override
    @Deprecated
    public String toString() {
        try {
            return appendTo(null, null).toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public float confWeight(long when) {
        if (!isBeliefOrGoal())
            throw new UnsupportedOperationException();

        long a = start();

        Truth t = truth();
        float cw = t.confWeight();
        if (a == ETERNAL)
            return cw;
        else if (when == ETERNAL)// || when == now) && o == when) //optimization: if at the current time and when
            return t.eternalizedConf();
        else {
            long z = end();

            if (z < a) { long x = a; a = z; z = x; } //order a..z
            if ((when >= a) && (when <= z)) {
                return cw;
            } else {
                long nearest; //nearest endpoint of the interval
                if (when <= a) nearest = a;
                else /*if (when > z)*/ nearest = z;
                long delta = Math.abs(nearest - when);

                float dur = dur();
                if (dur!=dur)
                    throw new RuntimeException("NaN duration");

                float dc = TruthPolation.evidenceDecay(cw, dur, delta);
                if (eternalizable())
                    return Math.max(dc, t.eternalizedConf());
                return dc;

            }

        }

    }

    public boolean eternalizable() {
        return term.vars() > 0;
        //return term.varIndep() > 0;
        //Op op = term.op();
        //return op ==IMPL || op ==EQUI || term.vars() > 0;
        //return op.statement || term.vars() > 0;
    }

    @Override
    public long start() {
        return occurrence();
    }

    /** end occurrence */
    @Override public long end() {

        //return occurrence();

        long dt = 0;
        if (op().temporal) {
            dt=dt();
            if (dt==DTERNAL)
                dt = 0;
        }
        return occurrence()+dt;
    }

    @Override
    public float dur() {
        return dur;
    }
}
