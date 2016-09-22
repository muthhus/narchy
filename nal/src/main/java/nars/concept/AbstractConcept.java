package nars.concept;

import javassist.scopedpool.SoftValueHashMap;
import nars.NAR;
import nars.budget.policy.ConceptPolicy;
import nars.term.container.TermContainer;
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


//    /**
//     * attempt insert a tasklink into this concept's tasklink bag
//     * return true if successfully inserted
//     *
//     * when a task is processed, a tasklink
//     * can be created at the concept of its term
//     *
//     * @return whether the link successfully was completed
//     */
//    static boolean link(Concept c, float scale, float minScale, @NotNull Activation activation) {
//
//        if (scale < minScale)
//            return false;
//
//        Budgeted b = activation.in;
//        if (b instanceof Task) {
//            c.linkTask((Task)b, scale);
//        }
//
//        return true;
//    }


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
