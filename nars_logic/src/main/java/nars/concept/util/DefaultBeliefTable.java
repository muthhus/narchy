package nars.concept.util;

import com.google.common.collect.Iterators;
import com.gs.collections.api.block.function.primitive.FloatFunction;
import nars.Global;
import nars.Memory;
import nars.NAR;
import nars.bag.impl.ArrayTable;
import nars.budget.Budget;
import nars.budget.BudgetFunctions;
import nars.budget.BudgetMerge;
import nars.nal.LocalRules;
import nars.nal.Tense;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Termed;
import nars.term.Terms;
import nars.term.compound.Compound;
import nars.truth.Truth;
import nars.truth.TruthFunctions;
import nars.util.ArraySortedIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;



/**
 * Stores beliefs ranked in a sorted ArrayList, with strongest beliefs at lowest indexes (first iterated)
 */
public class DefaultBeliefTable implements BeliefTable {

    final static BudgetMerge existingMergeFunction = BudgetMerge.max;

    @NotNull
    final Map<Task,Task> map;
    @NotNull
    final ArrayTable<Task,Task> eternal;
    @NotNull
    final ArrayTable<Task,Task> temporal;

    private long now; //cached value, updated before temporal operations begin
    private long minT, maxT;
    float duration;

    public DefaultBeliefTable(int cap, @NotNull Memory memory) {
        super();

        if (cap == 1) cap = 2;

        this.map = Global.newHashMap(cap/4); //new HashMap(cap/4);
        this.now = memory.time();
        this.minT = this.maxT = Tense.TIMELESS;
        this.duration = memory.duration.floatValue();

        eternal = new SetTable<Task>(cap/2, map,
            this::rankEternal
        );
        temporal = new SetTable<Task>(cap/2, map,
            this::rankTemporal
        );
    }

    public float rankEternal(@NotNull Task b) {
        return BeliefTable.rankEternal(b, now, duration);
    }


    public long getMinT() {
        return minT;
    }

    public long getMaxT() {
        return maxT;
    }

//    @Override
//    public boolean remove(@NotNull Task w) {
//        if (w.isEternal()) {
//            return eternal.remove(w)!=null;
//        } else {
//            if (temporal.remove(w)!=null) {
//                updateTime(now, true);
//                return true;
//            }
//            return false;
//        }
//    }

    void updateTime(long now, boolean updateRange) {
        this.now = now;
        if (updateRange) {
            if (temporal.isEmpty()) {
                minT = maxT = Tense.TIMELESS;
            }
            minT = Long.MAX_VALUE;
            maxT = Long.MIN_VALUE;
            for (Task x : temporal) {
                long o = x.occurrence();
                if (o > maxT) maxT = o;
                if (o < minT) minT = o;
            }
            //tRange = Math.max(1, maxT - minT);

        }
    }


    public float rankTemporal(@NotNull Task b) {
        return rankTemporal(b, now);
    }
    public float rankTemporal(@NotNull Task b, long when) {
        return BeliefTable.rankTemporal(b, when, duration);
    }


    @NotNull
    @Override
    public Iterator<Task> iterator() {
        return Iterators.concat(eternal.items.iterator(), temporal.items.iterator());
    }

    @Override
    public void forEach(@NotNull Consumer<? super Task> action) {
        eternal.forEach(action);
        temporal.forEach(action);
    }

    @Override
    public void setCapacity(int newCapacity) {
        if (newCapacity == 1) newCapacity = 2; //prevent 0 by accident
        eternal.setCapacity(newCapacity/2);
        temporal.setCapacity(newCapacity/2);
    }

    @Override
    public int size() {
        return map.size(); //eternal.size() + temporal.size();
    }

    @Override public boolean isEmpty() {
        return map.isEmpty(); // size()==0;
    }

    @Override
    public int getCapacity() {
        return eternal.capacity() + temporal.capacity();
    }

    //    @Override
//    public Task top(boolean hasQueryVar, long now, long occTime, Truth truth) {
//        throw new RuntimeException("not supposed to be called");
//    }


    @Override
    public void clear() {
        eternal.items.clear();
        temporal.items.clear();
        map.clear();
    }

    @Nullable
    @Override
    public final Task topEternal() {
        return eternal.top();
    }

    @Nullable
    @Override
    public final Task topTemporal(long when) {
        Task best = null;
        float bestRank = -1;
        List<? extends Task> l = temporal.items.getList();
        int ls = l.size();
        for (int i = 0; i < ls; i++) {
            Task x = l.get(i);
            float r = rankTemporal(x, when);
            if (r > bestRank) {
                best = x;
                bestRank = r;
            }
        }
        return best;
    }


//    @Deprecated @Override
//    public final Task top(Ranker r) {
//
//        Task[] tasks = getCachedNullTerminatedArray();
//        //if (tasks == null) return null;
//
//
//        float s = Float.NEGATIVE_INFINITY;
//        Task b = null;
//
//        for (int i = tasks.length - 1; i >= 0; i--) {
//            Task t = tasks[i];
//            if (t != null) {
//                float x = r.rank(t, s);
//                if (x + DefaultTruth.DEFAULT_TRUTH_EPSILON > s) {
//                    s = x;
//                    b = t;
//                }
//            }
//        }
//
////        Task t;
////        for (int i = 0; null != (t = tasks[i++]); ) {
////            float x = r.rank(t, s);
////            if (x > s) {
////                s = x;
////                b = t;
////            }
////        }
//
//        return b;
//    }


    /**
     * merges an input task with this belief table.
     * ordinarily this should never return null.
     * it will return the best matching old or new (input or
     * revised here) belief corresponding to the input.
     * <p>
     * the input will either be added or not depending
     * on its relation to the table's contents.
     *
     * if the new task is rejected, it will be deleted. callee must check
     * for this condition
     */
    @NotNull
    @Override
    public Task add(@NotNull Task input, @NotNull NAR nar) {

        long now = this.now = nar.time();

        Task revised = getRevision(input, now);
        if ((revised!=null) && (!revised.equals(input))) {
            //if (BeliefTable.stronger(revised, input)==revised) {
                nar.input(revised);
            //}
        }

        //else input = revised;

        //boolean insertedInput = tryInsert(input, memory);

        //if ((revised != null) && !revised.equals(input)) {

            //if (insertedInput)
        input = tryInsert(input, nar.memory);

        updateTime(now, !input.isEternal());

            //input = revised;
        //}

        //return input;
        return revised!=null ? revised : input;
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

    /**
     * creates a revision task (but does not input it)
     * if failed, returns null
     */
    public Task getRevision(@NotNull Task newBelief, long now) {

        List<Task> beliefs = tableFor(newBelief).items.getList();
        int bsize = beliefs.size();
        if (bsize == 0)
            return null; //nothing to revise with

        Compound newBeliefTerm = newBelief.term();
        float newBeliefFreq = newBelief.freq();
        long newBeliefOcc = newBelief.occurrence();

        //best found
        Task oldBelief = null;
        float best = 0;
        Truth conclusion = null;

        for (int i = 0; i < bsize; i++) {
            Task x = beliefs.get(i);
            if (!LocalRules.isRevisible(newBelief, x)) continue;

            float tRel = Terms.termRelevance(newBeliefTerm, x.term());
            if (tRel <= 0) continue;


//
//            float factor = tRel * freqMatch;
//            if (factor < best) {
//                //even with conf=1.0 it wouldnt be enough to exceed existing best match
//                continue;
//            }

            Truth c;
            if (newBelief.isEternal()) {
                c = TruthFunctions.revision(newBelief.truth(), x.truth());
            } else {
                c = TruthFunctions.revision(
                        newBelief,
                        x, now);
            }

            //float ffreqMatch = 1f/(1f + Math.abs(newBeliefFreq - x.freq()));
            float ffreqMatch = 1;
            c = c.withConfMult(tRel * ffreqMatch);

            float cc = c.conf();
            if (cc > best) {
                best = cc;
                oldBelief = x;
                conclusion = c;
            }
        }

        if (oldBelief == null) return null; //nothing matches


//        Truth newBeliefTruth = newBelief.truth();
//        Truth oldBeliefTruth = oldBelief.projection(newBeliefOcc, now);

//        Truth conclusion = TruthFunctions.revision(
//                newBelief,
//                oldBelief, now);
//        conclusion.setConfidence( conclusion.conf() * termRelevance );


        Termed<Compound> term = LocalRules.intermpolate(newBelief.concept(), oldBelief.concept(), newBelief.conf(), oldBelief.conf());

        MutableTask t = new MutableTask(term, newBelief.punc())
                .truth(conclusion)
                .parent(newBelief, oldBelief)
                .time(now, newBeliefOcc)
                .state(newBelief.state())
                .because("Revision");

        Budget bb = t.budget();
        BudgetFunctions.revise(bb, newBelief.truth(), oldBelief, conclusion, newBelief.budget());

        if (bb.isDeleted())
            throw new RuntimeException("deleted revision");

        return t;
    }

    /** try to insert but dont delete the input task if it wasn't inserted (but delete a displaced if it was)
     *  returns true if it was inserted, false if not
     * */
    private Task tryInsert(@NotNull Task incoming, @NotNull Memory memory) {


        ArrayTable<Task, Task> table = tableFor(incoming);

//        Task existing = map.get(incoming);
//        if (existing!=null)  {
//            if (existing!=incoming) {
//                existingMergeFunction.merge(existing.budget(), incoming.budget(), 1f);
//                ((MutableTask) existing).state(incoming.state()); //clear any state
//                onBeliefRemoved(incoming, "Duplicate", memory);
//            }
//            return existing;
//        }

        Task displaced = table.put(incoming,incoming);
        boolean inserted = displaced == null || displaced!=incoming;//!displaced.equals(t);
//        if (displaced!=null && inserted) {
//            onBeliefRemoved(displaced,
//                    "Displaced",
//                    //"Displaced by " + incoming,
//                    memory);
//        }

        return incoming;
    }

    @NotNull
    private ArrayTable<Task, Task> tableFor(@NotNull Task t) {
        return t.isEternal() ? this.eternal : this.temporal;
    }




    private static void onBeliefRemoved(@NotNull Task t, String reason, @NotNull Memory memory) {
        memory.remove(t, reason);
    }

//    static void checkForDeleted(@NotNull Task input, @NotNull ArrayTable<Task,Task> table) {
//        if (input.getDeleted())
//            throw new RuntimeException("deleted task being added");
//
//        table.forEach((Task dt) -> {
////            if (dt == null)
////                throw new RuntimeException("wtf");
//            if (dt.getDeleted()) {
//                throw new RuntimeException(
//                        //System.err.println(
//                        "deleted tasks should not be present in belief tables: " + dt);
//                //System.err.println(dt.getExplanation());
//                //remove(i);
//                //i--;
////
//            }
//        });
//    }

    final static class SetTable<T> extends ArrayTable<T,T> {
        public SetTable(int cap, Map<T,T> index, FloatFunction<T> score) {
            super(new LambdaSortedIndex(cap, score), index);
        }

        @Override
        public T key(T t) {
            return t;
        }

    }

    final static class LambdaSortedIndex<T> extends ArraySortedIndex<T> {
        private final FloatFunction<T> score;

        public LambdaSortedIndex(int cap, FloatFunction<T> score) {
            super(cap);
            this.score = score;
        }

        @Override public float score(T b) {
            return score.floatValueOf(b);
        }
    }


//    @Override
//    public final boolean tryAdd(Task input, Ranker r, Memory memory) {
//
//        if (Global.DEBUG) {
//            if (input.getDeleted())
//                throw new RuntimeException("deleted task being added");
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
}
