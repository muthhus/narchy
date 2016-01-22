package nars.concept;

import nars.Global;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Created by me on 7/29/15.
 */
public abstract class AbstractConcept implements Concept {

    private final Term term;

    @Nullable
    protected Map meta = null;

    protected AbstractConcept(Term term) {
        this.term = term;
    }


    @Override
    public final Term term() {
        return term;
    }

    /**
     * metadata table where processes can store and retrieve concept-specific data by a key. lazily allocated
     */
    @Nullable
    @Override
    public final Map<Object, Object> getMeta() {
        return meta;
    }


    private void setMeta(Map<Object, Object> meta) {
        this.meta = meta;
    }



    /** like Map.put for storing data in meta map
     *  @param value if null will perform a removal
     * */
    @Nullable
    public final Object put(@NotNull Object key, @Nullable Object value) {

        Map<Object, Object> currMeta = getMeta();

        if (value != null) {

            if (currMeta == null) setMeta(currMeta = Global.newHashMap());

            return currMeta.put(key, value);
        }
        else {
            return currMeta != null ? currMeta.remove(key) : null;
        }

    }

    @Override
    public final boolean equals(Object obj) {
        return (this == obj) || ((obj instanceof Termed)
                && ((Termed) obj).term().equals(term));
    }

    @Override
    public final int hashCode() {
        return term.hashCode();
    }

    @Override
    public final int compareTo(Termed o) {
        return term.compareTo(o.term());
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
