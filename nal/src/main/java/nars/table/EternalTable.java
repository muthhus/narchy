package nars.table;

import jcog.data.sorted.SortedArray;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.CompoundConcept;
import nars.concept.Concept;
import nars.task.AnswerTask;
import nars.task.Revision;
import nars.task.RevisionTask;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.TruthDelta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static nars.time.Tense.ETERNAL;


/**
 * Created by me on 5/7/16.
 */
public class EternalTable extends SortedArray<Task> implements TaskTable, SortedArray.Ranker<Task> {

    public static final EternalTable EMPTY = new EternalTable(0) {

        @Override
        public @Nullable Task put(@NotNull Task incoming) {
            throw new UnsupportedOperationException(incoming + " not insertable into EternalTable.EMPTY");
        }

        @Override
        public final int capacity() {
            return 0;
        }

        @Override
        public void forEach(Consumer<? super Task> action) {

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

    public void capacity(int c, @NotNull NAR nar) {
        if (this.capacity != c) {

            this.capacity = c;

            int s = size();

            //TODO can be accelerated by batch remove operation
            while (c < s--) {
                nar.tasks.remove(removeWeakest());
            }
        }
    }

    @Override
    public void forEach(Consumer<? super Task> action) {
        synchronized (this) {
            super.forEach(action);
        }
    }

    //    protected final Task removeWeakest(Object reason) {
//        if (!isEmpty()) {
//            Task x = remove(size() - 1);
//            x.delete(reason);
//        }
//    }


    public final Task strongest() {
        Object[] l = this.list;
        if (l.length == 0) return null;
        return (Task) l[0];
    }

    public final Task weakest() {
        Object[] l = this.list;
        if (l.length == 0) return null;
        int n = l.length - 1;
        Task w = null;
        while (n > 0 && (w = (Task) l[n]) != null) n--; //scan upwards for first non-null
        return w;

    }

    public final float rank(@NotNull Task w) {
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

    @Nullable
    public /*Revision*/Task tryRevision(@NotNull Task newBelief, @NotNull Concept concept, @NotNull NAR nar) {

        Object[] list = this.list;
        int bsize = list.length;
        if (bsize == 0)
            return null; //nothing to revise with


        //Try to select a best revision partner from existing beliefs:
        Task oldBelief = null;
        float bestRank = 0f, bestConf = 0f;
        Truth conclusion = null;

        Truth newBeliefTruth = newBelief.truth();


        for (int i = 0; i < bsize; i++) {
            Task x = (Task) list[i];

            if (x == null) //the array has trailing nulls from having extra capacity
                break;

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


        final float newBeliefWeight = newBelief.evi();
        float aProp = newBeliefWeight / ( newBeliefWeight + oldBelief.evi());
        Term t = Revision.intermpolate(
                newBelief.term(), oldBelief.term(),
                aProp,
                nar.random,
                true
        );

        return new RevisionTask( t,
                newBelief, oldBelief,
                conclusion,
                nar.time(),
                ETERNAL,
                (CompoundConcept)concept
            ).budget(oldBelief, newBelief)
             .log("Insertion Revision");
    }

    @Nullable public Task put(@NotNull final Task incoming) {
        Task displaced = null;

        if (size() == capacity()) {
            Task weakestPresent = weakest();
            if (weakestPresent!=null) {
                if (rank(weakestPresent) <= rank(incoming)) {
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

    @Nullable
    public TruthDelta add(@NotNull Task input, CompoundConcept<?> concept, @NotNull NAR nar) {

        int cap = capacity();
        if (cap == 0) {
            if (input.isInput())
                throw new RuntimeException("input task rejected (0 capacity): " + input);
            return null;
        }

        synchronized (this) {
            if ((input.conf() >= 1f) && (cap != 1) && (isEmpty() || (first().conf() < 1f))) {
                //AXIOMATIC/CONSTANT BELIEF/GOAL
                addEternalAxiom(input, this, nar);
                return new TruthDelta(input.truth(), input.truth()); //special
            }

            //Try forming a revision and if successful, inputs to NAR for subsequent cycle
            Task revised;
            if (!(input instanceof AnswerTask)) {
                revised = tryRevision(input, concept, nar);
                if (revised != null) {
                    if (revised.isDeleted()) {
                        revised = null;
                    } else if (Param.DEBUG) {

//                        if (revised.isDeleted())
//                            throw new RuntimeException("revised task is deleted");
                        if (revised.equals(input)) // || BeliefTable.stronger(revised, input)==input) {
                            throw new RuntimeException("useless revision");
                    }
                }
            } else {
                revised = null;
            }



            //Finally try inserting this task.  If successful, it will be returned for link activation etc
            TruthDelta delta = insert(input, nar);
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
                nar.inputLater(revised);
                //            nar.runLater(() -> {
                //                if (!revised.isDeleted())
                //                    nar.input(revised);
                //            });
            }

            return delta;
        }

    }


    /**
     * try to insert but dont delete the input task if it wasn't inserted (but delete a displaced if it was)
     * returns true if it was inserted, false if not
     */
    private TruthDelta insert(@NotNull Task input, @NotNull NAR nar) {

        Truth before = this.truth;

        Task displaced = put(input);

        if (displaced == input) {
            return null; //rejected
        } else if (displaced!=null) {
            TaskTable.removeTask(displaced,
                    "Displaced", nar
                    //"Displaced by " + incoming,
            );
        }

        return new TruthDelta(before, this.truth = truth());
    }

    private void addEternalAxiom(@NotNull Task input, @NotNull EternalTable et, NAR nar) {
        //lock incoming 100% confidence belief/goal into a 1-item capacity table by itself, preventing further insertions or changes
        //1. clear the corresponding table, set capacity to one, and insert this task
        Consumer<Task> overridden = t -> TaskTable.removeTask(t, "Overridden", nar);
        et.forEach(overridden);
        et.clear();
        et.capacity(1, nar);

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
