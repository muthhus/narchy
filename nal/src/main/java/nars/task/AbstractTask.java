package nars.task;

import nars.*;
import nars.budget.UnitBudget;
import nars.concept.Concept;
import nars.nal.Tense;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.util.data.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Default Task implementation
 * TODO move all mutable methods to MutableTask and call this ImmutableTask
 */
public abstract class AbstractTask extends UnitBudget implements Task, Temporal {

    /** content term of this task */
    private Termed<Compound> term;

    protected char punctuation;

    @Nullable
    private Truth truth;

    @Nullable
    private long[] evidence;

    private long creationTime = Tense.TIMELESS;
    private long occurrenceTime = Tense.ETERNAL;

    /** Array of tasks from which the Task is derived, or null if input
     *
     * These are not guaranteed to remain because it is
     * stored as a Soft or Weak reference so that
     * task ancestry does not grow uncontrollably;
     *
     * instead, we rely on the JVM garbage collector
     * to serve as an enforcer of AIKR
     *
     * @return The task from which the task is derived, or
     * null if it has been forgotten
     */
    @Nullable protected transient Reference<Task>[] parents;

//    /** Belief from which the Task is derived, or null if derived from a theorem     */
//    @Nullable protected transient Reference<Task> parentBelief;

    private transient int hash;



    @Nullable
    private List log;


//    public AbstractTask(Compound term, char punctuation, Truth truth, Budget bv, Task parentTask, Task parentBelief, Task solution) {
//        this(term, punctuation, truth,
//                bv.getPriority(),
//                bv.getDurability(),
//                bv.getQuality(),
//                parentTask, parentBelief,
//                solution);
//    }

    public AbstractTask(@NotNull Compound term, char punc, Truth truth, float p, float d, float q) {
        this(term, punc, truth, p, d, q, (Task) null, null);
    }

//    public AbstractTask(@NotNull Compound term, char punc, Truth truth, float p, float d, float q, Task parentTask, Task parentBelief) {
//        this(term, punc, truth,
//                p, d, q,
//                parentTask,
//                parentBelief
//        );
//    }

    /** copy/clone constructor */
    public AbstractTask(@NotNull Task task) {
        this(task, task.punc(), task.truth(),
                task.pri(), task.dur(), task.qua(),
                task.getParentsRef());
        setEvidence(task.evidence());
        setOccurrenceTime(task.occurrence());
    }

    void setTime(long creation, long occurrence) {
        this.creationTime = creation;
        setOccurrenceTime(occurrence);
    }


    protected final void setTerm(@NotNull Termed<Compound> t) {
        Termed existing = term;
        term = t; //use the provided instance even if equals
        if (!existing.equals(t)) {
            invalidate();
        }
    }

    public AbstractTask(@NotNull Termed<Compound> term, char punctuation, @Nullable Truth truth, float p, float d, float q,
                        @Nullable Task... parentTasks) {
        this(term, punctuation, truth, p, d, q, Global.reference(parentTasks));
    }

    public AbstractTask(@NotNull Termed<Compound> term, char punctuation, @Nullable Truth truth, float p, float d, float q,
                        @Nullable Reference<Task>[] parents) {
        super(p, d, q);

        this.punctuation = punctuation;

        //unwrap top-level negation
        if (term.op() == Op.NEGATE) {
            Term nt = term.term().term(0);
            if (nt instanceof Compound) {
                term = nt;

                if (isBeliefOrGoal())
                    truth = truth.negated();
            } else {
                throw new NAR.InvalidTaskException(this, "Top-level negation not wrapping a Compound");
            }
        }

        this.truth = truth;
        this.term = term;
        this.parents = parents;
        updateEvidence();
    }



    @Override @NotNull
    public final Task normalize(@NotNull Memory memory) {

        if (isDeleted())
            throw new NAR.InvalidTaskException(this, "Deleted");

        Compound t = term();

        if (!t.levelValid( memory.nal() ))
            throw new NAR.InvalidTaskException(this, "Unsupported NAL level");

        char punc = punc();
        if (punc == 0)
            throw new NAR.InvalidTaskException(this, "Unspecified punctuation");

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
                    truth = memory.truthDefault(punc);
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

        Termed<Compound> ntt = Task.normalizeTaskTerm(t, punc, memory, isInput());
        setTerm(ntt);

        // if a task has an unperceived creationTime,
        // set it to the memory's current time here,
        // and adjust occurenceTime if it's not eternal

        if (creation() <= Tense.TIMELESS) {
            long now = memory.time();
            long oc = occurrence();
            if (oc != Tense.ETERNAL)
                oc += now;

            setTime(now, oc);
        }

////        //shift the occurrence time if dt < 0
//        int termDur = term.term().dt();
//        if (termDur!=DTERNAL && termDur < 0) {
//            setOccurrenceTime(occurrence() - termDur);
//        }




        /** NaN quality is a signal that a budget's values need initialized */
        float q = qua();
        if (q!=q /* fast NaN test */) {
            //HACK for now just assume that only MutableTask supports unbudgeted input
            memory.budgetDefault((MutableTask)this);
        }



        //finally, assign a unique stamp if none specified (input)
        if (evidence == null) {

            //assert(isInput()); //throw new RuntimeException("derived task without evidence: " + this);
            if (!isInput())
                throw new RuntimeException("derived task without evidence: " + this);


            setEvidence(memory.newStampSerial());

            //this actually means it arrived from unknown origin.
            //we'll clarify what null evidence means later.
            //if data arrives via a hardware device, can a virtual
            //task be used as the parent when it generates it?
            //doesnt everything originate from something else?
            if (Global.DEBUG && (log == null))
                log("Input");

        }

        onInput(memory);

        return this;
    }


    /** if validated and entered into the system. can be overridden in subclasses to handle this event
     *  isnt called for Command tasks currently; they will be executed right away anyway
     * */
    protected void onInput(@NotNull Memory m) {

    }

    /**
     *
     * when executed; can be overridden in subclasses to handle this event;
     * (for Operation and negation of Operation ONLY)
     * */
    @Override public void execute(@NotNull Concept c, @NotNull NAR n) {

    }

    @Override
    public @Nullable Reference<Task>[] getParentsRef() {
        return parents;
    }

    /** includes: evidentialset, occurrencetime, truth, term, punctuation */
    private final int rehash() {

        int h = Util.hashCombine( Util.hashCombine(
                term().hashCode(),
                punc(),
                Arrays.hashCode(evidence())
            ),
            Long.hashCode( occurrence())
        );

        Truth t = truth();

        h = (t != null) ?
            Util.hashCombine(h, t.hashCode() ) :
            h;

//        int h = Objects.hash(
//                Arrays.hashCode(evidence()),
//                occurrence(),
//                punc(),
//                term(),
//                truth()
//        );

        if (h == 0) return 1; //reserve 0 for non-hashed

        return h;
    }

    @Override
    public boolean onConcept(@NotNull Concept c) {
        return true;
    }

    @NotNull @Override
    public final Compound term() {
        return term.term();
    }

    @Nullable @Override
    public final Truth truth() {
        return truth;
    }

    @Override
    public final void setTruth(@Nullable Truth t) {

        if (t == null && isBeliefOrGoal())
            throw new NullPointerException();

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
        return (punctuation == Symbols.QUESTION);
    }

    /**
     * Recognize a Belief (aka Judgment)
     * @return Whether the object is a Judgment
     */
    @Override public final boolean isBelief() {
        return (punctuation  == Symbols.BELIEF);
    }

    @Override public final boolean isGoal() {
        return (punctuation  == Symbols.GOAL);
    }

    @Override public final boolean isQuest() {
        return (punctuation  == Symbols.QUEST);
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
    public final boolean isDouble() {
        return getParentBelief() != null;
    }

    @Override
    public final char punc() {
        return punctuation;
    }

    @Nullable
    @Override
    public final long[] evidence() {
        long[] e = this.evidence;
        if (e == null) {
            updateEvidence();
            e = this.evidence;
//            if (e == null)
//                throw new NullPointerException();
        }
        return e;
    }

    @Override
    public final long creation() {
        return creationTime;
    }

    @Override
    public final long occurrence() {
        return occurrenceTime;
    }


    @Override
    public int compareTo(@NotNull Object obj) {
        if (this == obj) return 0;

        Task o = (Task)obj;
        int tt = term().compareTo(o.term());
        if (tt != 0) return tt;

        int tc = Character.compare(punc(), o.punc());
        if (tc != 0) return tc;

        Truth tr = this.truth();
        if (tr !=null) {
            @Nullable Truth otruth = o.truth();
            if (otruth == null)
                return 1;
            int tu = Truth.compare(tr, otruth);
            if (tu!=0) return tu;
        }

        int to = Long.compare( occurrence(), o.occurrence() );
        if (to!=0) return to;

        return Util.compare(evidence(), o.evidence());
    }

    @NotNull
    @Override
    public final Task setCreationTime(long creationTime) {
        if ((this.creationTime <= Tense.TIMELESS) && (occurrenceTime > Tense.TIMELESS)) {
            //use the occurrence time as the delta, now that this has a "finite" creationTime
            setOccurrenceTime(occurrenceTime + creationTime);
        }
        //if (this.creationTime != creationTime) {
        this.creationTime = creationTime;
            //does not need invalidated since creation time is not part of hash
        //}
        return this;
    }


    final void updateEvidence() {
        //supplying no evidence will be assigned a new serial
        //but this should only happen for input tasks (with no parent)

        Task pt = getParentTask();
        if (pt !=null) {
            Task pb = getParentBelief();
            if (pb!=null)
                setEvidence( Stamp.zip(pt, pb ));
            else
                setEvidence(pt.evidence());
        } else {
            setEvidence((long[])null);
        }

    }

    @Override
    public void delete() {
        if (!isDeleted()) {
            Reference<Task>[] p = this.parents;
            if (p !=null) {
                Global.unreference(p);
                //NO this.parents = null; //dont remove the array, it is there to indicate it is not an input task
            }
            if (!Global.DEBUG)
                this.log = null; //.clear();
        }
        super.delete();
    }

    protected final void invalidate() {
        hash = 0;
    }

    /** TODO for external use in MutableTask instances only */
    @Override public final void setOccurrenceTime(long o) {
        if (o != occurrenceTime) {
            this.occurrenceTime = o;
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
    public final boolean equals(@NotNull Object that) {

        return this == that ||
                (hashCode() == that.hashCode() && equivalentTo((Task) that, true, true, true, true, false));

    }

    @Override
    public final boolean equivalentTo(@NotNull Task that, boolean punctuation, boolean term, boolean truth, boolean stamp, boolean creationTime) {

        if (stamp) {
            //uniqueness includes every aspect of stamp except creation time
            //<patham9> if they are only different in creation time, then they are the same
            if (!equalStamp(that, true, creationTime, true))
                return false;
        }

        if (truth) {
            Truth thisTruth = truth();
            if (thisTruth == null) {
                //equal punctuation will ensure thatTruth is also null
            } else {
                if (!thisTruth.equals(that.truth())) return false;
            }
        }


        if (term) {
            if (!term().equals(that.term())) return false;
        }

        if (punctuation) {
            char thisPunc = punc();
            if (thisPunc != that.punc()) return false;
        }

        return true;
    }

    /**
     * Check if two stamps contains the same types of content
     * <p>
     * NOTE: hashcode will include within it the creationTime & occurrenceTime, so if those are not to be compared then avoid comparing hash
     *
     * @param s The Stamp to be compared
     * @return Whether the two have contain the same evidential base
     */
    public final boolean equalStamp(@NotNull Task s, boolean evidentialSet, boolean creationTime, boolean occurrenceTime) {
        if (this == s) return true;

        /*if (hash && (!occurrenceTime || !evidentialSet))
            throw new RuntimeException("Hash equality test must be followed by occurenceTime and evidentialSet equality since hash incorporates them");

        if (hash)
            if (hashCode() != s.hashCode()) return false;*/
        if (creationTime)
            if (creation() != s.creation()) return false;
        if (occurrenceTime)
            if (occurrence() != s.occurrence()) return false;
        if (evidentialSet) {
            return Arrays.equals(evidence(), s.evidence());
        }


        return true;
    }


    @NotNull
    @Override
    public Task log(@Nullable List historyToCopy) {
        if (!Global.DEBUG_TASK_LOG)
            return this;

        if ((historyToCopy != null) && (!historyToCopy.isEmpty())) {
            getOrCreateLog().addAll(historyToCopy);
        }
        return this;
    }

    /**
     * append an entry to this task's log history
     * useful for debugging but can also be applied to meta-analysis
     * ex: an entry might be a String describing a change in the story/history
     * of the Task and the reason for it.
     */
    @NotNull
    @Override
    public final Task log(Object entry) {
        if (!Global.DEBUG_TASK_LOG)
            return this;

        getOrCreateLog().add(entry);
        return this;
    }

    @Nullable
    @Override
    public final List log() {
        return (log);
    }


    @NotNull
    final List getOrCreateLog() {
        List exist = log();
        if (exist == null) {
            this.log = (exist = Global.newArrayList(1));
        }
        return exist;
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
        return appendTo(null, null).toString();
    }



}
