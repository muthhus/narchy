package nars.concept.table;

import com.google.common.collect.Iterators;
import nars.Global;
import nars.NAR;
import nars.bag.Table;
import nars.bag.impl.SortedTable;
import nars.budget.merge.BudgetMerge;
import nars.task.Task;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;


/**
 * Stores beliefs ranked in a sorted ArrayList, with strongest beliefs at lowest indexes (first iterated)
 */
public class DefaultBeliefTable implements BeliefTable {

    public static final String DUPLICATE_BELIEF_GOAL = "Duplicate Belief/Goal";

    @NotNull public final SortedTable<Task,Task> eternal;
    @NotNull public final TemporalBeliefTable temporal;
    @NotNull final Map<Task,Task> map;

    public static final BudgetMerge DuplicateMerge = BudgetMerge.max; //this should probably always be max otherwise incoming duplicates may decrease the existing priority

    public DefaultBeliefTable(int initialEternalCapacity, int initialTemporalCapacity) {

        Map<Task, Task> mp;
        this.map = mp =
            Global.newHashMap(initialEternalCapacity + initialTemporalCapacity);
            //new HashMap<>(1);

        /* Ranking by originality is a metric used to conserve original information in balance with confidence */
        eternal = new EternalTable(mp, initialEternalCapacity);
        temporal = new MicrosphereTemporalBeliefTable(mp, eternal, initialTemporalCapacity);
    }

    /** TODO this value can be cached per cycle (when,now) etc */
    @Nullable
    @Override
    public final Truth truth(long now, long when) {


        boolean hasEternal = !eternal.isEmpty();
        boolean hasTemporal = !temporal.isEmpty();

        if (hasTemporal) {
            Truth tt = temporal.truth(when);
            if (hasEternal) {
                //higher confidence
                Truth ee = topEternal().truth();
                if (ee == null)
                    return Truth.Null;
                return (tt == null || ee.conf() > tt.conf()) ? ee : tt;
            } else {
                return tt;
            }
        } else {
            return hasEternal ? topEternal().truth() : Truth.Null;

        }

    }
//    public float rankTemporalByOriginality(@NotNull Task b) {
//        return BeliefTable.rankTemporalByOriginality(b, lastUpdate);
//    }

    @NotNull
    @Override
    public Iterator<Task> iterator() {
        return Iterators.concat(
            eternal.iterator(),
            temporal.iterator()
        );
    }

    @Override
    public void forEach(@NotNull Consumer<? super Task> action) {
        eternal.forEach(action);
        temporal.forEach(action);
    }

    @Override
    public int size() {
        return map.size(); //eternal.size() + temporal.size();
    }

    @Override public boolean isEmpty() {
        return map.isEmpty(); // size()==0;
    }

    @Override
    public int capacity() {
        return eternal.capacity() + temporal.capacity();
    }

    @Override
    public void capacity(int eternals, int temporals) {
        eternal.setCapacity(eternals);
        temporal.setCapacity(temporals);
    }

    @Override
    public void remove(@NotNull Task belief) {
        Object removed = ((belief.isEternal()) ? eternal : temporal).remove(belief);
        assert(removed == belief);
        TaskTable.removeTask(belief, null);
    }

    @Override
    public void clear() {
        eternal.clear();
        temporal.clear();
    }



    @Nullable
    @Override
    public final Task topEternal() {
        return eternal.top();
    }

    @Nullable
    @Override
    public final Task topTemporal(long when, long now) {
        return temporal.top(when);
    }

    @Override
    public Task get(Task t) {
        return map.get(t);
    }

    @Nullable
    @Override
    public Task add(@NotNull Task input, @NotNull QuestionTable questions, @NotNull NAR nar) {

        /* if a duplicate exists, it will merge the incoming task and return true.
          otherwise false */
        Task existing = get(input);
        if (existing!=null) {
            if (existing!=input) {
                DuplicateMerge.merge(existing.budget(), input, 1f);
                input.delete(DUPLICATE_BELIEF_GOAL);
            }
            return null;
        }

        //Filter duplicates; return null if duplicate
        // (no link activation will propagate and TaskProcess event will not be triggered)
        Task result = (input.isEternal() ?
               addEternal(input, nar) :
               addTemporal(input, nar));

        if (result!=null) {
            questions.answer(result, nar);
        }

        return result;
    }



    private Task addEternal(@NotNull Task input, @NotNull NAR nar) {

        if (eternal.capacity() == 0)
            return input;


        @NotNull SortedTable<Task, Task> et = this.eternal;

        //Try forming a revision and if successful, inputs to NAR for subsequent cycle
        Task revised = ((EternalTable) et).tryRevision(input, nar);
        if (revised!=null)  {
            if(Global.DEBUG) {
                if (revised.isDeleted())
                    throw new RuntimeException("revised task is deleted");
                if (revised.equals(input)) // || BeliefTable.stronger(revised, input)==input) {
                    throw new RuntimeException("useless revision");
            }


            //SLOW REVISION:
            nar.input(revised); //will be processed on subsequent cycle

            //FAST REVISION: return the revision, but also attempt to insert the incoming task which caused it:
//            tryInsert(input, nar);
//            input = revised;
        }


        //AXIOMATIC/CONSTANT BELIEF/GOAL
        if (input.conf() >=1f && et.capacity()!=1 && (et.isEmpty()|| et.top().conf()<1f)) {
            //lock incoming 100% confidence belief/goal into a 1-item capacity table by itself, preventing further insertions or changes
            //1. clear the corresponding table, set capacity to one, and insert this task
            Consumer<Task> overridden = t -> {
                TaskTable.removeTask(t, "Overridden");
            };
            et.forEach(overridden);
            et.clear();
            et.setCapacity(1);

            //2. clear the other table, set capcity to zero preventing temporal tasks
            Table<Task, Task> otherTable = temporal;
            otherTable.forEach(overridden);
            otherTable.clear();
            otherTable.setCapacity(0);

            //NAR.logger.info("axiom: {}", input);

            et.put(input, input);

            return input;
        }

        //Finally try inserting this task.  If successful, it will be returned for link activation etc
        return insert(input, et) ? input : null;
    }

    @NotNull
    protected Task addTemporal(@NotNull Task input, @NotNull NAR nar) {

        input = temporal.ready(input, nar);
        if (input != null) {
            //inserting this task.  should be successful
            boolean ii = insert(input, temporal);
            assert (ii);
        }

        return input;

    }






    /** try to insert but dont delete the input task if it wasn't inserted (but delete a displaced if it was)
     *  returns true if it was inserted, false if not
     * */
    private static boolean insert(@NotNull Task incoming, @NotNull Table<Task, Task> table) {

        Task displaced = table.put(incoming, incoming);

        boolean inserted = (displaced == null) || (displaced != incoming);//!displaced.equals(t);

        if (displaced!=null && !displaced.isDeleted()) {
            TaskTable.removeTask(displaced,
                    "Displaced"
                    //"Displaced by " + incoming,
            );
        }

        return inserted;
    }

}



//    private Task insert(@NotNull Task t, @NotNull Memory memory) {
//        ArrayTable<Task, Task> table = tableFor(t);
//
//        Task existing = map.get(t);
//        if (existing!=null) {
//            existingMergeFunction.merge(existing.budget(), t.budget(), 1f);
//            return existing;
//        }
//
//        Task displaced = table.put(t,t);
//        if (displaced!=null) {
//            onBeliefRemoved(displaced, "Duplicate/Unbelievable/Undesirable", memory);
//        }
//
//        return t == displaced ? null: t;
//    }

//    @Nullable
//    public Task contains(Task incoming) {
//
//        Task existing = map.get(incoming);
//        if (existing!=null)  {
////            if (existing!=incoming) {
////                //existingMergeFunction.merge(existing.budget(), incoming.budget(), 1f);
////                //((MutableTask) existing).state(incoming.state()); //clear any state
////                onBeliefRemoved(incoming, "Duplicate", memory);
////            }
//            return existing;
//        }
//
//        return null;
//    }


//    static void checkForDeleted(@NotNull Task input, @NotNull ArrayTable<Task,Task> table) {
//        if (input.getDeleted())
//            throw new RuntimeException("Deleted task being added");
//
//        table.forEach((Task dt) -> {
////            if (dt == null)
////                throw new RuntimeException("wtf");
//            if (dt.getDeleted()) {
//                throw new RuntimeException(
//                        //System.err.println(
//                        "Deleted tasks should not be present in belief tables: " + dt);
//                //System.err.println(dt.getExplanation());
//                //remove(i);
//                //i--;
////
//            }
//        });
//    }



//    /** computes the truth/desire as an aggregate of projections of all
//     * beliefs to current time
//     */
//    public final float expectation(boolean positive, Memory memory) {
//
//        long time = memory.time();
//        int dur = memory.duration();
//
//        float[] d = { positive ?  0f : 1f };
//
//        Consumer<Task> rank = t -> {
//
//            float best = d[0];
//
//            //scale conf by relevance, not the expectation itself
//            float e =
//                    Truth.expectation(t.freq(), t.conf()) *
//                    BeliefTable.rankTemporalByConfidence( t, time, dur );
//
//
//            if ((positive && (e > best)) || (!positive && (e < best)))
//                d[0] = e;
//        };
//
//        ArrayTable<Task, Task> t = this.temporal, u = this.eternal;
//        if (!t.isEmpty()) {
//            t.forEach(rank);
//        }
//
//        if (!u.isEmpty()) {
//            u.forEach(rank);
//        }
//
//        float dd = d[0];
//        return dd;
//    }




//    final static class LambdaSortedIndex<T> extends ArraySortedIndex<T> {
//        private final FloatFunction<T> score;
//
//        public LambdaSortedIndex(int cap, FloatFunction<T> score) {
//            super(cap);
//            this.score = score;
//        }
//
//        @Override public float score(T b) {
//            return score.floatValueOf(b);
//        }
//    }


//    @Override
//    public final boolean tryAdd(Task input, Ranker r, Memory memory) {
//
//        if (Global.DEBUG) {
//            if (input.getDeleted())
//                throw new RuntimeException("Deleted task being added");
//            checkForDeleted();
//        }
//
//        float rankInput = r.rank(input);    // for the new isBelief
//
//        int siz = data.size();
//        boolean atCapacity = (capacity == siz);
//        Task[] tasks = getCachedNullTerminatedArray();
//
//        int i = 0;
//
//        for (Task b; null != (b = tasks[i++]); ) {
//
//            if (b.equals(input)) {
//                //these should be preventable earlier
//                if (b!=input) {
//                    onBeliefRemoved(input, "Duplicate", memory);
//                    if (Global.DEBUG) {
//                        checkForDeleted();
//                    }
//                }
//                return false;
//            }
//
//            float existingRank = r.rank(b, rankInput);
//
//            boolean inputGreater = !Float.isFinite(existingRank) || (rankInput > existingRank);
//            if (inputGreater) {
//                break; //item will be inserted at this index
//            }
//        }
//
//        i--; //-1 is correct since after the above for loop it will be 1 ahead
//
//
//        if (atCapacity) {
//            if (i == siz) {
//                //reached the end of the list and there is no room to add at the end
//                //here we cant remove it yet because it is needed for revision
//                return false;
//            } else {
//                Task removed = remove(siz - 1);
//                onBeliefRemoved(removed, "Forgotten", memory);
//            }
//        }
//
//        add(i, input);
//        return true;
//    }

//TODO provide a projected belief
//
//
//
//        //first create a projected
//
//
//        /*if (t.sentence == belief.sentence) {
//            return false;
//        }*/
//
//        if (belief.sentence.equalStamp(t.sentence, true, false, true)) {
////                if (task.getParentTask() != null && task.getParentTask().sentence.isJudgment()) {
////                    //task.budget.decPriority(0);    // duplicated task
////                }   // else: activated belief
//
//            getMemory().removed(belief, "Duplicated");
//            return false;
//        } else if (revisible(belief.sentence, t.sentence)) {
//            //final long now = getMemory().time();
//
////                if (nal.setTheNewStamp( //temporarily removed
////                /*
////                if (equalBases(first.getBase(), second.getBase())) {
////                return null;  // do not merge identical bases
////                }
////                 */
////                //        if (first.baseLength() > second.baseLength()) {
////                new Stamp(newStamp, oldStamp, memory.time()) // keep the order for projection
////                //        } else {
////                //            return new Stamp(second, first, time);
////                //        }
////                ) != null) {
//
//            //TaskSeed projectedBelief = t.projection(nal.memory, now, task.getOccurrenceTime());
//
//
//            //Task r = t.projection(nal.memory, now, newBelief.getOccurrenceTime());
//
//            //Truth r = t.projection(now, newBelief.getOccurrenceTime());
//                /*
//                if (projectedBelief.getOccurrenceTime()!=t.getOccurrenceTime()) {
//                }
//                */
//
//
//
//            Task revised = tryRevision(belief, t, false, nal);
//            if (revised != null) {
//                belief = revised;
//                nal.setCurrentBelief(revised);
//            }
//
//        }
//

//        if (!addToTable(belief, getBeliefs(), getMemory().param.conceptBeliefsMax.get(), Events.ConceptBeliefAdd.class, Events.ConceptBeliefRemove.class)) {
//            //wasnt added to table
//            getMemory().removed(belief, "Insufficient Rank"); //irrelevant
//            return false;
//        }
//    }

//    @Override
//    public Task addGoal(Task goal, Concept c) {
//        if (goal.equalStamp(t, true, true, false)) {
//            return false; // duplicate
//        }
//
//        if (revisible(goal.sentence, oldGoal)) {
//
//            //nal.setTheNewStamp(newStamp, oldStamp, memory.time());
//
//
//            //Truth projectedTruth = oldGoal.projection(now, task.getOccurrenceTime());
//                /*if (projectedGoal!=null)*/
//            {
//                // if (goal.after(oldGoal, nal.memory.param.duration.get())) { //no need to project the old goal, it will be projected if selected anyway now
//                // nal.singlePremiseTask(projectedGoal, task.budget);
//                //return;
//                // }
//                //nal.setCurrentBelief(projectedGoal);
//
//                Task revisedTask = tryRevision(goal, oldGoalT, false, nal);
//                if (revisedTask != null) { // it is revised, so there is a new task for which this function will be called
//                    goal = revisedTask;
//                    //return true; // with higher/lower desire
//                } //it is not allowed to go on directly due to decision making https://groups.google.com/forum/#!topic/open-nars/lQD0no2ovx4
//
//                //nal.setCurrentBelief(revisedTask);
//            }
//        }
//    }


//    public static float rankBeliefConfidence(final Sentence judg) {
//        return judg.getTruth().getConfidence();
//    }
//
//    public static float rankBeliefOriginal(final Sentence judg) {
//        final float confidence = judg.truth.getConfidence();
//        final float originality = judg.getOriginality();
//        return or(confidence, originality);
//    }


//    boolean addToTable(final Task goalOrJudgment, final List<Task> table, final int max, final Class eventAdd, final Class eventRemove, Concept c) {
//        int preSize = table.size();
//
//        final Memory m = c.getMemory();
//
//        Task removed = addToTable(goalOrJudgment, table, max, c);
//
//        if (size()!=preSize)
//            c.onTableUpdated(goalOrJudgment.getPunctuation(), preSize);
//
//        if (removed != null) {
//            if (removed == goalOrJudgment) return false;
//
//            m.emit(eventRemove, this, removed.sentence, goalOrJudgment.sentence);
//
//            if (preSize != table.size()) {
//                m.emit(eventAdd, this, goalOrJudgment.sentence);
//            }
//        }
//
//        return true;
//    }


//    /**
//     * Select a belief to interact with the given task in logic
//     * <p/>
//     * get the first qualified one
//     * <p/>
//     * only called in RuleTables.rule
//     *
//     * @return The selected isBelief
//     */
////    @Override
//    public Task match(final Task task, long now) {
//        if (isEmpty()) return null;
//
//        long occurrenceTime = task.getOccurrenceTime();
//
//        final int b = size();
//
//        if (task.isEternal()) {
//            Task eternal = top(true, false);
//
//        }
//        else {
//
//        }
//
//        for (final Task belief : this) {
//
//            //if (task.sentence.isEternal() && belief.isEternal()) return belief;
//
//
//            return belief;
//        }
//
//
//        Task projectedBelief = belief.projectTask(occurrenceTime, now);
//
//        //TODO detect this condition before constructing Task
//        if (projectedBelief.getOccurrenceTime()!=belief.getOccurrenceTime()) {
//            //belief = nal.derive(projectedBelief); // return the first satisfying belief
//            return projectedBelief;
//        }
//
//        return null;
//    }

//    @Override
//    public Task project(Task t, long now) {
//        Task closest = topRanked();
//        if (closest == null) return null;
//        return closest.projectTask(t.getOccurrenceTime(), now);
//    }
