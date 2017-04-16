package nars.table;

import jcog.data.sorted.SortedArray;
import nars.NAR;
import nars.Task;
import jcog.pri.PriMerge;
import nars.concept.TaskConcept;
import nars.task.Revision;
import nars.task.RevisionTask;
import nars.term.Compound;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;

import static nars.term.Terms.compoundOrNull;
import static nars.time.Tense.ETERNAL;


/**
 * Created by me on 5/7/16.
 */
public class EternalTable extends SortedArray<Task> implements TaskTable, FloatFunction<Task> {

    public static final EternalTable EMPTY = new EternalTable(0) {

        @Override
        public @Nullable Task put(@NotNull Task incoming) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeTask(Task x) {
            return false;
        }

        @Override
        public final int capacity() {
            //throw new UnsupportedOperationException();
            return 0;
        }

        @Override
        public void forEachTask(Consumer<? super Task> action) {

        }

        @Override
        public Iterator<Task> taskIterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return 0;
        }
    };

    int capacity;
    @Nullable
    private Truth truth;

    public EternalTable(int initialCapacity) {
        super(Task[]::new);
        this.capacity = initialCapacity;
    }

    public void capacity(int c) {
        if (this.capacity != c) {

            this.capacity = c;

            synchronized (this) {

                int s = size();

                //TODO can be accelerated by batch remove operation
                while (c < s--) {
                    Task r = removeWeakest();
                    r.delete();
                }
            }

        }
    }

    @Override
    public void forEachTask(Consumer<? super Task> x) {
        forEach(x);
    }


    //    @Override
//    public void forEach(Consumer<? super Task> action) {
//        //synchronized (this) {
//            super.forEach(action);
//        //}
//    }

    //    protected final Task removeWeakest(Object reason) {
//        if (!isEmpty()) {
//            Task x = remove(size() - 1);
//            x.delete(reason);
//        }
//    }


    public final Task match() {
        Object[] l = this.list;
        return (l.length == 0) ? null : (Task) l[0];
    }

    public final Task weakest() {
        Object[] l = this.list;
        if (l.length == 0) return null;
        int n = l.length - 1;
        Task w = null;
        while (n > 0 && (w = (Task) l[n]) != null) n--; //scan upwards for first non-null
        return w;

    }

    @Override
    public Iterator<Task> taskIterator() {
        return iterator();
    }

    public final float floatValueOf(@NotNull Task w) {
        //return rankEternalByConfAndOriginality(w);
        return -w.conf();
    }

//    public final float minRank() {
//        Task w = weakest();
//        return w == null ? 0 : rank(w);
//    }
//
//    @Override
//    public final int compare(@NotNull Task o1, @NotNull Task o2) {
//        float f1 = rank(o2); //reversed
//        float f2 = rank(o1);
//        if (f1 < f2)
//            return -1;
//        if (f1 > f2)
//            return 1;
//        return 0;
//    }

    @Deprecated
    static void removeTask(@NotNull Task t, @Nullable String reason) {
//        if (reason!=null && Param.DEBUG && t instanceof MutableTask)
//            ((MutableTask)t).log(reason);
        t.delete();
    }

    /**
     * @return null: no revision could be applied
     * ==newBelief: existing duplicate found
     * non-null: revised task
     */
    @Nullable
    private /*Revision*/Task tryRevision(@NotNull Task input, @NotNull TaskConcept concept, @NotNull NAR nar) {

        Object[] list = this.list;
        int bsize = list.length;
        if (bsize == 0)
            return null; //nothing to revise with


        //Try to select a best revision partner from existing beliefs:
        Task oldBelief = null;
        float bestRank = 0f, bestConf = 0f;
        Truth conclusion = null;

        Truth newBeliefTruth = input.truth();

        int dur = nar.dur();

        for (int i = 0; i < bsize; i++) {
            Task x = (Task) list[i];

            if (x == null) //the array has trailing nulls from having extra capacity
                break;

            if (x.equals(input)) {
                if (x != input)
                    PriMerge.maxBlend.apply(x, input.priority());
                return x;
            }

            if (!Revision.isRevisible(input, x))
                continue;


            //
            //            float factor = tRel * freqMatch;
            //            if (factor < best) {
            //                //even with conf=1.0 it wouldnt be enough to exceed existing best match
            //                continue;
            //            }

            //            float minValidConf = Math.min(newBeliefConf, x.conf());
            //            if (minValidConf < bestConf) continue;
            //            float minValidRank = BeliefTable.rankEternalByOriginality(minValidConf, totalEvidence);
            //            if (minValidRank < bestRank) continue;

            Truth oldBeliefTruth = x.truth();

            Truth c = Revision.revise(newBeliefTruth, oldBeliefTruth, 1f, bestConf);

            //avoid a weak or duplicate truth
            if (c == null || c.equals(oldBeliefTruth) || c.equals(newBeliefTruth))
                continue;

            float cconf = c.conf();
            final int totalEvidence = 1; //newBelief.evidence().length + x.evidence().length; //newBelief.evidence().length + x.evidence().length;
            float rank = rank(cconf, totalEvidence);

            if (rank > bestRank) {
                bestRank = rank;
                bestConf = cconf;
                oldBelief = x;
                conclusion = c;
            }
        }

        if (oldBelief == null) {
            return null;
        }


        final float newBeliefWeight = input.evi();
        float aProp = newBeliefWeight / (newBeliefWeight + oldBelief.evi());
        Compound t = compoundOrNull(Revision.intermpolate(
                input.term(), oldBelief.term(),
                aProp,
                nar.random,
                true
        ));
        if (t == null)
            return null;

        RevisionTask r = new RevisionTask(t,
                input, oldBelief,
                conclusion,
                nar.time(),
                ETERNAL, ETERNAL
        );
        r.budget(oldBelief, input);
        return r.log("Insertion Revision");
    }

    @Nullable
    public Task put(@NotNull final Task incoming) {
        Task displaced = null;

        if (size() == capacity()) {
            Task weakestPresent = weakest();
            if (weakestPresent != null) {
                if (floatValueOf(weakestPresent) <= floatValueOf(incoming)) {
                    displaced = removeWeakest();
                } else {
                    return incoming; //insufficient confidence
                }
            }
        }

        add(incoming, this);

        return displaced;
    }

    public final Truth truth() {
        Task s = match();
        return s != null ? s.truth() : null;
    }

    @Override
    public int capacity() {
        return capacity;
    }

//    @Override
//    public void remove(@NotNull Task belief, List<Task> displ) {
//        synchronized(builder) {
//            /* removed = */ remove(indexOf(belief, this));
//        }
//        TaskTable.removeTask(belief, null, displ);
//    }


    @Override
    public void clear() {
        synchronized (this) {
            forEachTask(Task::delete);
            super.clear();
        }
    }

    @Override
    public boolean removeTask(Task x) {

        x.delete();

        int index = indexOf(x, this);
        if (index == -1)
            return false; //HACK avoid synchronizing if the item isnt present

        synchronized (this) {
            int findAgainToBeSure = indexOf(x, this);
            return (findAgainToBeSure != -1) ?
                    remove(findAgainToBeSure) != null :
                    false;
        }


    }

    @Nullable
    public Task add(@NotNull Task input, TaskConcept concept, @NotNull NAR nar) {

        int cap = capacity();
        if (cap == 0) {
            if (input.isInput())
                throw new RuntimeException("input task rejected (0 capacity): " + input);
            return null;
        }

        Task revised = null;


            if ((input.conf() >= 1f) && (cap != 1) && (isEmpty() || (first().conf() < 1f))) {
                //AXIOMATIC/CONSTANT BELIEF/GOAL
                synchronized (this) {
                    addEternalAxiom(input, this, nar);
                    return input;
                }
            }

            //Try forming a revision and if successful, inputs to NAR for subsequent cycle
            /*if (!(input instanceof AnswerTask))*/

            revised = tryRevision(input, concept, nar);
            if (revised != null) {
                if (revised.equals(input)) {
                    //duplicate
                    //TODO decide what to do here
                    return revised;
                }

//                    if (revised.isDeleted()) {
//                        revised = null;
//                    } else if (Param.DEBUG) {
//
////                        if (revised.isDeleted())
////                            throw new RuntimeException("revised task is deleted");
//                        if (revised.equals(input)) // || BeliefTable.stronger(revised, input)==input) {
//                            throw new RuntimeException("useless revision");
//                    }
            }



        boolean inserted;
        synchronized (this) {
            //Finally try inserting this task.  If successful, it will be returned for link activation etc
            inserted = insert(input);
        }


        if (revised != null) {

            //            revised = insert(revised, displaced) ? revised : null;
            //
            //            if (revised!=null) {
            //                if (result == null) {
            //                    result = revised;
            //                } else {
            //                    //HACK
            //                    //insert a tasklink since it will not be created normally
            //                    nar.activate(revised, nar.conceptActivation.floatValue() /* correct? */);
            //                    revised.onConcept(revised.concept(nar), 0f);
            //
            //                }
            //            }
            //result = insert(revised, et) ? revised : result;

            nar.input(revised);

            //            nar.runLater(() -> {
            //                if (!revised.isDeleted())
            //                    nar.input(revised);
            //            });
        }

        return inserted ? input : null;
    }


    /**
     * try to insert but dont delete the input task if it wasn't inserted (but delete a displaced if it was)
     * returns true if it was inserted, false if not
     */
    private boolean insert(@NotNull Task input) {

        Truth before = this.truth;

        Task displaced = put(input);

        if (displaced == input) {
            //rejected
            return false;
        } else if (displaced != null) {
            removeTask(displaced,
                    "Displaced"
                    //"Displaced by " + incoming,
            );
        }
        return true;
    }

    private void addEternalAxiom(@NotNull Task input, @NotNull EternalTable et, NAR nar) {
        //lock incoming 100% confidence belief/goal into a 1-item capacity table by itself, preventing further insertions or changes
        //1. clear the corresponding table, set capacity to one, and insert this task
        et.forEachTask(t -> removeTask(t, "Overridden"));
        et.clear();
        et.capacity(1);

//        //2. clear the other table, set capcity to zero preventing temporal tasks
        //TODO
//        TemporalBeliefTable otherTable = temporal;
//        otherTable.forEach(overridden);
//        otherTable.clear();
//        otherTable.capacity(0);

        //NAR.logger.info("axiom: {}", input);

        et.put(input);

    }


    public final float rank(float eConf, int length) {
        //return rankEternalByConfAndOriginality(eConf, length);
        return eConf;
    }

    public boolean isFull() {
        return size() == capacity();
    }

}
