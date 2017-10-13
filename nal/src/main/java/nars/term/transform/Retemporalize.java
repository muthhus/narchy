package nars.term.transform;

import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import static nars.Op.TemporalBits;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

abstract public class Retemporalize implements CompoundTransform {


    public static final Retemporalize retemporalizeAllToDTERNAL = new RetemporalizeAll(DTERNAL);
    public static final Retemporalize retemporalizeAllToXTERNAL = new RetemporalizeAll(XTERNAL);
    public static final Retemporalize retemporalizeXTERNALToDTERNAL = new RetemporalizeFromTo(XTERNAL, DTERNAL);
    public static final Retemporalize retemporalizeXTERNALToZero = new RetemporalizeFromTo(XTERNAL, 0);

    @Nullable
    @Override
    public Term transform(Compound x, Op op, int dt) {
        if (!x.hasAny(TemporalBits)) {
            return x;
        } else {
            return CompoundTransform.super.transform(x, op, dt(x));
        }
    }


//    @Nullable
//    @Override
//    public final Term apply(@NotNull Term x) {
//
//        if (!(x instanceof Compound) || !x.hasAny(TemporalBits))
//            return x;
//
//        Compound cx = (Compound) x;
//        Op o = cx.op();
//        boolean temporal = o.temporal;
//        int tdt = temporal ? dt(cx) : DTERNAL;
//        if (parent != null) {
//
//            return transform(cx, o, tdt);
//
//        } else {
//            return temporal ? cx.dt(tdt) : cx;
//        }
//    }

    @Override abstract public int dt(Compound x);

    @Deprecated
    public static final class RetemporalizeAll extends Retemporalize {

        final int targetDT;

        public RetemporalizeAll(int targetDT) {
            this.targetDT = targetDT;
        }

        @Override
        public final int dt(Compound x) {
            return targetDT;
        }
    }

    public static final class RetemporalizeFromTo extends Retemporalize {

        final int from, to;

        public RetemporalizeFromTo(int from, int to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public int dt(Compound x) {
            int dt = x.dt();
            return dt == from ? to : dt;
        }
    }

}
