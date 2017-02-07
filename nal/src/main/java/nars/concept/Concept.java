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
import nars.$;
import nars.NAR;
import nars.Task;
import nars.attention.Activation;
import nars.bag.Bag;
import nars.conceptualize.state.ConceptState;
import nars.link.BLink;
import nars.table.BeliefTable;
import nars.table.QuestionTable;
import nars.table.TaskTable;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import nars.util.SoftException;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static nars.Op.*;

public interface Concept extends Termed {

    @NotNull Bag<Task> tasklinks();

    @NotNull Bag<Term> termlinks();

    @Nullable Map<Object, Object> meta();

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
            Object v;
            put(key, v = value.apply(key, null));
            return (C) v;
        } else {
            return (C) meta.compute(key, value);
        }
    }


    default String termString() {
        return term().toString();
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
        synchronized (m) {
            return (C) m.remove(key);
        }

    }


    @NotNull BeliefTable beliefs();

    @NotNull BeliefTable goals();

    @NotNull QuestionTable questions();

    @Nullable QuestionTable quests();


    @Nullable
    default Map metaOrCreate() {
        Map<Object, Object> m = meta();
        if (m == null) {
            setMeta(m = new UnifiedMap(1));
            //new WeakIdentityHashMap();
            //new SoftValueHashMap(1));
        }
        return m;
    }

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

    default void linkCapacity(int termlinks, int tasklinks) {
        termlinks().setCapacity(termlinks);
        tasklinks().setCapacity(tasklinks);
    }

    void delete(NAR nar);


    static void delete(@NotNull Concept c, @NotNull NAR nar) {
        List<Task> removed = $.newArrayList();
        c.state(ConceptState.Deleted, nar);
        nar.tasks.remove(removed);

        c.termlinks().clear();
        c.tasklinks().clear();
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


    /**
     * the value (if present in the meta table for the class key),
     * is a reference to an object preventing deletion and also
     * manages and takes responsibility for the remainder of
     * this concept's lifecycle.
     */
    interface Savior {
    }


    @Nullable
    default Truth belief(long when, long now, float dur) {
        return beliefs().truth(when, now, dur);
    }

    @Nullable
    default Truth goal(long when, long now, float dur) {
        return goals().truth(when, now, dur);
    }

    @Nullable
    default Truth belief(long now, float dur) {
        return beliefs().truth(now, dur);
    }

    @Nullable
    default Truth goal(long now, float dur) {
        return goals().truth(now, dur);
    }

    @Nullable
    default float goalConf(long now, float dur, float ifMissing) {
        Truth t = goals().truth(now, dur);
        return (t != null) ? t.conf() : ifMissing;
    }


    @Nullable
    default TaskTable tableFor(char punc) {
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

//    default @Nullable Task merge(@NotNull Task x, @NotNull Task y, long when, @NotNull NAR nar) {
//        long now = nar.time();
//        Truth truth = ((BeliefTable) tableFor(y.punc())).truth(when, now);
//        if (truth == null)
//            return null;
//        return Revision.answer(x, y, when, now, truth);
//    }


    //    /** link to a specific peer */
//    static <T> void linkPeer(@NotNull Bag<T> bag, @NotNull T x, @NotNull Budget b, float q) {
//        //@NotNull Bag<Termed> bag = termlinks();
//        BLink<? extends T> existing = bag.get(x);
//        if (existing == null)
//            return;
//
//        /*
//        Hebbian Learning:
//            deltaWeight = (input[fromNeuron] -
//                            output[toNeuron] * weight(fromNeuron,toNeuron)
//							* output[toNeuron] * this.learningRate);
//							*
//            deltaWeight = input[toNeuron] * output[toNeuron] * learningRate;
//            deltaWeight = (input - netInput) * output * this.learningRate; // is it right to use netInput here?
//			deltaWeight = input * desiredOutput * this.learningRate;
//         */
//
//
//        boolean init;
//        if (existing == null ) {
//            bag.put(x, b, q, null);
//            init = true;
//        } else {
//            init = false;
//        }
//
//        float bp = b.pri();
//        if (bp == bp /*!NaN */) {
//
//            final float learningRate = (bp * q) / bag.capacity();
//            //System.out.println(this + " activating " + x);
//            bag.forEach(tl -> {
//                //                if (active && init)
////                    return; //dont modify the newly inserted link
//
//                float p = tl.pri();
//                if (p!=p) //the link is currently deleted
//                    return;
//
//                boolean active = tl == existing;
//                float dp = (active ? learningRate : -learningRate);
//                tl.priAdd(dp);
//                //System.out.println(tl.toString2());
//            });
//
//        }
//
//    }

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


    default void forEachTask(boolean includeConceptBeliefs, boolean includeConceptQuestions, boolean includeConceptGoals, boolean includeConceptQuests, @NotNull Consumer<Task> each) {
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

            Consumer<BLink> printBagItem = b -> {
                try {
                    out.append(indent);
                    out.append(String.valueOf(b.get())).append(" ").append(b.toBudgetString());
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
                    beliefs().forEach(printTask);
                }
                out.append(" Questions:");
                if (questions().isEmpty()) out.append(" none").append('\n');
                else {
                    out.append('\n');
                    questions().forEach(printTask);
                }
            }

            if (showgoals) {
                out.append(" Goals:");
                if (goals().isEmpty()) out.append(" none").append('\n');
                else {
                    out.append('\n');
                    goals().forEach(printTask);
                }
                out.append(" Quests:");
                if (questions().isEmpty()) out.append(" none").append('\n');
                else {
                    out.append('\n');
                    quests().forEach(printTask);
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
    @Nullable ConceptState state(@NotNull ConceptState c, @NotNull NAR nar);

    default void commit() {
        tasklinks().commit();
        termlinks().commit();
    }

    default float beliefFreq(long time, float dur) {
        return beliefFreq(time, dur, Float.NaN);
    }

    default float beliefFreq(long time, float dur, float valueIfNonExistent) {
        return freq(time, dur, beliefs(), valueIfNonExistent);
    }

    default float goalFreq(long time, float dur) {
        return goalFreq(time, dur, Float.NaN);
    }

    default float goalFreq(long time, float dur, float valueIfNonExistent) {
        return freq(time, dur, goals(), valueIfNonExistent);
    }

    static float freq(long time, float dur, @NotNull BeliefTable table) {
        return freq(time, dur, table, Float.NaN);
    }

    static float freq(long time, float dur, @NotNull BeliefTable table, float valueIfNonExistent) {
        Truth t = table.truth(time, dur);
        return t != null ? t.freq() : valueIfNonExistent;
    }

    Activation process(@NotNull Task input, NAR nar);


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
