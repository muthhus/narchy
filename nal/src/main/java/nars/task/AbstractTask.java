package nars.task;

import jcog.Util;
import jcog.data.array.LongArrays;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.budget.BudgetFunctions;
import nars.budget.RawBudget;
import nars.concept.Concept;
import nars.index.term.TermIndex;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.time.Tense;
import nars.truth.Truth;
import nars.truth.TruthDelta;
import nars.util.task.InvalidTaskException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import static nars.$.t;
import static nars.Op.*;
import static nars.term.Terms.compoundOrNull;
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
public abstract class AbstractTask extends RawBudget implements Task {

    @NotNull
    private Compound term;

    protected char punc;

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


    public AbstractTask(@NotNull Termed<Compound> term, char punctuation, @Nullable Truth truth, float p, float q) {
        super();
        priority = p; //direct set
        quality = q; //direct set

        this.punc = punctuation;

        //unwrap top-level negation
        Compound tt = term.term();
        if (tt.op() == Op.NEG) {
            Term nt = tt.term(0);
            if (nt instanceof Compound) {
                tt = (Compound) nt;

                if (isBeliefOrGoal())
                    truth = truth.negated();
            } else {
                throw new InvalidTaskException(this, "Top-level negation not wrapping a Compound");
            }
        }

        this.truth = truth;
        this.term = tt;
    }




    @Override
    public void normalize(@NotNull NAR n) throws Concept.InvalidConceptException, InvalidTaskException {

        if (isDeleted())
            throw new InvalidTaskException(this, "Deleted");

        Compound t = term;

//        if (!t.levelValid( n.level() ))
//            throw new InvalidTaskException(this, "Unsupported NAL level");

        char punc = punc();
        if (punc == 0)
            throw new InvalidTaskException(this, "Unspecified punctuation");



        Compound cntt;
//        if (t instanceof SerialCompound) {
//            Compound xt = compoundOrNullDeserialized(((SerialCompound) t), n.concepts);
//            if (xt == null)
//                throw new InvalidTaskException(t, "Error decoding SerialCompound");
//            cntt = xt;
//        } else {
//            cntt = t;
//        }

        cntt = eval(n.concepts, t);
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



    }

    @Nullable protected Compound eval(@NotNull TermIndex index, @NotNull Compound t) {
        //        if (t instanceof SerialCompound) {
//            t = ((SerialCompound)t).build(i);
//        }
        t = compoundOrNull(t);
        if (t==null) return null;

        return compoundOrNull(index.eval(t));
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
                    Long.hashCode(start),
                    Long.hashCode(end)
            );

            if (t!=null)
                h = Util.hashCombine(h, t.hashCode());
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
    public final long start() {
        return start;
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


            int to = Long.compare(start, o.start());
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

    /** TODO for external use in MutableTask instances only */
    public final void setStart(long o) {
//        if ((o == Integer.MIN_VALUE || o == Integer.MAX_VALUE) && Param.DEBUG) {
//            System.err.println("Likely an invalid occurrence time being set");
//        }
        if (o != start) {

            this.start = o;
            invalidate();
        }
    }

    /** TODO for external use in MutableTask instances only */
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
                    that instanceof Task &&
                    hashCode() == that.hashCode() &&
                    equivalentTo((Task) that, true, true, true, true, true));

    }

    @Override
    public final boolean equivalentTo(@NotNull Task that, boolean punctuation, boolean term, boolean truth, boolean stamp, boolean occurrenceTime) {

        if (stamp && (!Arrays.equals(this.evidence, that.evidence())))
            return false;

        if (evidence.length > 1) {
            if (occurrenceTime && (this.start != that.start()) || (this.end != that.end()))
                return false;

            if (truth && !Objects.equals(this.truth, that.truth()))
                return false;
        }

        if (term && !this.term.equals(that.term()))
            return false;

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
    public float confWeight(long when, float dur) {
        if (!isBeliefOrGoal())
            throw new UnsupportedOperationException();

        long a = start();

        Truth t = truth();
        float cw = t.evi();
        if (a == ETERNAL)
            return cw;
        else if (when == ETERNAL)// || when == now) && o == when) //optimization: if at the current time and when
            return t.eternalizedEvi();
        else {
            long z = end();
            //float dur = dur();

//            if (z - start < dur)
//                z = Math.round(start + dur); //HACK



            if ((when >= a) && (when <= z)) {

                //full confidence

            } else {
                //nearest endpoint of the interval
                if (dur > 0)
                    cw = TruthPolation.evidenceDecay(cw, dur, Math.min(Math.abs(a - when), Math.abs(z - when)));
                else
                    cw = 0;

                if (eternalizable()) {
                    float et = t.eternalizedEvi();
                    if (et > cw)
                        cw = et;
                }
            }

            return cw;

        }

    }

    public boolean eternalizable() {
        return term.vars() > 0;
        //return term.varIndep() > 0;
        //return false;


        //Op op = term.op();
        //return op ==IMPL || op ==EQUI || term.vars() > 0;
        //return op.statement || term.vars() > 0;
    }



    /** end occurrence */
    @Override public final long end() {

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




}
