package nars.concept.util;

import com.google.common.collect.Iterators;
import nars.Global;
import nars.Memory;
import nars.NAR;
import nars.bag.impl.ArrayTable;
import nars.budget.BudgetFunctions;
import nars.budget.BudgetMerge;
import nars.nal.LocalRules;
import nars.nal.Tense;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Termed;
import nars.term.Terms;
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
public class ArrayBeliefTable implements BeliefTable {

    @NotNull final ArrayTable<Task,Task> eternal;
    @NotNull final ArrayTable<Task,Task> temporal;
    @NotNull final Map<Task,Task> map;

    public static final BudgetMerge DuplicateMerge = BudgetMerge.plusDQBlend;

    private long lastUpdate; //cached value, updated before temporal operations begin
    private long minT, maxT;

    float ageFactor;

    public ArrayBeliefTable(int cap, @NotNull Memory memory) {
        super();

        if (cap == 1) cap = 2;

        this.map =
                Global.newHashMap(0);
                //new HashMap(cap);

        this.minT = this.maxT = this.lastUpdate = memory.time();
        this.ageFactor = 1f/(memory.duration()*2f);

        /** Ranking by originality is a metric used to conserve original information in balance with confidence */
        eternal = new SetTable<>(map, new ArraySortedIndex<Task>(cap) {
            @Override
            public float score(@NotNull Task v) {
                return BeliefTable.rankEternalByOriginality(v);
            }
        });
        temporal = new SetTable<>(map, new ArraySortedIndex<Task>(cap) {
            @Override
            public float score(@NotNull Task v) {
                return rankTemporalByOriginality(v);
            }
        });

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

    void updateTimeRange() {
        if (temporal.isEmpty()) {
            minT = maxT = lastUpdate;
        } else {
            minT = Long.MAX_VALUE;
            maxT = Long.MIN_VALUE;
            List<Task> list = temporal.items.list();
            for (int i = 0, listSize = list.size(); i < listSize; i++) {
                long o = list.get(i).occurrence();
                if (o > maxT) maxT = o;
                if (o < minT) minT = o;
            }
        }

        //ageFactor = (minT!=maxT)? 1f/(maxT-minT) : 0;
    }


    public float rankTemporalByOriginality(@NotNull Task b) {
        return rankTemporalByOriginality(b, lastUpdate);
    }
    public float rankTemporalByOriginality(@NotNull Task b, long when) {
        return BeliefTable.rankEternalByOriginality(b) *
                BeliefTable.relevance(b, when, ageFactor);

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
    public final Task topTemporal(long when, long now) {
        Task best = null;
        float bestRank = -1;
        List<? extends Task> l = temporal.items.list();

        int ls = l.size();
        for (int i = 0; i < ls; i++) {
            Task x = l.get(i);
            float r = BeliefTable.rankTemporalByConfidence(x, when, ageFactor);
            if (r > bestRank) {
                best = x;
                bestRank = r;
            }
        }

        if (best!=null) {//if (project) {
            best = best.projectTask(when, now);
        }

        return best;
    }



    @Nullable
    @Override
    public Task add(@NotNull Task input, @NotNull NAR nar) {

        //Filter duplicates; return null if duplicate
        // (no link activation will propagate and TaskProcess event will not be triggered)
        if (filterDuplicate(input, nar))
            return null;

        //Try forming a revision and if successful, inputs to NAR for subsequent cycle
        tryRevision(input, nar);

        //Finally try inserting this task.  If successful, it will be returned for link activation etc
        return tryInsert(input, nar) ? input : null;
    }

    /** if a duplicate exists, it will merge the incoming task and return true.
     * otherwise false */
    private boolean filterDuplicate(@NotNull Task input, @NotNull NAR nar) {
        Task existing = contains(input);
        if (existing!=null) {
            if (existing!=input) {
                //Average allows duplicate tasks to not explode like plus would
                DuplicateMerge.merge(existing.budget(), input.budget(), 1f);
                //((MutableTask) existing).state(input.state()); //reset execution / anticipated state
                nar.remove(input, "Duplicate Belief/Goal");
            }
            return true;
        }
        return false;
    }

    private void tryRevision(@NotNull Task input, @NotNull NAR nar) {
        Task revised = getRevision(input, nar);
        if (revised!=null && !revised.isDeleted())  {
            if(Global.DEBUG) {
                if (revised.equals(input)) // || BeliefTable.stronger(revised, input)==input) {
                    throw new RuntimeException("useless revision: " + revised);
            }
            //if (BeliefTable.stronger(revised, input)==revised) {
            nar.input(revised); //will be processed on subsequent cycle
            //}
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

    /**
     * creates a revision task (but does not input it)
     * if failed, returns null
     */
    @Nullable
    public Task getRevision(@NotNull Task newBelief, @NotNull NAR nar) {
        long now = nar.time();

        List<Task> beliefs = tableFor(newBelief).items.list();
        int bsize = beliefs.size();
        if (bsize == 0)
            return null; //nothing to revise with

        Compound newBeliefTerm = newBelief.term();
        //long newBeliefOcc = newBelief.occurrence();
        //float newBeliefConf = newBelief.conf();

        //best found
        Task oldBelief = null;
        float bestRank = 0, bestConf = 0;
        Truth conclusion = null;
        long concTime = Tense.ETERNAL;

        for (int i = 0; i < bsize; i++) {
            Task x = beliefs.get(i);
            if (x.isDeleted() || !LocalRules.isRevisible(newBelief, x)) continue;

            float matchFactor = Terms.termRelevance(newBeliefTerm, x.term());
            if (matchFactor <= 0) continue;

//
//            float factor = tRel * freqMatch;
//            if (factor < best) {
//                //even with conf=1.0 it wouldnt be enough to exceed existing best match
//                continue;
//            }

            final int totalEvidence = 1; //newBelief.evidence().length + x.evidence().length;
            float minValidConf = Math.min(newBelief.conf(), x.conf());
            if (minValidConf < bestConf) continue;
            float minValidRank = BeliefTable.rankEternalByOriginality(minValidConf, totalEvidence);
            if (minValidRank < bestRank) continue;

            Truth c;
            long t;
            if (newBelief.isEternal()) {
                c = TruthFunctions.revision(newBelief.truth(), x.truth(), matchFactor, minValidConf);
                if (c == null)
                    continue;
                t = Tense.ETERNAL;
            } else {
                c = TruthFunctions.revision(
                        newBelief,
                        x, now, matchFactor, minValidConf);
                if (c == null)
                    continue;
                t = now; //Math.max(newBelief.occurrence(), x.occurrence());
            }

            //TODO avoid allocating Truth's here

            //float ffreqMatch = 1f/(1f + Math.abs(newBeliefFreq - x.freq()));

            float cconf = c.conf();
            float rank = BeliefTable.rankEternalByOriginality(cconf, totalEvidence);

            if ((cconf > 0) && (rank > bestRank)) {
                bestRank = rank;
                bestConf = cconf;
                oldBelief = x;
                conclusion = c;
                concTime = t;
            }
        }

        if (oldBelief == null)
            return null; //nothing matches

        if (conclusion.equals(newBelief.truth()) && concTime == newBelief.occurrence())
            return null; //equal

//        Truth newBeliefTruth = newBelief.truth();
//        Truth oldBeliefTruth = oldBelief.projection(newBeliefOcc, now);

//        Truth conclusion = TruthFunctions.revision(
//                newBelief,
//                oldBelief, now);
//        conclusion.setConfidence( conclusion.conf() * termRelevance );


        Termed<Compound> term = LocalRules.intermpolate(newBelief, oldBelief, newBelief.conf(), oldBelief.conf());

        MutableTask t = new MutableTask(term, newBelief.punc())
                .truth(conclusion)
                .parent(newBelief, oldBelief)
                .time(now, concTime)
                //.state(newBelief.state())
                .because("Insertion Revision");
                /*.because("Insertion Revision (%+" +
                                Texts.n2(conclusion.freq() - newBelief.freq()) +
                        ";+" + Texts.n2(conclusion.conf() - newBelief.conf()) + "%");*/


        BudgetFunctions.budgetRevision(t, newBelief, oldBelief);

        if (!BudgetFunctions.valid(t.budget(), nar))
            return null;

        return oldBelief.onRevision(t) ? t : null;
    }

    private Task contains(Task incoming) {

        Task existing = map.get(incoming);
        if (existing!=null)  {
//            if (existing!=incoming) {
//                //existingMergeFunction.merge(existing.budget(), incoming.budget(), 1f);
//                //((MutableTask) existing).state(incoming.state()); //clear any state
//                onBeliefRemoved(incoming, "Duplicate", memory);
//            }
            return existing;
        }

        return null;
    }



    /** try to insert but dont delete the input task if it wasn't inserted (but delete a displaced if it was)
     *  returns true if it was inserted, false if not
     * */
    private boolean tryInsert(@NotNull Task incoming, @NotNull NAR nar) {

        this.lastUpdate = nar.time();

        ArrayTable<Task, Task> table = tableFor(incoming);


        //AXIOMATIC/CONSTANT BELIEF/GOAL
        if (incoming.conf() >=1f && table.capacity()!=1 && (table.isEmpty()||table.top().conf()<1f)) {
            //lock incoming 100% confidence belief/goal into a 1-item capacity table by itself, preventing further insertions or changes
            //1. clear the corresponding table, set capacity to one, and insert this task
            Consumer<Task> overridden = t -> {
                onBeliefRemoved(t, "Overridden", nar);
            };
            table.forEach(overridden);
            table.clear();
            table.setCapacity(1);
            table.put(incoming, incoming);

            //2. clear the other table, set capcity to zero preventing temporal tasks
            table = (table == eternal) ? temporal : eternal;
            table.forEach(overridden);
            table.clear();
            table.setCapacity(0);
            return true;
        }

        Task displaced = table.put(incoming, incoming);

        boolean inserted = displaced == null || displaced!=incoming;//!displaced.equals(t);
//        if (displaced!=null && inserted) {
//
//        }
        if (displaced!=null && !displaced.isDeleted()) {
            onBeliefRemoved(displaced,
                    "Displaced",
                    //"Displaced by " + incoming,
                    nar);
        }

        if (inserted && !incoming.isEternal())
            updateTimeRange();

        return inserted;
    }

    @NotNull
    private ArrayTable<Task, Task> tableFor(@NotNull Task t) {
        return t.isEternal() ? this.eternal : this.temporal;
    }

    private static void onBeliefRemoved(@NotNull Task t, @Nullable String reason, @NotNull Memory memory) {
        memory.remove(t, reason);
    }

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

    final static class SetTable<T> extends ArrayTable<T,T> {
        public SetTable(Map<T, T> index, ArraySortedIndex<T> items) {
            super(items, index);
        }

        @Override
        public T key(T t) {
            return t;
        }

    }



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
}
