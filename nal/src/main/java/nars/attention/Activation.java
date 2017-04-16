package nars.attention;

import jcog.pri.Prioritized;
import nars.NAR;
import nars.Param;
import nars.concept.Concept;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 12/16/16.
 */
abstract public class Activation<B extends Prioritized> {

    @NotNull
    public final Concept origin;
    final MutableFloat linkOverflow = new MutableFloat(0);

    @NotNull
    protected final NAR nar;
    final float minScale; //cut-off limit for recursive spread
    protected final B in;

    public Activation(@NotNull B in, float scale, @NotNull Concept origin, NAR nar) {
        this.nar = nar;
        this.in = in;
        this.minScale = Param.BUDGET_EPSILON / (scale * in.pri());
        this.origin = origin;

    }
}
