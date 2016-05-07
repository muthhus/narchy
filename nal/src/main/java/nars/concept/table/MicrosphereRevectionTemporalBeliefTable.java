package nars.concept.table;

import nars.Global;
import nars.NAR;
import nars.bag.impl.ListTable;
import nars.bag.impl.SortedArrayTable;
import nars.bag.impl.SortedTable;
import nars.task.Revection;
import nars.task.Task;
import nars.task.TruthPolation;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import nars.truth.TruthFunctions;
import org.happy.collections.lists.decorators.SortedList_1x4;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/** stores the items unsorted; revection manages their ranking and removal */
public class MicrosphereRevectionTemporalBeliefTable extends ArrayListTable<Task,Task> implements TemporalBeliefTable {

    private TruthPolation polation;
    private final SortedTable<Task, Task> eternal;

    public MicrosphereRevectionTemporalBeliefTable(Map<Task, Task> mp, int cap, SortedTable<Task,Task> eternal) {
        super(mp, Global.newArrayList(cap));
        setCapacity(cap);
        this.eternal = eternal;
    }

    @Override
    public Task prepare(Task input, NAR nar) {
        if (isFull() /*&& temporal.capacity() > 1*/) {
            //Try forming a revision and if successful, inputs to NAR for subsequent cycle

            //TODO cache start, stop
            //long start = temporal.list().stream().mapToLong(t -> t.occurrence()).min().getAsLong();
            //long end = temporal.list().stream().mapToLong(t -> t.occurrence()).max().getAsLong();

            if (!Revection.revect(input, this, nar)) {
                return null; //rejected
            }

        }

        //proceed with this task now that there is room
        return input;
    }

    public final Task key(Task task) {
        return task;
    }

    @Override public Task top(long when, long now) {

        List<? extends Task> l = list();

        //find the best balance of temporal proximity and confidence
        int ls = l.size();
        if (ls == 1)
            return l.get(0); //the only task

        Task best = null;

        float ageFactor = 1f;///(1 + Math.abs(when - now) * 1f);
        float bestRank = -1;

        for (int i = 0; i < ls; i++) {
            Task x = l.get(i);
            float r = BeliefTable.rankTemporalByConfidence(x, when, now, ageFactor, bestRank);
            if (r > bestRank) {
                best = x;
                bestRank = r;
            }
        }

        return best;

    }

    @Override
    public Truth truth(long when) {
        if (polation == null || polation.capacity() < capacity()) {
            polation = new TruthPolation(capacity());
        }

        polation.set(list(), eternal.top());
        return polation.value(when);
    }


    //    public @Nullable Truth topTemporalCurrent(long when, long now, @Nullable Task topEternal) {
//        //find the temporal with the best rank
//        Task t = topTemporal(when, now);
//        if (t == null) {
//            return (topEternal != null) ? topEternal.truth() : Truth.Null;
//        } else {
//            Truth tt = t.truth();
//            return (topEternal() != null) ? tt.interpolate(topEternal.truth()) : tt;
//
//            //return t.truth();
//        }
//    }


//    //NEEDS DEBUGGED
//    @Nullable public Truth topTemporalWeighted(long when, long now, @Nullable Task topEternal) {
//
//        float sumFreq = 0, sumConf = 0;
//        float nF = 0, nC = 0;
//
//        if (topEternal!=null) {
//            //include with strength of 1
//
//            float ec = topEternal.conf();
//
//            sumFreq += topEternal.freq() * ec;
//            sumConf += ec;
//            nF+= ec;
//            nC+= ec;
//        }
//
//        List<Task> temp = list();
//        int numTemporal = temp.size();
//
//        if (numTemporal == 1) //optimization: just return the only temporal truth value if it's the only one
//            return temp.get(0).truth();
//
//
////        long maxtime = Long.MIN_VALUE;
////        long mintime = Long.MAX_VALUE;
////        for (int i = 0, listSize = numTemporal; i < listSize; i++) {
////            long t = temp.get(i).occurrence();
////            if (t > maxtime)
////                maxtime = t;
////            if (t < mintime)
////                mintime = t;
////        }
////        float dur = 1f/(1f + (maxtime - mintime));
//
//
//        long mdt = Long.MAX_VALUE;
//        for (int i = 0; i < numTemporal; i++) {
//            long t = temp.get(i).occurrence();
//            mdt = Math.min(mdt, Math.abs(now - t));
//        }
//        float window = 1f / (1f + mdt/2);
//
//
//        for (int i = 0, listSize = numTemporal; i < listSize; i++) {
//            Task x = temp.get(i);
//
//            float tc = x.conf();
//
//            float w = TruthFunctions.temporalIntersection(
//                    when, x.occurrence(), now, window);
//
//            //strength decreases with distance in time
//            float strength =  w * tc;
//
//            sumConf += tc * w;
//            nC+=tc;
//
//            sumFreq += x.freq() * strength;
//            nF+=strength;
//        }
//
//        return nC == 0 ? Truth.Null :
//                new DefaultTruth(sumFreq / nF, (sumConf/nC));
//    }

}
