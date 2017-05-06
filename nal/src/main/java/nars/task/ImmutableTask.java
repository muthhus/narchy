package nars.task;

import jcog.Util;
import jcog.pri.Pri;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.task.util.InvalidTaskException;
import nars.term.Compound;
import nars.truth.DiscreteTruth;
import nars.truth.Truth;
import nars.truth.TruthDelta;
import org.apache.commons.collections4.map.Flat3Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static nars.Op.*;
import static nars.term.Terms.compoundOrNull;
import static nars.time.Tense.ETERNAL;

/**
 * Created by me on 2/24/17.
 */
public class ImmutableTask extends Pri implements Task {

    public final Compound term;
    public final DiscreteTruth truth;
    public final byte punc;
    private final long creation, start, end;
    public final long[] stamp;
    final int hash;

    public Map meta;


    public ImmutableTask(Compound term, byte punc, @Nullable Truth truth, long creation, long start, long end, long[] stamp) throws InvalidTaskException {

        if ((punc == BELIEF) || (punc == GOAL)) {
            if (truth == null)
                throw new InvalidTaskException(term, "null truth");
        }

        if (term.op() == NEG) {
            term = compoundOrNull(term.unneg());
            if (term == null)
                throw new InvalidTaskException(term, "became non-compound on un-negation");

            if (truth != null)
                truth = truth.negated();
        }

        Task.taskContentValid(term, punc, null, false);

        this.priority = 0;

        this.term = term;

        this.truth =
            truth==null ? null :
                truth instanceof DiscreteTruth ?
                    ((DiscreteTruth)truth) :
                    new DiscreteTruth(truth.freq(), truth.conf());

        this.punc = punc;

        if (!((start==ETERNAL && end==ETERNAL) || (start <= end) )) {
            throw new RuntimeException("invalid task occurrence time: " + start + ".." + end);
        }

        this.start = start;
        this.end = end;

        //EVIDENCE STAMP
        assert(punc == COMMAND || (stamp.length > 0) );
        this.stamp = stamp;

        //CALCULATE HASH
        int h = Util.hashCombine(
                term.hashCode(),
                punc,
                Arrays.hashCode(stamp)
        );

        if (stamp.length > 1) {

            if (start!=ETERNAL) {
                h = Util.hashCombine(
                        Long.hashCode(start),
                        Long.hashCode(end), h
                );
            }

            Truth t = truth();
            if (t != null)
                h = Util.hashCombine(t.hashCode(), h);
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
                (that instanceof Task &&
                 hash == that.hashCode() &&
                 Task.equivalentTo(this, (Task) that, true, true, true, true, true));
    }

    @Nullable
    @Override
    public Truth truth() {
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
    public @NotNull Compound term() {
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
    public void feedback(TruthDelta delta, float deltaConfidence, float deltaSatisfaction, NAR nar) {

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
        return appendTo(null, null).toString();
    }


    @Override
    public Map meta() {
        return meta;
    }

    @Override
    public void meta(Object key, Object value) {
        //synchronized (this) {
        if (meta == null) {
            //meta = new UnifiedMap(1); /* for compactness */
            meta = new Flat3Map(); /* for compactness */
        }

        meta.put(key, value);
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

    public Task clone(Compound x) {
        ImmutableTask t = new ImmutableTask(x, punc, truth, creation, start(), end(), stamp);
        t.setPriority(this);
        return t;
    }

}
