package nars.concept;

import javassist.scopedpool.SoftValueHashMap;
import nars.NAR;
import nars.budget.Budgeted;
import nars.budget.policy.ConceptPolicy;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;
import nars.term.variable.Variable;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.BiFunction;

public interface AbstractConcept extends Concept, Term {

    //private final Bag<Task> taskLinks;
    //private final Bag<Termed> termLinks;

    //@NotNull
    //public final T term;


    //@Nullable
    //private Map meta;

    //transient final int _hash;


//    protected AbstractConcept(@NotNull T term, Bag<Termed> termLinks, Bag<Task> taskLinks) {
//        this.term = term;
//        this.taskLinks = taskLinks;
//        this.termLinks = termLinks;
//
//        //_hash = term.hashCode();
//
//    }

    //public static final Logger logger = LoggerFactory.getLogger(AbstractConcept.class);

    /** returns the outgoing component only */
    @Nullable
    static Concept linkSub(@NotNull Concept source, @NotNull Termed targetTerm,
                           @NotNull Budgeted b, float subScale, boolean alsoReverse,
                           @Nullable MutableFloat conceptOverflow,
                           @Nullable MutableFloat termlinkOverflow, @NotNull NAR nar) {

        /*
        if (!(task.isStructual() && (cLink0.getType() == TermLink.TRANSFORM))) {

        }*/



        /* activate concept */
        Concept target = nar.conceptualize(targetTerm, b, subScale,
                0f /* zero prevents direct recursive linking, it should go through the target concept though and happen through there */,
                conceptOverflow);

        if (targetTerm instanceof Variable) {

        } else {
            if (target == null)
                throw new RuntimeException("termlink to null concept: " + targetTerm);
        }

        if (target == source)
            throw new RuntimeException("termlink self-loop");



        /* insert termlink target to source */
        if (target!=null && alsoReverse) {
            subScale /= 2; //divide among both directions
            target.termlinks().put(source, b, subScale, termlinkOverflow);
        }

        /* insert termlink source to target */
        source.termlinks().put(target != null ? target : targetTerm, b, subScale, termlinkOverflow);

        return target;
    }


//    @Override @NotNull
//    public Term term() {
//        return term;
//    }

//    /**
//     * metadata table where processes can store and retrieve concept-specific data by a key. lazily allocated
//     */
//    @Nullable
//    @Override
//    public final Map meta() {
//        return meta;
//    }


    /** should not be called directly */
    void setMeta(@NotNull Map newMeta);


    @NotNull
    @Override default <C> C meta(@NotNull Object key, @NotNull BiFunction value) {
        @Nullable Map meta = meta();
        if (meta == null) {
            Object v;
            put(key, v = value.apply(key, null));
            return (C)v;
        } else {
            return (C) meta.compute(key, value);
        }
    }

    /** like Map.put for storing data in meta map
     *  @param value if null will perform a removal
     * */
    @Override
    @Nullable
    default Object put(@NotNull Object key, @Nullable Object value) {

        Map currMeta = meta();

        if (value != null) {

            if (currMeta == null) {
                setMeta(  currMeta =
                        //new WeakIdentityHashMap();
                        new SoftValueHashMap(1) );
            }

            return currMeta.put(key, value);
        }
        else {
            return currMeta != null ? currMeta.remove(key) : null;
        }

    }

//    @Override
//    public final boolean equals(@NotNull Object obj) {
//        return (this == obj) || term.equals(obj);
//    }
//
//    @Override
//    public final int hashCode() {
//        return term.hashCode();
//        //return _hash;
//    }

//    /**
//     * Return a string representation of the concept, called in ConceptBag only
//     *
//     * @return The concept name, with taskBudget in the full version
//     */
//    @Override
//    default String toString() {  // called from concept bag
//        //return (super.toStringBrief() + " " + key);
//        //return super.toStringExternal();
//        return term().toString();
//    }

//    /**
//     * Task links for indirect processing
//     */
//    @NotNull
//    @Override
//    public final Bag<Task> tasklinks() {
//        return taskLinks;
//    }

//    /**
//     * Term links between the term and its components and compounds; beliefs
//     */
//    @NotNull
//    @Override
//    public final Bag<Termed> termlinks() {
//        return termLinks;
//    }


//    public final boolean isConceptOf(@NotNull Termed t) {
//        return t == this || (t.term() == this);
//        //t.equalsAnonymously(term());
//    }

    @Override
    default void capacity(@NotNull ConceptPolicy p) {
        linkCapacity(p);
    }

    default void linkCapacity(@NotNull ConceptPolicy p) {
        termlinks().setCapacity( p.linkCap(this, true) );
        tasklinks().setCapacity( p.linkCap(this, false) );
    }


    default void delete() {
        termlinks().clear();
        tasklinks().clear();
    }

    @Override
    default void linkTask(@NotNull Task t, float scale) {
       tasklinks().put(t, t, scale, null);
    }




//    /**
//     * called from {@link NARRun}
//     */
//    @Override
//    public String toStringLong() {
//        String res =
//                toStringWithBudget() + " " + getTerm().toString()
//                        + toStringIfNotNull(getTermLinks().size(), "termLinks")
//                        + toStringIfNotNull(getTaskLinks().size(), "taskLinks")
//                        + toStringIfNotNull(getBeliefs().size(), "beliefs")
//                        + toStringIfNotNull(getGoals().size(), "goals")
//                        + toStringIfNotNull(getQuestions().size(), "questions")
//                        + toStringIfNotNull(getQuests().size(), "quests");
//
//        //+ toStringIfNotNull(null, "questions");
//        /*for (Task t : questions) {
//            res += t.toString();
//        }*/
//        // TODO other details?
//        return res;
//    }

//    private String toStringIfNotNull(final Object item, final String title) {
//        if (item == null) {
//            return "";
//        }
//
//        final String itemString = item.toString();
//
//        return new StringBuilder(2 + title.length() + itemString.length() + 1).
//                append(' ').append(title).append(':').append(itemString).toString();
//    }

//    /** called by memory, dont call self or otherwise */
//    public void delete() {
//        /*if (getMemory().inCycle())
//            throw new RuntimeException("concept " + this + " attempt to delete() during an active cycle; must be done between cycles");
//        */
//
//        if (getMeta() != null) {
//            getMeta().clear();
//            setMeta(null);
//        }
//        //TODO clear bags
//    }



}
