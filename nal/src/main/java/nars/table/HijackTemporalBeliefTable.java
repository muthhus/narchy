//package nars.table;
//
//import jcog.list.FasterList;
//import jcog.util.Top2;
//import nars.NAR;
//import nars.Param;
//import nars.Task;
//import nars.bag.TaskHijackBag;
//import nars.concept.BaseConcept;
//import nars.task.Revision;
//import nars.term.Term;
//import nars.truth.Stamp;
//import nars.truth.Truth;
//import org.eclipse.collections.api.block.function.primitive.FloatFunction;
//import org.eclipse.collections.api.list.MutableList;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import static java.lang.Math.abs;
//import static jcog.math.Interval.intersectLength;
//import static nars.time.Tense.ETERNAL;
//
///**
// * stores the items unsorted; revection manages their ranking and removal
// */
//public class HijackTemporalBeliefTable extends TaskHijackBag implements TemporalBeliefTable {
//
//    long now;
//
//    public HijackTemporalBeliefTable() {
//        super(0, 4 /* reprobes */);
//    }
//
//    @Override
//    public float pri(Task t) {
//        return TemporalBeliefTable.temporalTaskPriority(t, now, now, 1 /*HACK*/);
//    }
//
//
////    @Override
////    protected boolean replace(Task incoming, Task existing) {
////
////        if (incoming instanceof SignalTask) //intercept signal tasks and give them priority
////            return true;
////
////        float exPri = existing.pri();
////        if (exPri!=exPri)
////            return true;
////
////        float inPri = incoming.priElseZero();
////        if (!replace(inPri, exPri)) {
////            //existing.priMult(1f - /*temperature() * */ inPri/reprobes);
////            return false;
////        }
////        return true;
////        //return super.replace(incoming, existing, scale);
////    }
//
//    @Override
//    public void add(@NotNull Task x, BaseConcept c, NAR n) {
//        now = n.time();
//        super.add(x, c, n);
//    }
//
//    //    @Override
////    public Task add(@NotNull Task x) {
////        if (x instanceof AnswerTask) {
////            return x; //dont store interpolations/answers/projections etc
////        }
////        return super.add(x);
////    }
//
//
////    @Override
////    protected Task merge(@Nullable Task existing, @NotNull Task incoming, float scaleIgnored) {
////        if (incoming instanceof SignalTask)
////            return incoming;
////
////        if (existing!=null && !existing.equals(incoming)) {
////            return merge(existing, incoming, Math.max(incoming.creation(), existing.creation()), 0f);
////        } else {
////            return super.merge(existing, incoming, scaleIgnored);
////        }
////    }
//
//    static FloatFunction<Task> evidence(long start, long end, long now, int dur) {
//        return (x) -> x.evi(start, end, dur);
//
//
//            //float r = t.evi(when, dur) * (1+ (t.end()-t.start()) );// * t.qua();
//
////        //HACK present/future prediction boost
////        if (t.start() >= now) {
////            r += 1f;
////        }
//    }
//
//    @Nullable
//    static Task matchMerge(@NotNull FasterList<Task> l, long now, @NotNull Task toMergeWith, int dur) {
//        return l.maxBy(Float.NEGATIVE_INFINITY, rankMatchMerge(toMergeWith, now, dur));
//    }
//
////    @Override
////    protected boolean replace(float incoming, float existing) {
////        return incoming >= existing;
////    }
//
//
//    //    protected boolean replace0(Task incoming, Task existing, float scaleIgnored) {
////        assert(scaleIgnored==1f);
////
////        if (incoming instanceof SignalTask) //intercept signal tasks and give them priority
////            return true;
//////        if (existing instanceof ActionConcept.CuriosityTask)
//////            return true;
////
////        if (!(existing instanceof SignalTask)) {
////            //return true if there is a possibility of merge, and do a merge of 2 non-equal tasks in merge()
////            Task i = (Task) incoming;
////            if ((random().nextFloat()) >= Math.abs(existing.freq() - i.freq())) {
////                /*if (!Stamp.overlapping(i, known))*/
////                {
////                    if (null != Interval.intersect(existing.start(), existing.end(), i.start(), i.end()))
////                        return true;
////                }
////            }
////        }
////
////        return replace(priConf(incoming) , priConf(existing));
////        //return super.replace(incoming, existing, scale);
////    }
//
//
//
//
//
//    //    @Override
////    public float pri(@NotNull Task key) {
////        long dt = key.mid() - lastCommitTime;
////
////        float f = super.pri(key) * key.conf() * 1f / (1f + Math.abs(dt));
////        if (dt > 0)
////            f = or(f, 0.5f);
////
////        return f;
////    }
//
//    //    public void capacity(int newCapacity, NAR nar) {
////
////        if (this.capacity != newCapacity) {
////
////            this.capacity = newCapacity;
////
////            //compress until under-capacity
////            ifSizeExceedsWriteWith(newCapacity, (l) -> {
////
////                int toRemove = l.size() - newCapacity; //will be positive
////
////                if (clean(l)) {
////                    toRemove = l.size() - newCapacity;
////                }
////
////                if (toRemove > 0) {
////
////                    Time time = nar.time;
////
////                    int dur = time.dur();
////                    long now = time.time();
////
////                    float confMin = nar.confMin.floatValue();
////
////                    Function<Task, Float> rank = temporalConfidence(now, now, dur);
////
////                    while (l.size() > capacity) {
////
////                        Task a = l.minBy(rank);
////
////                        Task b = matchMerge(l, now, a, dur);
////
////                        Task c = (b != null && b != a) ? merge(a, b, now, confMin, dur) : null;
////
////                        remove(l, a, true);
////
////                        if (c != null) {
////                            remove(l, b, true);
////
////                            l.add(c);
////                        }
////
////                    }
////
////                }
////
////            });
////
////        }
////
////    }
//
//
//
////    @Override
////    protected Consumer<Task> forget(float avgToBeRemoved) {
////        return new PForget<Task>(avgToBeRemoved) {
////            @Override public void accept( @NotNull Task b) {
////                b.priSub(avgToBeRemoved * (1f - b.conf()));
////            }
////        };
////    }
//
////    @Override
////    public float pri(@NotNull Task t) {
////        return t.pri();
//////        //return (1f + t.priElseZero()) * (1f + t.conf());
//////        float p = t.priSafe(-1);
//////        if (p >= 0)
//////            return Util.or((1f + p), (1f + t.conf()));
//////        else
//////            return -1;
////    }
//
//
////    /**
////     * apply feedback for a newly inserted task
////     */
////    protected void feedback(MutableList<Task> l, @NotNull Task inserted) {
////        float f = inserted.freq();
////        float c = inserted.conf();
////        long is = inserted.start();
////        long ie = inserted.end();
////
////        float penaltySum = 0;
////
////        for (int i = 0, lSize = l.size(); i < lSize; i++) {
////            Task x = l.get(i);
////            if (x == inserted) continue;
////
////
////            float df = abs(f - x.freq());
////
////            if (df > Param.BUDGET_EPSILON) {
////
////                long xs = x.start();
////                long xe = x.end();
////                long overlapLength = Interval.intersectLength(is, ie, xs, xe);
////                if (overlapLength >= 0) {
////
////                    float penalty = df * ((1f + overlapLength) / (1f + (xe - xs)));
////                    if (penalty > Param.BUDGET_EPSILON) {
////                        Priority b = x.budget();
////                        float pBefore = b.priElseZero(), pAfter;
////                        if (pBefore > 0) {
////                            b.priMult(1f - penalty);
////                            pAfter = b.pri();
////                            penaltySum += pAfter - pBefore;
////                        }
////                    }
////
////                }
////
////            }
////        }
////
////        if (penaltySum > 0)
////            inserted.budget().priAdd(penaltySum); //absorb removed priority from sibling tasks
////    }
//
//
////    /** HACK use of volatiles here is a hack. it may rarely cause the bag to experience flashbacks. proper locking can solve this */
////    private volatile long minT = MAX_VALUE, maxT = MIN_VALUE;
//
////    public long range() {
////        if (minT==MAX_VALUE || maxT==MIN_VALUE) {
////            //cached valus invalidated, re-compute
////            forEach(u -> {
////                long o = u.occurrence();
////                if (minT > o)
////                    minT = o;
////                if (maxT < o)
////                    maxT = o;
////            });
////        } else {
////            //using cached value
////        }
////
////        if (minT==MAX_VALUE || maxT==MIN_VALUE) {
////            return 1; //empty probably} else {
////        } else {
////
////            return Math.max(0, maxT - minT);
////        }
////
////    }
//
////    public int duration() {
////        //return (((float)range()) / size()) * 2f;
////        return 1f;
////    }
//
//
////    @Override
////    public final boolean isFull() {
////        return size() == capacity();
////    }
//
//
//    //    private void invalidateDuration() {
////        //invalidate
////        minT = MAX_VALUE;
////        maxT = MIN_VALUE;
////    }
//
//
////    @Override
////    public void minTime(long minT) {
////        this.min = minT;
////    }
////
////    @Override
////    public void maxTime(long maxT) {
////        this.max = maxT;
////    }
//
////
////    @Override
////    public final void range(long[] t) {
////        for (Task x : this.items) {
////            if (x != null) {
////                long o = x.occurrence();
////                if (o < t[0]) t[0] = o;
////                if (o > t[1]) t[1] = o;
////            }
////        }
////    }
//
////
////    final boolean remove(MutableList<Task> l, @NotNull Task x, boolean delete) {
////        if (l.remove(x)) {
////            if (delete)
////                x.delete();
////            remove(x);
////            return true;
////        }
////        return false;
////    }
//
//    /**
//     * max value given to the ideal match for the provided task to be merged with
//     */
//    @NotNull
//    static public FloatFunction<Task> rankMatchMerge(@NotNull Task y, long now, int dur) {
//
//        //prefer
//        //  same frequency
//        //  long time from now
//        //  non-zero overlap with the task
//        //  least stamp overlap
//        //  low confidence?
//        //  similar range
//
//        long ys = y.start();
//        long ye = y.end();
//        float fdur = dur;
//        float yRange = (ye - ys) / fdur;
//        float yf = y.freq();
//        float yDist = Math.min(abs(ye - now), abs(ys - now)) / fdur;
//        long[] yStamp = y.stamp();
//
//        return x -> {
//            if ((x == y) || (x == null))
//                return Float.NEGATIVE_INFINITY;
//
//            long xs = x.start();
//            long xe = x.end();
////            float xtc = x.conf(yo);
////            if (xtc!=xtc) xtc = 0;
//
//            long overlap = intersectLength(ys, ye, xs, xe);
//            if (overlap == -1)
//                return Float.NEGATIVE_INFINITY; //dont allow merge if no overlap
//
//            return (1f - abs(x.freq() - yf)) *
//                    (1f / (1f + abs(yRange - (xe - xs)))) *
//                    (1f + Math.min(abs(xe - now), abs(xs - now)) / yDist) *
//                    (1f + overlap / (1 + yRange)) *
//                    (1f - Stamp.overlapFraction(yStamp, x.stamp())/2f)
//                    //(1f + (1f - x.conf()))
//                    ;
//        };
//
//    }
//
//
//
//    //    static private FloatFunction<Task> temporalConfidenceF(long when, long now, int dur) {
////        return x -> rankTemporalByConfidence(x, when, now, dur);
////    }
//
//
//
//
////    /**
////     * frees one slot by removing 2 and projecting a new belief to their midpoint. returns the merged task
////     */
////    protected Task compress(@Nullable Task input, long now, FasterList<Task> l, int dur, float confMin) {
////
////        int cap = capacity();
////
////        if (l.size() < cap || clean(l)) {
////            return input; //no need for compression
////        }
////
////        //return rankTemporalByConfidenceAndOriginality(t, when, now, -1);
////        float inputRank = input != null ? rankTemporalByConfidence(input, now, dur) : Float.POSITIVE_INFINITY;
////
////        Task a = l.minBy(temporalConfidence(now, now, dur));
////
////        if (a == null) {
////            return null; // this probably shouldnt happen
////        }
////
////        if (inputRank < rankTemporalByConfidence(a, now, dur)) {
////            //if weaker than everything else, attempt a merge of the input with another
////
////            Task b = matchMerge(l, now, input, dur);
////            if (b == null) {
////                return null; //nothing to merge with
////            }
////
////            Task merged = merge(input, b, now, confMin);
////            if (merged == null) {
////                return null; //merge failed
////            }
////
////            remove(l, b, true);
////
////            return merged;
////
////        } else {
////            //merge 2 weakest and allow the input as-is
////
////            remove(l, a, false);
////
////            Task b = matchMerge(l, now, a, dur);
////            if (b != null) {
////                Task merged = merge(a, b, now, confMin);
////
////                a.delete();  //delete a after the merge, so that its budget revises
////
////                if (merged == null) {
////                    return input;
////                } else {
////                    remove(l, b, true);
////                    return merged;
////                }
////            } else {
////                a.delete();
////
////                return input;
////            }
////
////        }
////
////    }
//
//
//    @Override
//    public Task match(long start, long end, @Nullable Term against, NAR nar) {
//
//
//        if (start == ETERNAL)  start = end = nar.time();
//
//        long now = nar.time();
//        int dur = nar.dur();
//
//        //choose only top:
//        //return maxBy(evidence(when, now, dur));
//
//
//        //choose top 2 and merge them:
//        Top2<Task> s = new Top2(evidence(start, end, now, dur), this);
//        Task a = s.a;
//        if (s.b == null || (s.a.during(start,end) && !s.b.during(start, end)))
//            return a;
//
//        Task c = Revision.merge(a, s.b, now, nar);
//        return c != null ? c : a;
//    }
//
//
//    @Override
//    public Truth truth(long start, long end,  @Nullable EternalTable eternal, NAR nar) {
//
//        if (start == ETERNAL)  start = end = nar.time();
//
//        Truth x = Param.truth(
//                eternal != null ? eternal.strongest() : null,
//                start, end, nar.dur(), this);
//        return x;
//
////        if (x != null && x.conf() >= Param.TRUTH_EPSILON) {
////            return x.ditherFreqConf(nar.truthResolution.floatValue(), nar.confMin.floatValue(), 1f);
////        } else {
////            return null; //cut-off
////        }
//
//
//        //return Truth.maxConf(r, topEternal);
//        //return (r == null && topEternal != null) ? topEternal.truth() : r;
//    }
//
//    final boolean clean(MutableList<Task> l) {
//        return l.removeIf(x -> {
//            if (x.isDeleted()) {
//                remove(x);
//                return true;
//            }
//            return false;
//        });
//    }
//
//
//    //    public final boolean removeIf(@NotNull Predicate<? super Task> o) {
////
////        IntArrayList toRemove = new IntArrayList();
////        for (int i = 0, thisSize = this.size(); i < thisSize; i++) {
////            Task x = this.get(i);
////            if ((x == null) || (o.test(x)))
////                toRemove.add(i);
////        }
////        if (toRemove.isEmpty())
////            return false;
////        toRemove.forEach(this::remove);
////        return true;
////    }
//
//    //    public Task weakest(Task input, NAR nar) {
////
////        //if (polation == null) {
////            //force update for current time
////
////        polation.credit.clear();
////        Truth current = truth(nar.time());
////        //}
////
//////        if (polation.credit.isEmpty())
//////            throw new RuntimeException("empty credit table");
////
////        List<Task> list = list();
////        float min = Float.POSITIVE_INFINITY;
////        Task minT = null;
////        for (int i = 0, listSize = list.size(); i < listSize; i++) {
////            Task t = list.get(i);
////            float x = polation.value(t, -1);
////            if (x >= 0 && x < min) {
////                min = x;
////                minT = t;
////            }
////        }
////
////        System.out.println("removing " + min + "\n\t" + polation.credit);
////
////        return minT;
////    }
//
//
//    //    public @Nullable Truth topTemporalCurrent(long when, long now, @Nullable Task topEternal) {
////        //find the temporal with the best rank
////        Task t = topTemporal(when, now);
////        if (t == null) {
////            return (topEternal != null) ? topEternal.truth() : Truth.Null;
////        } else {
////            Truth tt = t.truth();
////            return (topEternal() != null) ? tt.interpolate(topEternal.truth()) : tt;
////
////            //return t.truth();
////        }
////    }
//
//
////    //NEEDS DEBUGGED
////    @Nullable public Truth topTemporalWeighted(long when, long now, @Nullable Task topEternal) {
////
////        float sumFreq = 0, sumConf = 0;
////        float nF = 0, nC = 0;
////
////        if (topEternal!=null) {
////            //include with strength of 1
////
////            float ec = topEternal.conf();
////
////            sumFreq += topEternal.freq() * ec;
////            sumConf += ec;
////            nF+= ec;
////            nC+= ec;
////        }
////
////        List<Task> temp = list();
////        int numTemporal = temp.size();
////
////        if (numTemporal == 1) //optimization: just return the only temporal truth value if it's the only one
////            return temp.get(0).truth();
////
////
//////        long maxtime = Long.MIN_VALUE;
//////        long mintime = Long.MAX_VALUE;
//////        for (int i = 0, listSize = numTemporal; i < listSize; i++) {
//////            long t = temp.get(i).occurrence();
//////            if (t > maxtime)
//////                maxtime = t;
//////            if (t < mintime)
//////                mintime = t;
//////        }
//////        int dur = 1f/(1f + (maxtime - mintime));
////
////
////        long mdt = Long.MAX_VALUE;
////        for (int i = 0; i < numTemporal; i++) {
////            long t = temp.get(i).occurrence();
////            mdt = Math.min(mdt, Math.abs(now - t));
////        }
////        float window = 1f / (1f + mdt/2);
////
////
////        for (int i = 0, listSize = numTemporal; i < listSize; i++) {
////            Task x = temp.get(i);
////
////            float tc = x.conf();
////
////            float w = TruthFunctions.temporalIntersection(
////                    when, x.occurrence(), now, window);
////
////            //strength decreases with distance in time
////            float strength =  w * tc;
////
////            sumConf += tc * w;
////            nC+=tc;
////
////            sumFreq += x.freq() * strength;
////            nF+=strength;
////        }
////
////        return nC == 0 ? Truth.Null :
////                new DefaultTruth(sumFreq / nF, (sumConf/nC));
////    }
//
//}
