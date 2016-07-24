package nars.concept.table;

import nars.NAR;
import nars.Param;
import nars.task.AnswerTask;
import nars.task.Revision;
import nars.task.RevisionTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Termed;
import nars.truth.Truth;
import nars.util.data.sorted.SortedArray;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import static nars.concept.table.BeliefTable.rankEternalByConfAndOriginality;
import static nars.nal.Tense.ETERNAL;

/**
 * Created by me on 5/7/16.
 */
public class EternalTable extends SortedArray<Task> implements TaskTable, Comparator<Task> {

    int capacity;

    public EternalTable(int initialCapacity) {
        super(Task[]::new);
        capacity(initialCapacity, null);
    }

    public void capacity(int c, List<Task> displ) {
        if (this.capacity != c) {

            this.capacity = c;

            int s = size();

            //TODO can be accelerated by batch remove operation
            while (c < s--) {
                Task x = removeWeakest();
                displ.add(x);
            }
        }
    }

    @Override
    public void forEach(Consumer<? super Task> action) {
        synchronized(builder) {
            super.forEach(action);
        }
    }

    //    protected final Task removeWeakest(Object reason) {
//        if (!isEmpty()) {
//            Task x = remove(size() - 1);
//            x.delete(reason);
//        }
//    }


    //TODO use first() and last()
    @Deprecated public final Task strongest() {
        synchronized(builder) {
            if (isEmpty()) return null;
            return list[0];
        }
    }

    @Deprecated public final Task weakest() {
        synchronized(builder) {
            int s = size();
            if (s==0) return null;
            return list[s - 1];
        }
    }

    public float minRank() {
        Task w = weakest();
        if ( w== null) return 0;
        return rankEternalByConfAndOriginality(w);
    }

    @Override
    public final int compare(@NotNull Task o1, @NotNull Task o2) {
        float f1 = rankEternalByConfAndOriginality(o2); //reversed
        float f2 = rankEternalByConfAndOriginality(o1);
        if (f1 < f2)
            return -1;
        if (f1 > f2)
            return 1;
        return 0;
    }

    @Nullable
    public /*Revision*/Task tryRevision(@NotNull Task newBelief, @NotNull NAR nar) {

        synchronized(builder) {
            int bsize = size();
            if (bsize == 0)
                return null; //nothing to revise with

            //Try to select a best revision partner from existing beliefs:
            Task oldBelief = null;
            float bestRank = 0f, bestConf = 0f;
            Truth conclusion = null;
            final float newBeliefConf = newBelief.conf();
            Truth newBeliefTruth = newBelief.truth();


            for (int i = 0; i < bsize; i++) {
                Task x = list[i];

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

                Truth c = Revision.revisionEternal(newBeliefTruth, oldBeliefTruth, 1f, bestConf);
                if (c == null)
                    continue;

                //avoid a duplicate truth
                if (c.equals(oldBeliefTruth) || c.equals(newBeliefTruth))
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

            if (oldBelief != null) {
                @Nullable Termed<Compound> t = Revision.intermpolate(
                        newBelief, oldBelief,
                        newBeliefConf, oldBelief.conf()
                );

                if (t == null) {
                    //throw new RuntimeException("null interpolation " + newBelief + " with " + oldBelief);

                    //HACK just use the strongest belief's term
                    t = (newBelief.conf() > oldBelief.conf()) ? newBelief.term() : oldBelief.term();
                }

                return new RevisionTask(
                        t,
                        newBelief, oldBelief,
                        conclusion,
                        nar.time(), ETERNAL).
                        budget(oldBelief, newBelief).
                        log("Insertion Revision");
            }
        }

        return null;
    }

    public final Task put(final Task t) {
        Task displaced = null;

        if (size() == capacity()) {
            Task l = weakest();
            if (l.conf() <= t.conf()) {
                displaced = removeWeakest();
            }
        }

        add(t, this);

        return displaced;
    }

    public final Truth truth() {
        Task s = strongest();
        return s!=null ? s.truth() : null;
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

    public Task add(@NotNull Task input, List<Task> displaced, @NotNull NAR nar) {

        int cap = capacity();
        if (cap == 0) {
            if (input.isInput())
                throw new RuntimeException("input task rejected (0 capacity): " + input);
            return null;
        }

        synchronized (builder) {
            if ((input.conf() >= 1f) && (cap != 1) && (isEmpty() || (first().conf() < 1f))) {
                //AXIOMATIC/CONSTANT BELIEF/GOAL
                addEternalAxiom(input, this, displaced);
                return input;
            }

            removeDeleted(displaced);

            //Try forming a revision and if successful, inputs to NAR for subsequent cycle
            Task revised;
            if (!(input instanceof AnswerTask)) {
                revised = tryRevision(input, nar);
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
            return result;
        }

    }



    /** try to insert but dont delete the input task if it wasn't inserted (but delete a displaced if it was)
     *  returns true if it was inserted, false if not
     * */
    private boolean insert(@NotNull Task incoming, List<Task> displ) {

        Task displaced = put(incoming);

        if (displaced!=null && !displaced.isDeleted()) {
            TaskTable.removeTask(displaced,
                    "Displaced", displ
                    //"Displaced by " + incoming,
            );
        }

        boolean inserted = (displaced == null) || (displaced != incoming);//!displaced.equals(t);
        return inserted;
    }

    private void addEternalAxiom(@NotNull Task input, @NotNull EternalTable et, List<Task> displ) {
        //lock incoming 100% confidence belief/goal into a 1-item capacity table by itself, preventing further insertions or changes
        //1. clear the corresponding table, set capacity to one, and insert this task
        Consumer<Task> overridden = t -> TaskTable.removeTask(t, "Overridden", displ);
        et.forEach(overridden);
        et.clear();
        et.capacity(1, displ);

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
        return rankEternalByConfAndOriginality(eConf, length);
    }

    public boolean isFull() {
        return size() == capacity();
    }

    /** TODO abstract into removeIf(Predicate<> p) ... */
    public void removeDeleted(List<Task> displ) {

        synchronized (builder) {
            int s = size();
            for (int i = 0; i < s; ) {
                Task n = list[i];
                if (n == null || n.isDeleted()) {
                    displ.add( remove(i) );
                    s--;
                } else {
                    i++;
                }
            }
        }
    }
}
