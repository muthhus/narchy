package nars.term.transform;

import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

abstract public class Retemporalize implements CompoundTransform {


    public static final Retemporalize retemporalizeAllToDTERNAL = new RetemporalizeAll(DTERNAL);
    public static final Retemporalize retemporalizeAllToXTERNAL = new RetemporalizeAll(XTERNAL);
    public static final Retemporalize retemporalizeXTERNALToDTERNAL = new RetemporalizeNonXternal(DTERNAL);
    public static final Retemporalize retemporalizeXTERNALToZero = new RetemporalizeNonXternal(0);

    @Override
    public final boolean testSuperTerm(@NotNull Compound c) {
        return c.isTemporal();
    }

    @Nullable
    @Override
    public final Term apply(@Nullable Compound parent, @NotNull Term x) {

        if (!(x instanceof Compound) || !x.hasAny(Op.TemporalBits))
            return x;

        Compound cx = (Compound) x;
        int tdt = dt(cx);
        if (parent != null) {
            return cx.transform(tdt, this);
        } else {
            return cx.dt(tdt);
        }
    }

    @Override abstract public int dt(Compound x);

    @Deprecated
    public static class RetemporalizeAll extends Retemporalize {

        final int targetDT;

        public RetemporalizeAll(int targetDT) {
            this.targetDT = targetDT;
        }

        @Override
        public int dt(Compound x) {
            return x.op().temporal ? targetDT : DTERNAL;
        }
    }

    @Deprecated
    public static class RetemporalizeNonXternal extends Retemporalize {

        final int dtIfXternal;

        public RetemporalizeNonXternal(int dtIfXternal) {
            this.dtIfXternal = dtIfXternal;
        }

        @Override
        public int dt(Compound x) {
            if (!x.op().temporal) return DTERNAL;

            int dt = x.dt();
            return (dt == XTERNAL) ? dtIfXternal : dt;
        }
    }

}
