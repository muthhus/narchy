/*
 * Concept.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.concept;

import com.google.common.collect.Iterators;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Longs;
import nars.NAR;
import nars.bag.Bag;
import nars.concept.util.BeliefTable;
import nars.concept.util.TaskTable;
import nars.task.Task;
import nars.term.Termed;
import nars.term.compound.Compound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;

public interface Concept extends Termed, Comparable<Termed> {

    Bag<Task> getTaskLinks();
    Bag<Termed> getTermLinks();

    @Nullable
    Map<Object, Object> getMeta();

    default boolean hasGoals() {
        return !getGoals().isEmpty();
    }

    default boolean hasBeliefs() {
        return !getBeliefs().isEmpty();
    }

    default boolean hasQuestions() {
        return !getQuestions().isEmpty();
    }

    default boolean hasQuests() {
        return !getQuests().isEmpty();
    }


    @NotNull
    default String toInstanceString() {
        String id = Integer.toString(System.identityHashCode(this), 16);
        return this + "::" + id;
    }

    Object put(@NotNull Object key, @Nullable Object value);


    /** like Map.gett for getting data stored in meta map */
    @Nullable
    default <C> C get(@NotNull Object key) {
        Map<Object, Object> m;
        return null == (m = getMeta()) ? null : (C) m.get(key);
    }

    /**
     * Get the current overall desire value. TODO to be refined
     */
    default float getDesire(long now) {
//        return hasGoals() ?
//            getGoals().getMeanProjectedExpectation(now) : 0;
        return hasGoals() ?
            getGoals().top(now).truth().getExpectation() : 0;
    }
    /**
     * Get the current overall belief value. TODO to be refined
     */
    default float getBelief(long now) {
//        return hasBeliefs() ?
//                getBeliefs().getMeanProjectedExpectation(now) : 0;
        return hasBeliefs() ?
                getBeliefs().top(now).truth().getExpectation() : 0;
    }

    /** satisfaction/success metric:
     * if desire exists, returns 1.0 / (1 + Math.abs(belief - desire))
     *  otherwise zero */
    default float getSuccess(long now) {
        return hasBeliefs() && hasGoals()
                ?
                1.0f / (1.0f +
                        Math.abs(getBelief(now) - getDesire(now))) :
                0;
    }

    @Nullable
    BeliefTable getBeliefs();
    @Nullable
    BeliefTable getGoals();

    @Nullable
    TaskTable getQuestions();
    @Nullable
    TaskTable getQuests();



//    /** debugging utility */
//    default public void ensureNotDeleted() {
//        if (isDeleted())
//            throw new RuntimeException("Deleted concept should not activate TermLinks");
//    }




    @Nullable
    Task processBelief(Task task, NAR nar);

    @Nullable
    Task processGoal(Task task, NAR nar);

    Task processQuestion(Task task, NAR nar);

    Task processQuest(Task task, NAR nar);





//    /** returns the best belief of the specified types */
//    default public Task getStrongestBelief(boolean eternal, boolean nonEternal) {
//        return getBeliefs().top(eternal, nonEternal);
//    }
//
//
//    default public Task getStrongestGoal(boolean eternal, boolean nonEternal) {
//        return getGoals().top(eternal, nonEternal);
//    }

//
//    default Iterator<? extends Termed> getTermedAdjacents(boolean termLinks, boolean taskLinks) {
//        if (termLinks && taskLinks) {
//            return concat(
//                    getTermLinks().iterator(), getTaskLinks().iterator()
//            );
//        }
//        if (termLinks) {
//            return getTermLinks().iterator();
//        }
//        if (taskLinks) {
//            return getTaskLinks().iterator();
//        }
//
//        return null;
//    }

    default void print() {
        print(System.out);
    }

    default void print(@NotNull PrintStream out) {
        print(out, true, true, true, true);
    }

    /** prints a summary of all termlink, tasklink, etc.. */
    default void print(@NotNull PrintStream out, boolean showbeliefs, boolean showgoals, boolean showtermlinks, boolean showtasklinks) {

        out.println("concept: " + toInstanceString());

        String indent = "  \t";
        if (showbeliefs) {
            out.print(" Beliefs:");
            if (getBeliefs().isEmpty()) out.println(" none");
            else {out.println();
            getBeliefs().forEach(s -> {
                out.print(indent); out.println(s);
            });}
            out.print(" Questions:");
            if (getQuestions().isEmpty()) out.println(" none");
            else {out.println();
            getQuestions().forEach(s -> {
                out.print(indent); out.println(s);
            });}
        }

        if (showgoals) {
            out.print(" Goals:");
            if (getGoals().isEmpty()) out.println(" none");
            else {out.println();
            getGoals().forEach(s -> {
                out.print(indent);
                out.println(s);
            });}
            out.print(" Quests:");
            if (getQuestions().isEmpty()) out.println(" none");
            else {out.println();
                getQuests().forEach(s -> {
                    out.print(indent); out.println(s);
                });}
        }

        if (showtermlinks) {

            out.println("\n TermLinks:");
            getTermLinks().top(b-> {
                out.print(indent);
                out.print(b.get() + " " + b.toBudgetString());
                out.print(" ");
            });
        }

        if (showtasklinks) {
            out.println("\n TaskLinks:");
            getTaskLinks().top(b-> {
                out.print(indent);
                out.print(b.get() + " " + b.toBudgetString());
                out.print(" ");
            });
        }

        out.println();
    }


//    default Iterator<Term> adjacentTerms(boolean termLinks, boolean taskLinks) {
//        return transform(adjacentTermables(termLinks, taskLinks), Termed::getTerm);
//    }

//    default Iterator<Concept> adjacentConcepts(boolean termLinks, boolean taskLinks) {
//        final Iterator<Concept> termToConcept = transform(adjacentTerms(termLinks, taskLinks), new Function<Termed, Concept>() {
//            @Override
//            public Concept apply(final Termed term) {
//                return getMemory().concept(term.getTerm());
//            }
//        });
//        return filter(termToConcept, Concept.class); //should remove null's (unless they never get included anyway), TODO Check that)
//    }



    @Nullable
    Termed[] getTermLinkTemplates();

    Ordering<Task> taskCreationTime = new Ordering<Task>() {
        @Override
        public int compare(@NotNull Task left, @NotNull Task right) {
            return Longs.compare(
                    left.getCreationTime(),
                    right.getCreationTime());
        }
    };

    @NotNull
    default Iterator<Task> iterateTasks(boolean onbeliefs, boolean ongoals, boolean onquestions, boolean onquests) {

        TaskTable beliefs = onbeliefs ? getBeliefs() : null;
        TaskTable goals =   ongoals ? getGoals() : null ;
        TaskTable questions = onquestions ?  getQuestions() : null;
        TaskTable quests = onquests ? getQuests() : null;

        Iterator<Task> b1 = beliefs != null ? beliefs.iterator() : Iterators.emptyIterator();
        Iterator<Task> b2 = goals != null ? goals.iterator() : Iterators.emptyIterator();
        Iterator<Task> b3 = questions != null ? questions.iterator() : Iterators.emptyIterator();
        Iterator<Task> b4 = quests != null ? quests.iterator() : Iterators.emptyIterator();
        return Iterators.concat(b1, b2, b3, b4);

    }



    /**
     * process a task in this concept
     * @return true if process affected the concept (ie. was inserted into a belief table)
     */
    @NotNull
    Task process(@NotNull Task task, @NotNull NAR nar);

    /** attempt insert a tasklink into this concept's tasklink bag
     *  return true if successfully inserted
     * */
    boolean link(@NotNull Task task, float scale, float minScale, @NotNull NAR nar);

    //void linkTemplates(Budget budget, float scale, NAR nar);




    default boolean link(@NotNull Task task, float initialScale, @NotNull NAR nar) {
        float minScale =
            nar.memory.taskLinkThreshold.floatValue() / task.getBudget().summary();

        if (!Float.isFinite(minScale)) return false;
        return link(task, initialScale, minScale, nar);
    }

    /**
     *
     * @param thisTask task with a term equal to this concept's
     * @param otherTask task with a term equal to another concept's
     * @return number of links created (0, 1, or 2)
     */
    default void crossLink(@NotNull Task thisTask, @NotNull Task otherTask, float initialScale, @NotNull NAR nar) {
        Compound otherTerm = otherTask.term();
        if (otherTerm.equals(term()))
            return; //self

        link(otherTask, initialScale, nar);

        Concept other = nar.conceptualize(otherTask, thisTask.getBudget(), initialScale);
        if (other == null)
            return;

        other.link(thisTask, initialScale, nar);
    }



//    public Task getTask(boolean hasQueryVar, long occTime, Truth truth, List<Task>... lists);
//
//    default public Task getTask(Sentence query, List<Task>... lists) {
//        return getTask(query.hasQueryVar(), query.getOccurrenceTime(), query.getTruth(), lists);
//    }

}
