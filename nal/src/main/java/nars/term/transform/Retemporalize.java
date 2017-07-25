package nars.term.transform;

import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

abstract public class Retemporalize implements CompoundTransform {



    @Override
    public final boolean testSuperTerm(@NotNull Compound c) {
        return (c.hasAny(Op.TemporalBits));
    }

    @Nullable
    @Override
    public final Term apply(@Nullable Compound parent, @NotNull Term term) {
        if (term instanceof Compound && term.hasAny(Op.TemporalBits)) {
            Compound x = (Compound) term;
            return x.transform(dt(x), this);
        }
        return term;
    }

    abstract public int dt(@NotNull Compound x);


    @Deprecated  public static class RetemporalizeNonXternal extends Retemporalize {

        final int dtIfXternal;

        public RetemporalizeNonXternal(int dtIfXternal) {
            this.dtIfXternal = dtIfXternal;
        }

        @Override public int dt(@NotNull Compound x) {
            int dt = x.dt();
            return (dt==DTERNAL||dt==XTERNAL) ? dtIfXternal : dt;
        }
    }

}
