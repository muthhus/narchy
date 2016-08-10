package nars.concept;

import javassist.scopedpool.SoftValueHashMap;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.budget.policy.ConceptPolicy;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.BiFunction;

public interface AbstractConcept extends Concept {

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

    /** crosslinks termlinks */
    @Nullable
    static Concept linkSub(@NotNull Concept source, @NotNull Termed target,
                           float subScale,
                           @Nullable NAR.Activation activation, @NotNull NAR nar) {

        /* activate concept */
        Concept targetConcept;


        if (!linkable(target)) {
            targetConcept = null;
        } else {
            targetConcept = nar.concept(target, true);
            if (targetConcept == null)
                throw new NullPointerException(target + " did not resolve to a concept");
            //if (targetConcept!=null)


            activation.activate(targetConcept, subScale);
//            targetConcept = nar.activate(target,
//                    activation);
            //if (targetConcept == null)
                //throw new RuntimeException("termlink to null concept: " + target);
        }

        Term ttt = target.term();
//        if (tt.equals( source.term() ))
//            throw new RuntimeException("termlink self-loop");


//        /* insert termlink target to source */
        boolean alsoReverse = true;
        if (targetConcept!=null && alsoReverse) {
            subScale /= 2; //divide among both directions

            targetConcept.termlinks().put(source.term(), activation.in, subScale, activation.overflow);
        }

        /* insert termlink source to target */
        source.termlinks().put(ttt, activation.in, subScale, activation.overflow);



        return targetConcept;
    }

    static boolean linkable(@NotNull Termed target) {
//        return !(target instanceof Variable);
        if (target instanceof Variable) {
            return false;
        }
        Term x = target.term();
        if (x instanceof Compound) {

            if (x.op() == Op.NEG) {
                if (((Compound) x).term(0) instanceof Variable)
                    return false;
            }
        }
        return true;
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




    default void linkCapacity(@NotNull ConceptPolicy p) {
        termlinks().setCapacity( p.linkCap(this, true) );
        tasklinks().setCapacity( p.linkCap(this, false) );
    }


    @Override
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
