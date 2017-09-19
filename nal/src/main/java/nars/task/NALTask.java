package nars.task;

import jcog.Util;
import jcog.pri.Pri;
import nars.Param;
import nars.Task;
import nars.control.Cause;
import nars.task.util.InvalidTaskException;
import nars.term.Term;
import nars.truth.DiscreteTruth;
import nars.truth.Truth;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static nars.Op.*;
import static nars.Param.CAUSE_CAPACITY;
import static nars.time.Tense.ETERNAL;

/**
 * Created by me on 2/24/17.
 */
public class NALTask extends Pri implements Task {

    public final Term term;
    public final DiscreteTruth truth;
    public final byte punc;
    private final long creation, start, end;

    public final long[] stamp;

    /**
     * TODO final
     */
    public short[] cause = ArrayUtils.EMPTY_SHORT_ARRAY;

    final int hash;

    public Map meta;


    public NALTask(Term term, byte punc, @Nullable Truth truth, long creation, long start, long end, long[] stamp) throws InvalidTaskException {

        if ((punc == BELIEF) || (punc == GOAL)) {
            if (truth == null)
                throw new InvalidTaskException(term, "null truth");
        }


//        //special case: simplify repeating conjunction of events
//        if (term.op() == CONJ) {
//            int dt = term.dt();
//            if (dt !=DTERNAL && dt!=0) {
//                Term s0 = term.sub(0);
//                if (s0 instanceof Compound && s0.unneg() instanceof Compound && s0.equals(term.sub(1))) {
//                    @Nullable Compound s01 = normalizedOrNull(s0, $.terms);
//                    if (s01!=null) {
//                        term = s01;
//                        if (dt > 0) {
//                            end = start + dt;
//                        } else if (dt < 0) {
//                            end = start - dt;
//                        }
//                    }
//                }
//            }
//        }

//        if (Param.DEBUG) {
//            term.recurseTerms(t -> {
//                assert (!(t instanceof UnnormalizedVariable));
//            });
//        }

        Task.taskContentValid(term, punc, null, false);

        this.pri = 0;

        this.term = term;

        this.truth =
                truth == null ? null :
                        truth instanceof DiscreteTruth ?
                                ((DiscreteTruth) truth) :
                                new DiscreteTruth(truth.freq(), truth.conf());

        this.punc = punc;


        assert (start == ETERNAL && end == ETERNAL) || (start != ETERNAL && start <= end) :
                "start=" + start + ", end=" + end + " is invalid task occurrence time";

        //ensure that a temporal task is at least as long as the contained dt.
        //bugs and rounding off-by-N errors may produce inexact results, this corrects it.
        if (start != ETERNAL && term.op() == CONJ) {
            int tdt = term.dtRange();
            if (tdt > 0) {
                if (tdt > (end - start)) {
                    end = start + tdt; //keeps start (left)-aligned, end is stretched if necessary
                }
            } else if (tdt < 0) {
                throw new RuntimeException("dt overflow");
            }
        }

        this.start = start;
        this.end = end;

        //EVIDENCE STAMP
        assert (punc == COMMAND || (stamp.length > 0)) : "non-command tasks must have non-empty stamp";
        this.stamp = stamp;

        //CALCULATE HASH
        int h = Util.hashCombine(
                term.hashCode(),
                punc,
                Arrays.hashCode(stamp)
        );

        if (stamp.length > 1) {

            if (start != ETERNAL) {
                h = Util.hashCombine(
                        Long.hashCode(start),
                        Long.hashCode(end), h
                );
            }

            DiscreteTruth t = this.truth;
            if (t != null)
                h = Util.hashCombine(t.hash, h);
        }

        this.hash = h;
        this.creation = creation;

        //READY
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public final boolean equals(Object that) {
        return this == that ||
                (hash == that.hashCode() &&
                    that instanceof Tasked &&
                    Task.equal(this, ((Tasked) that).task())
                );
    }


    /**
     * combine cause: should be called in all Task bags and belief tables on merge
     */
    public void causeMerge(Task incoming) {
        this.cause = Cause.zip(this, incoming);
    }

    @Nullable
    @Override
    public final Truth truth() {
        return truth;
    }

    @Override
    public byte punc() {
        return punc;
    }

    @Override
    public long creation() {
        return creation;
    }


    @Override
    public Term term() {
        return term;
    }

    @Override
    public long start() {
        return start;
    }

    @Override
    public long end() {
        return end;
    }


    @Override
    public @NotNull long[] stamp() {
        return stamp;
    }

    @Override
    public short[] cause() {
        return cause;
    }

    @Override
    public boolean delete() {
        if (super.delete()) {
            if (!Param.DEBUG)
                this.meta = null; //.clear();
            return true;
        }
        return false;
    }

    @Override
    @Deprecated
    public @Nullable List log() {
        if (meta != null) {
            Map m = meta;
            Object s = m.get(String.class);
            if (s != null)
                return (List) s;
        }
        return null;
    }


    @NotNull
    @Override
    @Deprecated
    public String toString() {
        return appendTo(null).toString();
    }


    @Override
    public Map meta() {
        return meta;
    }

    @Override
    public synchronized void meta(Object key, Object value) {
        //synchronized (this) {
        if (meta == null) {
            meta = UnifiedMap.newWithKeysValues(key, value); /* for compactness */
        } else {
            meta.put(key, value);
        }
        //}
    }

    @Override
    public <X> X meta(Object key) {
        if (meta != null) {
            //synchronized (this) {
            return (X) meta.get(key);
            //}
        }
        return null;
    }

//    @Nullable
//    public Task project(long newStart, int dur, float confMin) {
//        float newConf = conf(newStart, dur);
//        if (newConf < confMin)
//            return null;
//
//
//        ImmutableTask t = new ImmutableTask(term, punc, $.t(freq(), newConf), creation, newStart, newStart, stamp);
//        t.setPriority(this);
//        //t.meta
//        //t.log("Projected")
//        return t;
//    }

    public Task clone(Term x) {
        NALTask t = new NALTask(x, punc, truth, creation, start(), end(), stamp);
        t.setPri(this);
        return t;
    }

    @Override
    public final float freq() {
        return truth.freq;
    }

    @Override
    public final float conf() {
        return truth.conf;
    }

    @Override
    public final float evi() {
        return truth.evi();
    }

    @Override
    public final float eviEternalized() {
        return truth.eviEternalized();
    }

    @Override
    public double coord(boolean maxOrMin, int dimension) {
        switch (dimension) {
            case 0:
                return maxOrMin ? end : start;
            case 1:
                return truth.freq;
            case 2:
                return truth.conf;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public double range(int dim) {
        switch (dim) {
            case 0: return end-start;
            case 1: return 0;
            case 2: return 0;
            default:
                throw new UnsupportedOperationException();
        }
    }

    /** rtree cost heuristic; constant result */
    @Override public float freqCost() {
        return 1;
    }

    /** rtree cost heuristic; constant result */
    @Override public float confCost() {
        return 1;
    }
}
