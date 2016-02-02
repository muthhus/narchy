package nars.nal.meta.pre;

import nars.nal.meta.AtomicBooleanCondition;
import nars.nal.meta.PremiseMatch;

/**
 * True if the premise task and belief are both non-eternal events
 */
abstract public class events extends AtomicBooleanCondition<PremiseMatch> {

    /** task is before or simultaneous with belief which follows (T ... B) */
    public static final events after = new events() {

        @Override
        public String toString() {
            return "after";
        }

        @Override
        boolean allow(long taskOcc, long beliefOcc) {
            return (beliefOcc - taskOcc) >= 0;
        }
    };


    @Override
    public final boolean booleanValueOf(PremiseMatch m) {
        return m.premise.isEvent() && allow(m.premise.task().occurrence(), m.premise.belief().occurrence());
    }

    abstract boolean allow(long taskOcc, long beliefOcc);
}
