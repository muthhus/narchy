package nars.nal.meta.pre;

import nars.concept.ConceptProcess;
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
        public boolean booleanValueOf(PremiseMatch m) {
            return beliefBeforeOrDuringTask(m.premise);
        }

    };

    /** task is before or simultaneous with belief which follows (T ... B) */
    public static final events afterOrEternal = new events() {

        @Override
        public String toString() {
            return "afterOrEternal";
        }

        @Override
        public boolean booleanValueOf(PremiseMatch m) {
            ConceptProcess p = m.premise;
            return p.isEternal() || beliefBeforeOrDuringTask(p);
        }
    };

    private static boolean beliefBeforeOrDuringTask(ConceptProcess p) {

        return p.isEvent()
               &&
               (p.belief().occurrence() - p.task().occurrence()) >= 0;
    }



}
