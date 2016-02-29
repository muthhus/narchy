package nars.concept;

import com.sun.jna.WeakIdentityHashMap;
import nars.NAR;
import nars.bag.Bag;
import nars.budget.Budgeted;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Created by me on 7/29/15.
 */
public abstract class AbstractConcept<T extends Term> implements Concept {

    protected final Bag<Task> taskLinks;
    protected final Bag<Termed> termLinks;

    @NotNull
    public final T term;

    @Nullable
    protected Map meta;

    protected AbstractConcept(@NotNull T term, Bag<Task> taskLinks, Bag<Termed> termLinks) {
        this.term = term;
        this.taskLinks = taskLinks;
        this.termLinks = termLinks;
    }

    public static final Logger logger = LoggerFactory.getLogger(AbstractConcept.class);

    public static final void linkTerm(@NotNull Concept source, @NotNull Concept target,
                                      @NotNull Budgeted b, float subScale, boolean out, boolean in) {

        /*if (logger.isDebugEnabled())
            logger.debug("TermLink: {} <//> {} ", source, target); */

        if (source == target)
            throw new RuntimeException("termlink self-loop");

        /** activate local's termlink to template */
        if (out)
            source.termlinks().put(target, b, subScale);

        /** activate (reverse) template's termlink to local */
        if (in)
            target.termlinks().put(source, b, subScale);

    }


    @Override @NotNull
    public Term term() {
        return term;
    }

    /**
     * metadata table where processes can store and retrieve concept-specific data by a key. lazily allocated
     */
    @Nullable
    @Override
    public final Map<Object, Object> meta() {
        return meta;
    }


    @Override public Object putCompute(Object key, BiFunction value) {
        if (meta == null) {
            Object v;
            put(key, v = value.apply(key, null));
            return v;
        } else {
            return meta.compute(key, value);
        }
    }

    /** like Map.put for storing data in meta map
     *  @param value if null will perform a removal
     * */
    @Override
    @Nullable
    public final Object put(@NotNull Object key, @Nullable Object value) {

        Map<Object, Object> currMeta = meta;

        if (value != null) {

            if (currMeta == null) {
                this.meta = currMeta = new WeakIdentityHashMap();
            }

            return currMeta.put(key, value);
        }
        else {
            return currMeta != null ? currMeta.remove(key) : null;
        }

    }

    @Override
    public final boolean equals(Object obj) {
        return (this == obj) || term.equals(obj);
    }

    @Override
    public final int hashCode() {
        return term.hashCode();
    }

    @Override
    public final int compareTo(@NotNull Object o) {
        return term.compareTo(o);
    }

    /**
     * Return a string representation of the concept, called in ConceptBag only
     *
     * @return The concept name, with taskBudget in the full version
     */
    @Override
    public final String toString() {  // called from concept bag
        //return (super.toStringBrief() + " " + key);
        //return super.toStringExternal();
        return term.toString();
    }

    /**
     * Task links for indirect processing
     */
    @Override
    public final Bag<Task> tasklinks() {
        return taskLinks;
    }

    /**
     * Term links between the term and its components and compounds; beliefs
     */
    @Override
    public final Bag<Termed> termlinks() {
        return termLinks;
    }

    /** atoms have no termlink templates, they are irreducible */
    @Nullable @Override public abstract List<Termed> termlinkTemplates();

    /**
     * when a task is processed, a tasklink
     * can be created at the concept of its term
     */
    @Override public boolean linkTask(@NotNull Budgeted task, float scale, float minScale, @NotNull NAR nar) {

        if (task instanceof Task)
            taskLinks.put((Task)task, task.budget(), scale);

        return true;
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
