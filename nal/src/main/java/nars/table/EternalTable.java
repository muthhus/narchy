package nars.table;

import com.google.common.collect.Streams;
import jcog.data.sorted.SortedArray;
import jcog.pri.Pri;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.BaseConcept;
import nars.control.Activate;
import nars.control.Cause;
import nars.task.NALTask;
import nars.task.Revision;
import nars.term.Term;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.util.BudgetFunctions;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static nars.term.Terms.normalizedOrNull;
import static nars.time.Tense.ETERNAL;


/**
 * Created by me on 5/7/16.
 */
public class EternalTable extends SortedArray<Task> implements TaskTable, FloatFunction<Task> {

    public static final EternalTable EMPTY = new EternalTable(0) {

        @Override
        public Task strongest() {
            return null;
        }

        @Override
        public Task weakest() {
            return null;
        }

        @Override
        public @Nullable Task put(@NotNull Task incoming) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeTask(Task x) {
            return false;
        }

        @Override
        public @Nullable void add(@NotNull Task input, BaseConcept c, @NotNull NAR nar) {
            //nothing
        }


        @Override
        public void setCapacity(int c) {

        }

        @Override
        public void forEachTask(Consumer<? super Task> action) {

        }

        @NotNull
        @Override
        public Iterator<Task> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return 0;
        }
    };

    @Nullable
    private Truth truth;

    public EternalTable(int initialCapacity) {
        super();
        setCapacity(initialCapacity);
    }

    @Override
    public Stream<Task> stream() {
        return Streams.stream(iterator());
    }

    @Override
    protected Task[] newArray(int oldSize) {
        return new Task[grow(oldSize)];
    }

    public void setCapacity(int c) {
        int wasCapacity = this.capacity();
        if (wasCapacity != c) {

            synchronized (this) {
                if (capacity() == c)
                    return; //already set

                int s = size;

                //TODO can be accelerated by batch remove operation
                while (c < s--) {
                    removeLast().delete();
                }

                resize(c);
            }

        }
    }

    @Override
    public void forEachTask(Consumer<? super Task> x) {
        super.forEach((y) -> {
            if (!y.isDeleted()) x.accept(y);
        });
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


    public Task strongest() {
        Object[] l = this.list;
        return (l.length == 0) ? null : (Task) l[0];
    }

    public Task weakest() {
        Object[] l = this.list;
        if (l.length == 0) return null;
        int n = l.length - 1;
        Task w = null;
        while (n > 0 && (w = (Task) l[n]) != null) n--; //scan upwards for first non-null
        return w;

    }

    @Override
    public final Iterator<Task> taskIterator() {
        return iterator();
    }

    /**
     * for ranking purposes
     */
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
    private /*Revision*/Task tryRevision(@NotNull Task y /* input */, @NotNull BaseConcept concept, @NotNull NAR nar) {

        Object[] list = this.list;
        int bsize = list.length;
        if (bsize == 0)
            return null; //nothing to revise with


        //Try to select a best revision partner from existing beliefs:
        Task oldBelief = null;
        Truth conclusion = null;

        Truth newBeliefTruth = y.truth();

        for (int i = 0; i < bsize; i++) {
            Task x = (Task) list[i];

            if (x == null) //the array has trailing nulls from having extra capacity
                break;

            if (x.equals(y)) {
                return x;
            }

            if (!Revision.isRevisible(y, x))
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

            Truth xt = x.truth();

            Truth yt = Revision.revise(newBeliefTruth, xt, 1f, conclusion == null ? 0 : conclusion.evi());
            if (yt==null)
                continue;

            yt = yt.ditherFreqConf(nar.truthResolution.floatValue(), nar.confMin.floatValue(), 1f);
            if (yt == null || yt.equals(xt) || yt.equals(newBeliefTruth)) ////avoid a weak or duplicate truth
                continue;

            oldBelief = x;
            conclusion = yt;

        }

        if (oldBelief == null)
            return null;

        final float newBeliefWeight = y.evi();

        //TODO use Task.tryContent in building the task:

        float aProp = newBeliefWeight / (newBeliefWeight + oldBelief.evi());
        Term t = normalizedOrNull(
                Revision.intermpolate(
                y.term(), oldBelief.term(),
                aProp,
                nar.random(),
                false
        ));


        Truth revisionTruth = conclusion;
        Task prevBelief = oldBelief;
        return Task.tryTask(t, y.punc(), conclusion, (term, truth) -> {
            NALTask r = new NALTask(t,
                    y.punc(),
                    revisionTruth,
                    nar.time(),
                    ETERNAL, ETERNAL,
                    Stamp.zip(prevBelief.stamp(), y.stamp(), 0.5f /* TODO proportionalize */)
            );


            r.setPri(BudgetFunctions.fund(1f, false, prevBelief, y));
            r.cause = Cause.zip(y, prevBelief);

            if (Param.DEBUG)
                r.log("Insertion Revision");
            return r;
        });
    }

    @Nullable
    public Task put(@NotNull final Task incoming) {
        Task displaced = null;

        synchronized (this) {
            if (size == capacity()) {
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
            return (findAgainToBeSure != -1) && remove(findAgainToBeSure) != null;
        }


    }

    @Nullable
    public void add(@NotNull Task input, BaseConcept c, @NotNull NAR nar) {

        int cap = capacity();
        if (cap == 0) {
            //may be deleted already
            /*if (input.isInput())
                throw new RuntimeException("input task rejected (0 capacity): " + input + " "+ this + " " + this.capacity());*/
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
                    //already present duplicate, so ignore
                    return;
                } else if (revised.equals(input)) { //HACK todo avoid this duplcate equals which is already known from tryRevision

//                    float maxActivation = 1f - revised.priSafe(0);
//                    activation = Math.min(maxActivation, input.priSafe(0)); //absorb up to 1.0 max
                    float before = revised.priElseZero();
                    revised.priMax(input.priElseZero());
                    float after = revised.priElseZero();

                    activated = revised;
                    activation = after - before; //use previous value
                    input.delete();

                    if (activation >= Pri.EPSILON) {
                        ((NALTask) revised).merge(input);
                    } else {
                        activated = null; //dont bother activating
                    }
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
                            nar.eventTask.emit(input);
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


        if (activated != null)
            Activate.activate(activated, activation, nar);
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
        et.setCapacity(1);

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
