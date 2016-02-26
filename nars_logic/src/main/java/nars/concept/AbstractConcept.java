package nars.concept;

import nars.Global;
import nars.NAR;
import nars.bag.Bag;
import nars.budget.Budget;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Created by me on 7/29/15.
 */
public abstract class AbstractConcept implements Concept {

    protected final Bag<Task> taskLinks;
    protected final Bag<Termed> termLinks;

    @NotNull
    public final Term term;

    @Nullable
    protected Map meta = null;

    protected AbstractConcept(@NotNull Term term, Bag<Task> taskLinks, Bag<Termed> termLinks) {
        this.term = term;
        this.taskLinks = taskLinks;
        this.termLinks = termLinks;
    }

    public static final void linkTerm(@NotNull Concept source, @NotNull Concept target, Budget b, float subScale, boolean out, boolean in) {

        if (source == target)
            throw new RuntimeException("termlink self-loop");

        /** activate local's termlink to template */
        //float termlinkScale = termLinkOut(this, target.term());
        if (out)
            source.termlinks().put(target, b, subScale /* * termlinkScale*/);

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


    private void setMeta(@NotNull Map<Object, Object> meta) {
        this.meta = meta;
    }



    /** like Map.put for storing data in meta map
     *  @param value if null will perform a removal
     * */
    @Nullable
    public final Object put(@NotNull Object key, @Nullable Object value) {

        Map<Object, Object> currMeta = meta();

        if (value != null) {

            if (currMeta == null)
                setMeta(currMeta = Global.newHashMap()); //lazy allocate

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
    @Override public boolean link(@NotNull Task task, float scale, float minScale, @NotNull NAR nar) {

        //activate tasklink locally
        Budget taskBudget = task.budget();

        /*if (taskLinkOut(this, t)) {*/
            taskLinks.put(task, taskBudget, scale);
        //}

        return true;
    }

    public final void linkTerm(@NotNull Concept target, Budget b, float subScale) {
        linkTerm(this, target, b, subScale, true, true);
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
