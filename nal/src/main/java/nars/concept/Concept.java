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

import jcog.bag.Bag;
import jcog.pri.PriReference;
import nars.NAR;
import nars.Task;
import nars.conceptualize.state.ConceptState;
import nars.table.BeliefTable;
import nars.table.QuestionTable;
import nars.table.TaskTable;
import nars.term.Term;
import nars.term.Termed;
import nars.term.container.TermContainer;
import nars.util.SoftException;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static nars.Op.*;

public interface Concept extends Termed {



    @NotNull Bag<Task,PriReference<Task>> tasklinks();

    @NotNull Bag<Term,PriReference<Term>> termlinks();

    @Nullable Map meta();

    /**
     * should not be called directly
     */
    void setMeta(@NotNull Map newMeta);

    /**
     * follows Map.compute() semantics
     */
    @NotNull
    default <C> C meta(@NotNull Object key, @NotNull BiFunction value) {
        @Nullable Map meta = meta();
        if (meta == null) {
            if (value!=null) {
                Object v;
                put(key, v = value.apply(key, null));
                return (C) v;
            } else {
                return null;
            }
        } else {
            return (C) meta.compute(key, value);
        }
    }

    /**
     * like Map.gett for getting data stored in meta map
     */
    @Nullable
    default <C> C get(@NotNull Object key) {
        Map m = meta();
        return null == m ? null : (C) m.get(key);
    }

    default <C> C remove(@NotNull Object key) {

        Map m = meta();
        if (m == null)
            return null;
        //synchronized (m) {
        return (C) m.remove(key);
        //}

    }


    @NotNull BeliefTable beliefs();

    @NotNull BeliefTable goals();

    @NotNull QuestionTable questions();

    @Nullable QuestionTable quests();


    @Nullable
    default Map metaOrCreate() {
        Map<Object, Object> m = meta();
        if (m == null) {
            setMeta(
                m = new UnifiedMap(1)
                //TODO try FlatMap3
            );
            //new WeakIdentityHashMap();
            //new SoftValueHashMap(1));
        }
        return m;
    }

    Concept[] EmptyArray = new Concept[0];

    /**
     * like Map.put for storing data in meta map
     *
     * @param value if null will perform a removal
     */
    @Nullable
    default Object put(@NotNull Object key, @Nullable Object value) {
        if (value != null) {
            return metaOrCreate().put(key, value);
        } else {
            return remove(key);
        }
    }


    default void delete(@NotNull NAR nar) {

        termlinks().clear();
        tasklinks().clear();

        beliefs().clear();
        goals().clear();
        questions().clear();
        quests().clear();

        state(ConceptState.Deleted);

    }


    /**
     * same Map.putIfAbsent semantics: returns null if no previous value existed
     */
    default Object putIfAbsent(@NotNull Object key, @NotNull Object value) {
        synchronized (term()) {
            return metaOrCreate().putIfAbsent(key, value);
        }
    }

    default <X> X computeIfAbsent(@NotNull Object key, Supplier value) {
        synchronized (term()) {
            return (X) metaOrCreate().computeIfAbsent(key, (k) -> value.get());
        }
    }


    default boolean isDeleted() {
        return state() == ConceptState.Deleted;
    }

    @Nullable
    default TaskTable table(byte punc) {
        switch (punc) {
            case BELIEF:
                return beliefs();
            case GOAL:
                return goals();
            case QUESTION:
                return questions();
            case QUEST:
                return quests();
            default:
                throw new UnsupportedOperationException();
        }
    }


    default void forEachTask(boolean includeConceptBeliefs, boolean includeConceptQuestions, boolean includeConceptGoals, boolean includeConceptQuests, @NotNull Consumer<Task> each) {
        if (includeConceptBeliefs) beliefs().forEachTask(each);
        if (includeConceptQuestions) questions().forEachTask(each);
        if (includeConceptGoals) goals().forEachTask(each);
        if (includeConceptQuests) quests().forEachTask(each);
    }

    default void forEachTask(@NotNull Consumer<Task> each) {
        beliefs().forEachTask(each);
        questions().forEachTask(each);
        goals().forEachTask(each);
        quests().forEachTask(each);
    }

//    @NotNull
//    default Iterator<Task> iterateTasks(boolean onbeliefs, boolean ongoals, boolean onquestions, boolean onquests) {
//
//        TaskTable beliefs = onbeliefs ? beliefs() : null;
//        TaskTable goals = ongoals ? goals() : null;
//        TaskTable questions = onquestions ? questions() : null;
//        TaskTable quests = onquests ? quests() : null;
//
//        Iterator<Task> b1 = beliefs != null ? beliefs.taskIterator() : Collections.emptyIterator();
//        Iterator<Task> b2 = goals != null ? goals.taskIterator() : Collections.emptyIterator();
//        Iterator<Task> b3 = questions != null ? questions.taskIterator() : Collections.emptyIterator();
//        Iterator<Task> b4 = quests != null ? quests.taskIterator() : Collections.emptyIterator();
//        return Iterators.concat(b1, b2, b3, b4);
//    }


    default void print() {
        print(System.out);
    }

    default <A extends Appendable> A print(@NotNull A out) {
        print(out, true, true, true, true);
        return out;
    }


    /**
     * prints a summary of all termlink, tasklink, etc..
     */
    default void print(@NotNull Appendable out, boolean showbeliefs, boolean showgoals, boolean showtermlinks, boolean showtasklinks) {

        try {
            out.append("concept: ").append(toString()).append('\n');
            String indent = "  \t";

            Consumer<Task> printTask = s -> {
                try {
                    out.append(indent);
                    out.append(s.toString());
                    out.append(" ");
                    Object ll = s.lastLogged();
                    if (ll != null)
                        out.append(ll.toString());
                    out.append('\n');
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };

            Consumer<PriReference> printBagItem = b -> {
                try {
                    out.append(indent);
                    out.append(String.valueOf(b.get())).append(' ').append(b.toBudgetString());
                    out.append(" ");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };

            if (showbeliefs) {
                out.append(" Beliefs:");
                if (beliefs().isEmpty()) out.append(" none").append('\n');
                else {
                    out.append('\n');
                    beliefs().forEachTask(printTask);
                }
                out.append(" Questions:");
                if (questions().isEmpty()) out.append(" none").append('\n');
                else {
                    out.append('\n');
                    questions().forEachTask(printTask);
                }
            }

            if (showgoals) {
                out.append(" Goals:");
                if (goals().isEmpty()) out.append(" none").append('\n');
                else {
                    out.append('\n');
                    goals().forEachTask(printTask);
                }
                out.append(" Quests:");
                if (questions().isEmpty()) out.append(" none").append('\n');
                else {
                    out.append('\n');
                    quests().forEachTask(printTask);
                }
            }

            if (showtermlinks) {
                //out.append("TermLinkTemplates: ");
                //out.appendln(termlinkTemplates());

                out.append("\n TermLinks: ").append(String.valueOf(termlinks().size())).append(String.valueOf('/')).append(String.valueOf(termlinks().capacity())).append('\n');

                termlinks().forEach(printBagItem);
            }

            if (showtasklinks) {
                out.append("\n TaskLinks: ").append(String.valueOf(tasklinks().size())).append(String.valueOf('/')).append(String.valueOf(tasklinks().capacity())).append('\n');

                tasklinks().forEach(printBagItem);
            }

            out.append('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @NotNull ConceptState state();

    /**
     * returns the previous state
     */
    ConceptState state(@NotNull ConceptState c);

    /** can return null if no templates */
    @Nullable TermContainer templates();

    /**
     * Created by me on 9/13/16.
     */
    final class InvalidConceptException extends SoftException {

        @NotNull
        public final Termed term;
        @NotNull
        public final String reason;

        public InvalidConceptException(@NotNull Termed term, @NotNull String reason) {
            this.term = term;
            this.reason = reason;
        }

        @NotNull
        @Override
        public String getMessage() {
            return "InvalidConceptTerm: " + term + " (" + term.getClass() + "): " + reason;
        }

    }
}
