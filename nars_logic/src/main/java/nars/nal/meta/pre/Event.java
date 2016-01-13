package nars.nal.meta.pre;

import nars.Premise;
import nars.nal.PremiseMatch;
import nars.nal.meta.AtomicBooleanCondition;
import nars.nal.nal7.Tense;
import nars.task.Temporal;
import nars.term.compound.Compound;

/**
 * After(%X,%Y) Means that
 * %X is after %Y
 * TODO use less confusing terminology and order convention
 */
abstract public class Event extends AtomicBooleanCondition<PremiseMatch> {

    //public static final Event the = new Event();

    protected Event() {
    }

    @Override
    abstract public String toString();

    @Override
    public boolean booleanValueOf(PremiseMatch m) {
        Premise premise = m.premise;
        if (premise.isEvent()) {
            int tDelta = ((Temporal) premise.getBelief()).tDelta((Temporal) premise.getTask());
            return booleanValueOfEvent(m, tDelta);
        }
        return false;
    }

    /** @param tDelta task->belief delta (positive if belief after task, negative if task after belief) */
    protected abstract boolean booleanValueOfEvent(PremiseMatch m, int tDelta);

    /** belief then task */
    public final static class After extends Event {

        public static final After forward = new After(true);
        public static final After reverse = new After(false);
        private final boolean positive;

        protected After(boolean positive) {
            super();
            this.positive = positive;
        }

        @Override
        protected boolean booleanValueOfEvent(PremiseMatch m, int tDelta) {
            if (tDelta >= 0) {
                m.tDelta.set(positive ? tDelta : -tDelta);
                return true;
            }
            return false;
        }

        @Override public String toString() {
            return "after(" + (positive ? "forward" : "reverse") + ")";
        }
    }

    /** applies dt to the derived term according to premise terms */
    public abstract static class dt extends AtomicBooleanCondition<PremiseMatch> {


        public static final dt avg = new dt() {
            @Override protected boolean computeDT(Compound a, int at, Compound b, int bt, PremiseMatch m) {
                int avg = (at + bt) / 2;
                //float diff = Math.abs(at - bt)/avg;
                m.tDelta.set(-avg);
                return true;
            }

            @Override public String toString() {
                return "dt(avg)";
            }
        };
        public static final dt task = new dt() {
            @Override protected boolean computeDT(Compound a, int at, Compound b, int bt, PremiseMatch m) {
                m.tDelta.set(at);
                return true;
            }

            @Override public String toString() {
                return "dt(task)";
            }
        };


        protected dt() {

        }

        @Override
        abstract public String toString();

        @Override
        public boolean booleanValueOf(PremiseMatch m) {
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

        protected abstract boolean computeDT(Compound a, int at, Compound b, int bt, PremiseMatch m);
    }
//    /** task then/simultaneously belief */
//    public static final class Before extends Event {
//
//        public static final Before forward = new Before(true);
//        public static final Before reverse = new Before(false);
//        private final boolean positive;
//
//        protected Before(boolean pos) {
//            super();
//            this.positive = pos;
//        }
//
//        @Override
//        protected boolean booleanValueOfEvent(PremiseMatch m, int tDelta) {
//            if (tDelta >= 0) {
//                m.tDelta.set(positive ? -tDelta : tDelta);
//                return true;
//            }
//            return false;
//        }
//
//        @Override public String toString() {
//            return "before";
//        }
//    }
}
