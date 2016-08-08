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
import nars.NAR;
import nars.Symbols;
import nars.bag.Bag;
import nars.budget.Budget;
import nars.budget.Budgeted;
import nars.budget.policy.ConceptPolicy;
import nars.concept.table.BeliefTable;
import nars.concept.table.QuestionTable;
import nars.concept.table.TaskTable;
import nars.link.BLink;
import nars.task.Revision;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Concept extends Termed {

    @NotNull Bag<Task> tasklinks();

    @NotNull Bag<Term> termlinks();

    @Nullable Map<Object, Object> meta();


    @Nullable
    Object put(@NotNull Object key, @Nullable Object value);

    /**
     * like Map.gett for getting data stored in meta map
     */
    @Nullable
    default <C> C get(@NotNull Object key) {
        Map m = meta();
        return null == m ? null : (C) m.get(key);
    }

    /** follows Map.compute() semantics */
    @NotNull
    <C> C meta(Object key, BiFunction value);


    @NotNull BeliefTable beliefs();

    @NotNull BeliefTable goals();

    @NotNull QuestionTable questions();

    @NotNull QuestionTable quests();

    @Nullable
    default Truth belief(long when, long now) {
        return hasBeliefs() ? beliefs().truth(when, now) : null;
    }

    @Nullable
    default Truth desire(long when, long now) {
        return hasGoals() ? goals().truth(when, now) : null;
    }

    @Nullable
    default Truth belief(long now) {
        return belief(now, now);
    }

    @Nullable
    default Truth desire(long now) {
        return desire(now, now);
    }


//    boolean contains(Task t);


    default boolean hasGoals() {
        return !goals().isEmpty();
    }

    default boolean hasBeliefs() {
        return !beliefs().isEmpty();
    }

    default boolean hasQuestions() { return !questions().isEmpty();    }

    default boolean hasQuests() {
        return !quests().isEmpty();
    }


    @Nullable
    default TaskTable tableFor(char punc) {
        switch(punc) {
            case Symbols.BELIEF: return beliefs();
            case Symbols.GOAL: return goals();
            case Symbols.QUESTION: return questions();
            case Symbols.QUEST: return quests();
            default:
                throw new UnsupportedOperationException();
        }
    }

//    default BeliefTable tableAnswering(char punc) {
//        switch (punc) {
//            case Symbols.QUESTION: return beliefs();
//            case Symbols.QUEST: return goals();
//            default:
//                throw new UnsupportedOperationException();
//        }
//    }


    default @Nullable Task merge(@NotNull Task x, @NotNull Task y, long when, @NotNull NAR nar) {
        long now = nar.time();
        Truth truth = ((BeliefTable) tableFor(y.punc())).truth(when, now);
        if (truth == null)
            return null;
        return Revision.merge(x, y, when, now, truth );
    }

    /**
     * process a task in this concept
     *
     * @param displaced collects tasks which have been displaced by the potential insertion of this task
     * @return true if process affected the concept (ie. was inserted into a belief table)
     */
    Task process(@NotNull Task task, @NotNull NAR nar);


    /**
     * attempt insert a tasklink into this concept's tasklink bag
     * return true if successfully inserted
     *
     * when a task is processed, a tasklink
     * can be created at the concept of its term
     *
     * @return whether the link successfully was completed
     */
    default boolean link(float linkScale, float minScale, @NotNull NAR nar, NAR.Activation activation) {

        Budgeted b = activation.in;
        if (b instanceof Task) {
            linkTask((Task)b, linkScale);
        }

        activation.concepts.addToValue(this, linkScale);

        linkAny(b, linkScale, minScale, nar, activation);

        return true;
    }

    void linkAny(@NotNull Budgeted b, float scale, float minScale, @NotNull NAR nar, NAR.Activation activation);


    void linkTask(@NotNull Task t, float scale);



    default boolean link(@NotNull Budgeted b, float initialScale, @NotNull NAR nar, NAR.Activation activation) {
        float p = b.priIfFiniteElseNeg1();
        if (p < 0)
            return false;

        return link(initialScale,
                nar.taskLinkThreshold.floatValue() / p, //minScale
                nar, activation);
    }

    /**
     * @param thisTask  task with a term equal to this concept's
     * @param otherTask task with a term equal to another concept's
     * @return number of links created (0, 1, or 2)
     */
    default void crossLink(@NotNull Task thisTask, @NotNull Task otherTask, float scale, @NotNull NAR nar) {

        Concept other = nar.concept(otherTask);
        if (other == null || other.equals(this))
            return; //null or same concept

        float halfScale = scale / 2f;

        NAR.Activation a = new NAR.Activation(thisTask);

        link(otherTask, halfScale, nar, a);
        other.link(thisTask, halfScale, nar, a);

        a.run(nar);

    }

    /** link to a specific peer */
    static <T> void linkPeer(@NotNull Bag<T> bag, @NotNull T x, @NotNull Budget b, float q) {
        //@NotNull Bag<Termed> bag = termlinks();
        BLink<? extends T> existing = bag.get(x);
        if (existing == null)
            return;

        /*
        Hebbian Learning:
            deltaWeight = (input[fromNeuron] -
                            output[toNeuron] * weight(fromNeuron,toNeuron)
							* output[toNeuron] * this.learningRate);
							*
            deltaWeight = input[toNeuron] * output[toNeuron] * learningRate;
            deltaWeight = (input - netInput) * output * this.learningRate; // is it right to use netInput here?
			deltaWeight = input * desiredOutput * this.learningRate;
         */


        boolean init;
        if (existing == null ) {
            bag.put(x, b, q, null);
            init = true;
        } else {
            init = false;
        }

        float bp = b.pri();
        if (bp == bp /*!NaN */) {

            final float learningRate = (bp * q) / bag.capacity();
            //System.out.println(this + " activating " + x);
            bag.forEach(tl -> {
                //                if (active && init)
//                    return; //dont modify the newly inserted link

                float p = tl.pri();
                if (p!=p) //the link is currently deleted
                    return;

                boolean active = tl == existing;
                float dp = (active ? learningRate : -learningRate);
                tl.priAdd(dp);
                //System.out.println(tl.toString2());
            });

        }

    }

//    /** link to all existing termlinks, hierarchical and heterarchical */
//    default void linkPeers(@NotNull Budgeted b, float scale, @NotNull NAR nar, boolean recurse) {
//        List<Termed> targets = Global.newArrayList(termlinks().size());
//        termlinks().forEach(tl -> targets.add(tl.get()));
//        float subScale = scale / targets.size();
//        targets.forEach(t -> {
//            //System.out.println(Concept.this + " activate " + t + " " + b + "x" + subScale);
//            termlinks().put(t, b, subScale, null); //activate the termlink
//            nar.conceptualize(t, b, subScale, recurse ? subScale : 0f, null);
//        });
//
//    }


    default void visitTasks(@NotNull Consumer<Task> each, boolean includeConceptBeliefs, boolean includeConceptQuestions, boolean includeConceptGoals, boolean includeConceptQuests) {
        if (includeConceptBeliefs) beliefs().forEach(each);
        if (includeConceptQuestions) questions().forEach(each);
        if (includeConceptGoals) goals().forEach(each);
        if (includeConceptQuests) quests().forEach(each);
    }


    @NotNull
    default Iterator<Task> iterateTasks(boolean onbeliefs, boolean ongoals, boolean onquestions, boolean onquests) {

        TaskTable beliefs = onbeliefs ? beliefs() : null;
        TaskTable goals = ongoals ? goals() : null;
        TaskTable questions = onquestions ? questions() : null;
        TaskTable quests = onquests ? quests() : null;

        Iterator<Task> b1 = beliefs != null ? beliefs.iterator() : Collections.emptyIterator();
        Iterator<Task> b2 = goals != null ? goals.iterator() : Collections.emptyIterator();
        Iterator<Task> b3 = questions != null ? questions.iterator() : Collections.emptyIterator();
        Iterator<Task> b4 = quests != null ? quests.iterator() : Collections.emptyIterator();
        return Iterators.concat(b1, b2, b3, b4);
    }



    default void print() {
        print(System.out);
    }

    default void print(@NotNull PrintStream out) {
        print(out, true, true, true, true);
    }


    /**
     * prints a summary of all termlink, tasklink, etc..
     */
    default void print(@NotNull PrintStream out, boolean showbeliefs, boolean showgoals, boolean showtermlinks, boolean showtasklinks) {

        out.println("concept: " + toString());

        String indent = "  \t";

        Consumer<Task> printTask = s -> {
            out.print(indent);
            out.print(s);
            out.print(" ");
            out.print(s.lastLogged());
            out.println();
        };

        if (showbeliefs) {
            out.print(" Beliefs:");
            if (beliefs().isEmpty()) out.println(" none");
            else {
                out.println(); beliefs().forEach(printTask);
            }
            out.print(" Questions:");
            if (questions().isEmpty()) out.println(" none");
            else {
                out.println(); questions().forEach(printTask);
            }
        }

        if (showgoals) {
            out.print(" Goals:");
            if (goals().isEmpty()) out.println(" none");
            else {
                out.println();                goals().forEach(printTask);
            }
            out.print(" Quests:");
            if (questions().isEmpty()) out.println(" none");
            else {
                out.println();                quests().forEach(printTask);
            }
        }

        if (showtermlinks) {
            //out.print("TermLinkTemplates: ");
            //out.println(termlinkTemplates());

            out.println("\n TermLinks:");
            termlinks().forEach(b -> {
                out.print(indent);
                out.print(b.get() + " " + b.toBudgetString());
                out.print(" ");
            });
        }

        if (showtasklinks) {
            out.println("\n TaskLinks:");
            tasklinks().forEach(b -> {
                out.print(indent);
                out.print(b.get() + " " + b.toBudgetString());
                out.print(" ");
            });
        }

        out.println('\n');
    }

    void delete();

    @Nullable ConceptPolicy policy();

    void policy(@Nullable ConceptPolicy c, long now);

    default boolean active() {
        return policy()!=null;
    }


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

    /**
     * Created by me on 3/23/16.
     */
    interface ConceptBuilder extends Function<Term, Termed> {

//        @NotNull
//        Bag<Task> taskbag();
//        Bag<Term> termbag(Map<Term, Term> map);

        ConceptPolicy init();
        ConceptPolicy awake();
        ConceptPolicy sleep();
    }
}
