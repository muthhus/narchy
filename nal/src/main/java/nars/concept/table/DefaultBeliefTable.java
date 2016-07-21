package nars.concept.table;

import com.google.common.collect.Iterators;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.task.AnswerTask;
import nars.task.GeneratedTask;
import nars.task.Task;
import nars.truth.Truth;
import nars.truth.TruthFunctions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import static nars.nal.Tense.ETERNAL;


/**
 * Stores beliefs ranked in a sorted ArrayList, with strongest beliefs at lowest indexes (first iterated)
 */
public class DefaultBeliefTable implements BeliefTable {

    @NotNull public final EternalTable eternal;
    @NotNull public final TemporalBeliefTable temporal;



    public DefaultBeliefTable(int initialEternalCapacity, int initialTemporalCapacity) {



        /* Ranking by originality is a metric used to conserve original information in balance with confidence */
        eternal = new EternalTable(initialEternalCapacity);
        temporal = newTemporalBeliefTable(initialTemporalCapacity);
    }

    @NotNull
    protected static TemporalBeliefTable newTemporalBeliefTable(int initialTemporalCapacity) {
        return new MicrosphereTemporalBeliefTable(initialTemporalCapacity);
    }

    /** TODO this value can be cached per cycle (when,now) etc */
    @Override
    public final Truth truth(long when, long now) {

        final Truth ee;
        synchronized (eternal) {
            ee = eternal.truth();
        }

        final Truth tt;
        synchronized (temporal) {
            tt = temporal.truth(when, now, eternal);
        }

        if (tt!=null) {
            if (ee != null) {
                return (ee.conf() > tt.conf()) ? ee : tt;
            } else {
                return tt;
            }
        } else {
            return ee!=null ? ee : null;
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
        return eternal.size() + temporal.size();
    }

    @Override public boolean isEmpty() {
        return size()==0;
    }

    @Override
    public int capacity() {
        return eternal.capacity() + temporal.capacity();
    }

    @Override
    public void capacity(int eternals, int temporals) {
        eternal.capacity(eternals);
        temporal.capacity(temporals);
    }

    @Override
    public void remove(@NotNull Task belief, List<Task> displ) {
        ((belief.isEternal()) ? eternal : temporal).remove(belief, displ);
    }

    @Override
    public void clear() {
        eternal.clear();
        temporal.clear();
    }

    /** calculates the max confidence of a belief within the given frequency range */
    public float confMax(float minFreq, float maxFreq) {
        float max = 0;

        //HACK eternal top task may not hold the highest confidence (since rank involves originality) however we'll use that value here
        if (!eternal.isEmpty()) {
            Task s = eternal.strongest();
            float f = s.freq();
            if ((f >= minFreq) && (f <= maxFreq)) {
                max = s.conf();
            }
        }

        List<Task> temporals = ((MicrosphereTemporalBeliefTable)temporal);
        for (int i = 0, temporalsSize = temporals.size(); i < temporalsSize; i++) {
            Task t = temporals.get(i);
            if (t != null) {
                float f = t.freq();
                if ((f >= minFreq) && (f <= maxFreq)) {
                    float c = t.conf();
                    if (c > max)
                        max = c;
                }
            }
        }

        return max;
    }


    @Nullable
    @Override
    public final Task topEternal() {
        EternalTable ee = eternal;
        if (!ee.isEmpty()) {
            synchronized (eternal) {
                return ee.strongest();
            }
        }
        return null;
    }

    @Override
    public final Task topTemporal(long when, long now, Task against) {
        TemporalBeliefTable tt = temporal;
        if (!tt.isEmpty()) {
            synchronized (temporal) {
                return tt.strongest(when, now, against);
            }
        }
        return null;
    }


    @Override public Task add(@NotNull Task input, @NotNull QuestionTable questions, List<Task> displaced, @NotNull NAR nar) {


        //Filter duplicates; return null if duplicate
        // (no link activation will propagate and TaskProcess event will not be triggered)
        Task result;
        if (input.isEternal()) {
            synchronized (eternal) {
                result = addEternal(input, displaced, nar);
            }
        } else {
            synchronized (temporal) {

                result = temporal.add(input, eternal, displaced, nar);

                float eternalizationFactor = Param.ETERNALIZE_FORGOTTEN_TEMPORAL_TASKS;
                if (eternalizationFactor > 0f && displaced.size() > 0 && eternal.capacity() > 0) {
                    eternalizeForgottenTemporals(displaced, nar, eternalizationFactor);
                }
            }
        }

        if (result!=null) {
            questions.answer(result, nar, displaced);
        }

        return result;
    }

    protected void eternalizeForgottenTemporals(List<Task> displaced, @NotNull NAR nar, float factor) {
        float confMin = nar.confMin.floatValue();

        @NotNull EternalTable eternal = this.eternal;

        float minRank = eternal.isFull() ? eternal.minRank() : 0;

        int displacedSize = displaced.size();

        //should use indexed list access because adding eternal might add new eternal tasks at the end (which should not be processed here
        for (int i = 0; i < displacedSize; i++) {
            Task d = displaced.get(i);

            assert(d.occurrence()!=ETERNAL);

            if (!d.isDeleted()) {
                float eConf = TruthFunctions.eternalize(d.conf()) * factor;
                if (eConf > confMin) {
                    if (eternal.rank(eConf, d.evidence().length) > minRank) {

                        Task ee = new GeneratedTask(
                                d.term(), d.punc(),
                                $.t(d.freq(), eConf)
                            )
                                .time(nar.time(), ETERNAL)
                                .evidence(d)
                                .budget(d.budget())
                                .log("Eternalized");

                        Task ff = addEternal(ee, displaced, nar);
                        if (ff == null) {
                            throw new RuntimeException("eternal rejected " + ee + " but this could have been prevented before constructing and inserting it");
                        } else {
                            if (d.term().toString().equals("I(a0)")) {
                                System.out.println(eternal.size() + " / " + eternal.capacity());
                                System.out.println(temporal.size() + " / " + temporal.capacity());
                                System.out.println("eternalize: " + d + "\n\t" + ee + "\n\t\t" + ff);
                            }
                        }
                    }

                }
            }
        }
    }

    private Task addEternal(@NotNull Task input, List<Task> displaced, @NotNull NAR nar) {

        @NotNull EternalTable et = this.eternal;

        int cap = et.capacity();
        if (cap == 0) {
            if (input.isInput())
                throw new RuntimeException("input task rejected (0 capacity): " + input);
            return null;
        }
        else if ((input.conf() >= 1f) && (cap != 1) && (et.isEmpty() || (et.first().conf() < 1f))) {
            //AXIOMATIC/CONSTANT BELIEF/GOAL
            addEternalAxiom(input, et, displaced);
            return input;
        }


        //Try forming a revision and if successful, inputs to NAR for subsequent cycle
        Task revised;
        if (!(input instanceof AnswerTask)) {
            revised = et.tryRevision(input, nar);
            if (revised != null) {

                try {
                    revised = revised.normalize(nar); //may throw an exception
                } catch (NAR.InvalidTaskException e) {
                    e.printStackTrace();
                    revised = null;
                }

                if (Param.DEBUG) {

                    if (revised.isDeleted())
                        throw new RuntimeException("revised task is deleted");
                    if (revised.equals(input)) // || BeliefTable.stronger(revised, input)==input) {
                        throw new RuntimeException("useless revision");
                }
            }
        } else {
            revised = null;
        }


        //Finally try inserting this task.  If successful, it will be returned for link activation etc
        Task result = insert(input, displaced) ? input : null;
        if (revised!=null) {

            revised = insert(revised, displaced) ? revised : null;

            if (result == null)
                result = revised;

            //result = insert(revised, et) ? revised : result;
//            nar.runLater(() -> {
//                if (!revised.isDeleted())
//                    nar.input(revised);
//            });
        }
        return result;
    }

    private void addEternalAxiom(@NotNull Task input, @NotNull EternalTable et, List<Task> displ) {
        //lock incoming 100% confidence belief/goal into a 1-item capacity table by itself, preventing further insertions or changes
        //1. clear the corresponding table, set capacity to one, and insert this task
        Consumer<Task> overridden = t -> TaskTable.removeTask(t, "Overridden", displ);
        et.forEach(overridden);
        et.clear();
        et.capacity(1);

        //2. clear the other table, set capcity to zero preventing temporal tasks
        TemporalBeliefTable otherTable = temporal;
        otherTable.forEach(overridden);
        otherTable.clear();
        otherTable.capacity(0);

        //NAR.logger.info("axiom: {}", input);

        et.put(input);
    }


//


    /** try to insert but dont delete the input task if it wasn't inserted (but delete a displaced if it was)
     *  returns true if it was inserted, false if not
     * */
    private boolean insert(@NotNull Task incoming, List<Task> displ) {

        Task displaced = eternal.put(incoming);

        if (displaced!=null && !displaced.isDeleted()) {
            TaskTable.removeTask(displaced,
                    "Displaced", displ
                    //"Displaced by " + incoming,
            );
        }

        boolean inserted = (displaced == null) || (displaced != incoming);//!displaced.equals(t);
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
