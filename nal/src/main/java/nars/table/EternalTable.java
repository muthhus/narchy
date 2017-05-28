package nars.table;

import jcog.data.sorted.SortedArray;
import jcog.pri.Pri;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.budget.BudgetFunctions;
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

import static nars.term.Terms.normalizedOrNull;
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
        super();
        this.capacity = initialCapacity;
    }

    @Override
    protected Task[] newArray(int oldSize) {
        return new Task[grow(oldSize)];
    }

    public void capacity(int c) {
        if (this.capacity != c) {

            this.capacity = c;

            synchronized (this) {

                int s = size();

                //TODO can be accelerated by batch remove operation
                while (c < s--) {
                    Task r = removeLast();
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


    public final Task strongest() {
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
    private /*Revision*/Task tryRevision(@NotNull Task newBelief /* input */, @NotNull TaskConcept concept, @NotNull NAR nar) {

        Object[] list = this.list;
        int bsize = list.length;
        if (bsize == 0)
            return null; //nothing to revise with


        //Try to select a best revision partner from existing beliefs:
        Task oldBelief = null;
        Truth conclusion = null;

        Truth newBeliefTruth = newBelief.truth();

        int dur = nar.dur();

        for (int i = 0; i < bsize; i++) {
            Task x = (Task) list[i];

            if (x == null) //the array has trailing nulls from having extra capacity
                break;

            if (x.equals(newBelief)) {
                return x;
            }

            if (!Revision.isRevisible(newBelief, x))
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

            Truth c = Revision.revise(newBeliefTruth, oldBeliefTruth, 1f, conclusion == null ?  0 : conclusion.evi());

            //avoid a weak or duplicate truth
            if (c == null || c.equals(oldBeliefTruth) || c.equals(newBeliefTruth))
                continue;

            oldBelief = x;
            conclusion = c;

        }

        if (oldBelief == null)
            return null;

        final float newBeliefWeight = newBelief.evi();

        //TODO use Task.tryContent in building the task:

        float aProp = newBeliefWeight / (newBeliefWeight + oldBelief.evi());
        Compound t = normalizedOrNull(Revision.intermpolate(
                newBelief.term(), oldBelief.term(),
                aProp,
                nar.random(),
                true
        ), nar.terms);

        if (t == null)
            return null;

        RevisionTask r = new RevisionTask(t,
                newBelief, oldBelief,
                conclusion,
                nar.time(),
                ETERNAL, ETERNAL);
        r.setPri(BudgetFunctions.fund(1f,false,oldBelief,newBelief));

        if (Param.DEBUG)
            r.log("Insertion Revision");

        return r;
    }

    @Nullable
    public Task put(@NotNull final Task incoming) {
        Task displaced = null;

        synchronized (this) {
            if (size() == capacity()) {
                Task weakestPresent = weakest();
                if (weakestPresent != null) {
                    if (floatValueOf(weakestPresent) <= floatValueOf(incoming)) {
                        displaced = removeLast();
                    } else {
                        return incoming; //insufficient confidence
                    }
                }
            }

            add(incoming, this);
        }

        return displaced;
    }

    public final Truth truth() {
        Task s = strongest();
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
    public void add(@NotNull Task input, TaskConcept c, @NotNull NAR nar) {

        int cap = capacity();
        if (cap == 0) {
            if (input.isInput())
                throw new RuntimeException("input task rejected (0 capacity): " + input);
            return;
        }

        Task activated;
        float activation;

        if ((input.conf() >= 1f) && (cap != 1) && (isEmpty() || (first().conf() < 1f))) {
            //AXIOMATIC/CONSTANT BELIEF/GOAL
            synchronized (this) {
                addEternalAxiom(input, this, nar);
                activation = input.priSafe(0);
                activated = input;
            }
        } else {


            Task revised = tryRevision(input, c, nar);
            if (revised != null) {
                if (revised == input) {
                    activation = 0;//already present duplicate, so ignore
                    activated = null;
                } else if (revised.equals(input)) {
                    activation = input.priSafe(0) - revised.priSafe(0);
                    activated = revised; //use previous value
                    input.delete();
                } else {
                    //a novel revision
                    if (insert(revised)) {
                        activation = revised.priSafe(0);
                        activated = revised;
                    } else {
                        activation = 0; //couldnt insert the revision
                        activated = null;
                        revised.delete();
                    }

                    boolean inputIns = insert(input);
                    if (inputIns) {
                        if (activated == null) {
                            activated = input;
                            activation = input.priSafe(0);
                        } else {
                            //revised will be activated, but at least emit a taskProcess for the input task
                            nar.eventTaskProcess.emit(input);
                        }
                    } else {
                        activated = null;
                        activation = 0;
                        input.delete();
                    }
                }
            } else {
                if (insert(input)) {
                    activated = input;
                    activation = input.priSafe(0);
                } else {
                    activation = 0;
                    activated = null;
                    input.delete();
                }
            }
        }


        if (activation >= Pri.EPSILON) {
            TaskTable.activate(activated, Float.POSITIVE_INFINITY, c, nar);
        }
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




}
