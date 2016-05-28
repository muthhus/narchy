package nars.nar.util;

import nars.Global;
import nars.NAR;
import nars.nal.Reasoner;
import nars.data.Range;
import nars.nal.Deriver;
import nars.nal.meta.PremiseEval;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

/**
 * single-threaded reasoner, including:
 *      premise generator and processor
 *          (re-uses the same result collection buffer)
*
 */
public class DefaultReasoner extends Reasoner {



    /**
     * derived tasks with truth confidence lower than this value are discarded.
     */
    @Range(min = 0, max = 1f)
    public final @NotNull MutableFloat confMin;

//        public DefaultPremiseGenerator(@NotNull NAR nar, Deriver deriver) {
//            /** the resutls buffer should probably be a Set because the derivations may duplicate */
//            this(nar, deriver, Global.newHashSet(64));
//        }

    public DefaultReasoner(@NotNull NAR nar, @NotNull Deriver deriver) {
        super(nar, new PremiseEval(nar.index, nar.random, deriver));

        this.confMin = new MutableFloat(Global.TRUTH_EPSILON);
    }

    /**
     * update derivation parameters (each frame)
     */
    @Override public final void frame(@NotNull NAR nar) {
        matcher.setMinConfidence(confMin.floatValue());
    }




}
