package nars.nal.meta.pre;

import nars.concept.ConceptProcess;
import nars.nal.meta.AtomicBooleanCondition;
import nars.nal.meta.PremiseEval;
import org.jetbrains.annotations.NotNull;

/**
 * True if the premise task and belief are both non-eternal events
 */
abstract public class events extends AtomicBooleanCondition<PremiseEval> {

    /** task is before or simultaneous with belief which follows (T ... B) */
    public static final events after = new events() {

        @Override
        public String toString() {
            return "after";
        }

        @Override
        public boolean booleanValueOf(@NotNull PremiseEval m) {
            return beliefBeforeOrDuringTask(m.currentPremise);
        }

    };

    /** task is before or simultaneous with belief which follows (T ... B) */
    public static final events afterOrEternal = new events() {

        @Override
        public String toString() {
            return "afterOrEternal";
        }

        @Override
        public boolean booleanValueOf(@NotNull PremiseEval m) {
            ConceptProcess p = m.currentPremise;
            return p.isEternal() || beliefBeforeOrDuringTask(p);
        }
    };

    private static boolean beliefBeforeOrDuringTask(@NotNull ConceptProcess p) {

        return p.isEvent()
               &&
               (p.belief().occurrence() - p.task().occurrence()) >= 0;
    }



}
