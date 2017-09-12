package nars.term.transform;

import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.XTERNAL;

abstract public class Retemporalize implements CompoundTransform {


    @Override
    public final boolean testSuperTerm(@NotNull Compound c) {
        return (c.hasAny(Op.TemporalBits));
    }

    @Nullable
    @Override
    public final Term apply(@Nullable Compound parent, @NotNull Term x) {
        if (x.hasAny(Op.TemporalBits)) {
            return x.transform(dt(x), this);
        } else {
            return x;
        }
    }

    abstract public int dt(@NotNull Term x);

    @Deprecated
    public static class RetemporalizeAll extends Retemporalize {

        final int targetDT;

        public RetemporalizeAll(int targetDT) {
            this.targetDT = targetDT;
        }

        @Override
        public int dt(@NotNull Term x) {
            return targetDT;
        }
    }

    @Deprecated
    public static class RetemporalizeNonXternal extends Retemporalize {

        final int dtIfXternal;

        public RetemporalizeNonXternal(int dtIfXternal) {
            this.dtIfXternal = dtIfXternal;
        }

        @Override
        public int dt(@NotNull Term x) {
            int dt = x.dt();
            return (dt == XTERNAL) ? dtIfXternal : dt;
        }
    }

}
