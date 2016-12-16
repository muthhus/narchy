package nars.budget;

import nars.NAR;
import nars.Param;
import nars.concept.Concept;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 12/16/16.
 */
abstract public class Activation {

    @NotNull
    public final Concept src;
    public final MutableFloat linkOverflow = new MutableFloat(0);
    @NotNull
    protected final NAR nar;
    protected final float minScale; //cut-off limit for recursive spread
    protected final Budgeted in;

    public Activation(@NotNull NAR nar, @NotNull Budgeted in, float scale, @NotNull Concept src) {
        this.nar = nar;
        this.in = in;
        this.minScale = Param.BUDGET_EPSILON / (scale * in.pri());
        this.src = src;
    }
}
