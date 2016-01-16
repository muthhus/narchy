package nars.nal.meta.pre;

import nars.nal.PremiseMatch;
import nars.nal.meta.AtomicBooleanCondition;
import nars.nal.nal7.Tense;
import nars.term.compound.Compound;
import org.jetbrains.annotations.NotNull;

/** applies dt to the derived term according to premise terms */
public abstract class dt extends AtomicBooleanCondition<PremiseMatch> {


    public static final dt avg = new dt() {
        @Override protected boolean computeDT(Compound t, int tt, Compound b, int bt, @NotNull PremiseMatch m) {
            int avg = (tt + bt) / 2;
            //float diff = Math.abs(at - bt)/avg;
            m.tDelta.set(-avg);
            return true;
        }

        @Override public String toString() {
            return "dt(avg)";
        }
    };
    public static final dt sum = new dt() {
        @Override protected boolean computeDT(Compound t, int tt, Compound b, int bt, @NotNull PremiseMatch m) {
            int sum = (tt + bt);
            m.tDelta.set(sum);
            return true;
        }

        @Override public String toString() {
            return "dt(sum)";
        }
    };

    public static final dt sumNeg = new dt() {
        @Override protected boolean computeDT(Compound t, int tt, Compound b, int bt, @NotNull PremiseMatch m) {
            int sum = (tt + bt);
            m.tDelta.set(-sum);
            return true;
        }

        @Override public String toString() {
            return "dt(sumNeg)";
        }
    };
    public static final dt task = new dt() {
        @Override protected boolean computeDT(Compound t, int tt, Compound b, int bt, @NotNull PremiseMatch m) {
            m.tDelta.set(tt);
            return true;
        }

        @Override public String toString() {
            return "dt(task)";
        }
    };
    public static final dt exact = new dt() {
        @Override protected boolean computeDT(Compound t, int tt, Compound b, int bt, @NotNull PremiseMatch m) {
            if (tt == bt) {
                m.tDelta.set(tt);
                return true;
            }
            return false;
        }

        @Override public String toString() {
            return "dt(exact)";
        }
    };
    public static final dt opposite = new dt() {
        @Override protected boolean computeDT(Compound t, int tt, Compound b, int bt, @NotNull PremiseMatch m) {
            if (tt == -bt) {
                m.tDelta.set(tt);
                return true;
            }
            return false;
        }

        @Override public String toString() {
            return "dt(opposite)";
        }
    };

    /** belief minus task */
    public static final dt bmint = new dt() {
        @Override protected boolean computeDT(Compound t, int tt, Compound b, int bt, @NotNull PremiseMatch m) {
            m.tDelta.set(bt- tt);
            return true;
        }

        @Override public String toString() {
            return "dt(bmint)";
        }
    };
    /** task minus belief */
    public static final dt tminb = new dt() {
        @Override protected boolean computeDT(Compound t, int tt, Compound b, int bt, @NotNull PremiseMatch m) {
            m.tDelta.set(tt - bt);
            return true;
        }

        @Override public String toString() {
            return "dt(tminb)";
        }
    };



    protected dt() {

    }

    @NotNull
    @Override
    abstract public String toString();

    @Override
    public boolean booleanValueOf(@NotNull PremiseMatch m) {
        Compound a = m.premise.getTaskTerm();
        int at = a.t();

        Compound b = m.premise.getBeliefCompound();
        if (b == null) return false;
        int bt = b.t();

        if (at == Tense.ITERNAL) {
            if (bt == Tense.ITERNAL)
                return true; //both atemporal, nothing needs done just allow
            else
                return false;  //mismatch

        } else if (bt == Tense.ITERNAL) {
            return false; //mismatch
        } else {
            return computeDT(a, at, b, bt, m);
        }
    }

    protected abstract boolean computeDT(Compound t, int tt, Compound b, int bt, PremiseMatch m);
}
