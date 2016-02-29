package nars.task;

import nars.Global;
import nars.Memory;
import nars.NAR;
import nars.budget.UnitBudget;
import nars.concept.Concept;
import nars.nal.Tense;
import nars.term.Operator;
import nars.term.Compound;
import nars.term.Termed;
import nars.truth.DefaultTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.util.data.Util;
import nars.util.event.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static nars.Global.dereference;
import static nars.Global.reference;

/**
 * Default Task implementation
 * TODO move all mutable methods to MutableTask and call this ImmutableTask
 */
public abstract class AbstractTask extends UnitBudget
        implements Task, Temporal {

    /** content term of this task */
    private Termed<Compound> term;

    @Nullable
    protected TaskState state;

    protected char punctuation;

    private Truth truth;

    @Nullable
    private long[] evidentialSet;

    private long creationTime = Tense.TIMELESS;
    private long occurrenceTime = Tense.ETERNAL;

    /**
     * Task from which the Task is derived, or null if input
     */
    @Nullable
    protected transient Reference<Task> parentTask; //should this be transient? we may want a Special kind of Reference that includes at least the parent's Term
    /**
     * Belief from which the Task is derived, or null if derived from a theorem
     */
    @Nullable
    protected transient Reference<Task> parentBelief;

    private transient int hash;



    @Nullable
    private Reference<List> log;


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

    public AbstractTask(@NotNull Compound term, char punc, Truth truth, float p, float d, float q, Task parentTask, Task parentBelief) {
        this(term, punc, truth,
                p, d, q,
                Global.reference(parentTask),
                reference(parentBelief)
        );
    }

    /** copy/clone constructor */
    public AbstractTask(@NotNull Task task) {
        this(task, task.punc(), task.truth(),
                task.pri(), task.dur(), task.qua(),
                task.getParentTaskRef(), task.getParentBeliefRef());
        setEvidence(task.evidence());
        setOccurrenceTime(task.occurrence());
    }

    @NotNull
    @Override
    public Task task() {
        return this;
    }

    void setTime(long creation, long occurrence) {
        this.creationTime = creation;

        boolean changed = this.occurrenceTime!=occurrence;
        if (changed) {
            this.occurrenceTime = occurrence;
            invalidate();
        }

        /*setCreationTime(creation);
        setOccurrenceTime(occurrence);*/
    }


    protected final void setTerm(@NotNull Termed<Compound> t) {
        if (t == null)
            throw new RuntimeException("null term");

        if (term!=t) {
            term = t;
            invalidate();
        }
    }


    public AbstractTask(@NotNull Termed<Compound> term, char punctuation, @Nullable Truth truth, float p, float d, float q,
                        @Nullable Reference<Task> parentTask, @Nullable Reference<Task> parentBelief) {
        super(p, d, q);
        this.truth = truth;
        this.punctuation = punctuation;
        this.term = term;
        this.parentTask = parentTask;
        this.parentBelief = parentBelief;
        updateEvidence();
    }

    @Override
    public final Task normalize(@NotNull Memory memory) {

//        if (hash != 0) {
//            /* already validated */
//            return this;
//        }

        if (isDeleted())
            return null;

        Compound t = term();
        if (!t.levelValid( memory.nal() ))
            return null;


        char punc = punc();
        if (punc == 0)
            throw new RuntimeException("Punctuation must be specified before generating a default budget");

        //this conflicts with weakref's
        /*if (!isCommand()) {
            ensureValidParentTaskRef();
        }*/

        //noinspection IfStatementWithTooManyBranches
        if (isJudgmentOrGoal()) {

        } else if (isQuestOrQuestion()) {
            if (truth!=null)
                throw new RuntimeException("quests and questions must have null truth");
        } else if (isCommand()) {
            //..
        } else {
            throw new RuntimeException("invalid punctuation: " + punc);
        }

        //normalize term
        Termed<Compound> normalizedTerm = memory.index.normalized(t);
        if ((normalizedTerm == null) || (!Task.validTaskTerm(normalizedTerm.term()))) {
            return null;
        }
        setTerm(normalizedTerm);


        if (truth == null && isJudgmentOrGoal()) {
            //apply the default truth value for specified punctuation
            truth = new DefaultTruth(punc, memory);
        }


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






        //---- VALID TASK BEYOND THIS POINT

        /** NaN quality is a signal that a budget's values need initialized */
        if (!Float.isFinite(qua())) {
            //HACK for now just assume that only MutableTask supports unbudgeted input
            memory.applyDefaultBudget((MutableTask)this);
        }



        //finally, assign a unique stamp if none specified (input)
        if (evidence() == null) {
            if (!isInput()) {
                throw new RuntimeException("non-Input task without evidence: " + this);
            } else {

                setEvidence(memory.newStampSerial());

                //this actually means it arrived from unknown origin.
                //we'll clarify what null evidence means later.
                //if data arrives via a hardware device, can a virtual
                //task be used as the parent when it generates it?
                //doesnt everything originate from something else?
                if (log == null)
                    log("Input");
            }
        }


        //hash = rehash();

        onInput(memory);

        return this;
    }

    @Nullable
    @Override
    public TaskState state() {
        return state;
    }

    /** if validated and entered into the system. can be overridden in subclasses to handle this event
     *  isnt called for Command tasks currently; they will be executed right away anyway
     * */
    protected void onInput(Memory m) {

    }

    /** when executed; can be overridden in subclasses to handle this event;
     *  returns whether there was any activity executed
     * */
    @Override public boolean execute(NAR n) {

        //DEFAULT EXECUTION PROCEDURE: trigger listener reactions

        Topic<Task> tt = n.exe.get(
            Operator.operator(term())
        );

        boolean executable = (tt != null && !tt.isEmpty());
        if (executable) {
            //beforeNextFrame( //<-- enqueue after this frame, before next

            tt.emit(this);

//            if (!inputGoal.isEternal()) {
//                //execution drains temporal task's budget in proportion to durability
//                Budget inputGoalBudget = inputGoal.budget();
//                inputGoalBudget.priMult(1f - inputGoalBudget.dur());
//            }

        }
        return executable;
    }

    /** includes: evidentialset, occurrencetime, truth, term, punctuation */
    private int rehash() {

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
    public final void onConcept(Concept c) {

        //intermval generally contains unique information that should not be replaced
        //if (term instanceof TermMetadata)
            //return;

        //if debug, check that they are equal..
        //term = (Compound) c.getTerm(); //HACK the cast
    }

    @Override
    public final Compound term() {
        return term.term();
    }

    @Override
    public Truth truth() {
        return truth;
    }

    @Override
    public void setTruth(Truth t) {
        if (!Objects.equals(truth, t)) {
            truth = t;
            invalidate();
        }
    }

    @Override
    public final boolean isAnticipated() {
        return isJudgment() && !isEternal() && (state() == TaskState.Anticipated || isInput());
    }

    @NotNull
    protected Task setEvidence(long... evidentialSet) {
        if (this.evidentialSet!=evidentialSet) {
            this.evidentialSet = evidentialSet;
            invalidate();
        }
        return this;
    }

    @Override
    public final boolean isDouble() {
        return getParentBelief() != null;
    }

    @Override
    public final boolean isSingle() {
        return getParentBelief()==null;
    }





    @Override
    public final char punc() {
        return punctuation;
    }

    @Nullable
    @Override
    public final long[] evidence() {
        long[] e = this.evidentialSet;
        if (e == null) {
            updateEvidence();
            e = this.evidentialSet;
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
            int tu = Truth.compare(o.truth(), tr);
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

        if (getParentTask()!=null) {
            if (isDouble())
                setEvidence( Stamp.zip(getParentTask(), getParentBelief() ));
            else if ( isSingle() )
                setEvidence(getParentTask().evidence());
        } else {
            setEvidence((long[])null);
        }

    }

    public final void invalidate() {
        hash = 0;
    }

    @Override
    public void setOccurrenceTime(long o) {
        if (o != occurrenceTime) {
            this.occurrenceTime = o;
            invalidate();
        }
    }

    @Override
    public final void setEternal() {
        setOccurrenceTime(Tense.ETERNAL);
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
        if (this == that) return true;
        //if (that instanceof Task) {

            //hash test has probably already occurred, coming from a HashMap
            if (hashCode() != that.hashCode()) return false;

            return equivalentTo((Task) that, true, true, true, true, false);
        //}
        //return false;
    }

    @Override
    public final boolean equivalentTo(@NotNull Task that, boolean punctuation, boolean term, boolean truth, boolean stamp, boolean creationTime) {

        if (this == that) return true;

        char thisPunc = punc();

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


    @Nullable
    @Override
    public Reference<Task> getParentTaskRef() {
        return parentTask;
    }

    @Nullable
    @Override
    public Reference<Task> getParentBeliefRef() {
        return parentBelief;
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
        return dereference(log);
    }


    @NotNull
    final List getOrCreateLog() {
        List exist = log();
        if (exist == null) {
            this.log = reference(exist = Global.newArrayList(1));
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



    /**
     * Get the parent belief of a task
     *
     * @return The belief from which the task is derived
     */
    @Nullable
    @Override
    public final Task getParentBelief() {
        return dereference(parentBelief);
    }



    @NotNull
    @Override
    public final Task name() {
        return this;
    }

    @NotNull
    @Override
    @Deprecated
    public String toString() {
        return appendTo(null, null).toString();
    }

    @Override
    public long start() {
        return occurrenceTime;
    }

    @Override
    public long end() {
        return occurrenceTime;// + duration;
    }


}
