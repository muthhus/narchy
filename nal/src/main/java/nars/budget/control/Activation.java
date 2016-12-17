package nars.budget.control;

import nars.NAR;
import nars.Param;
import nars.budget.Budgeted;
import nars.concept.Concept;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 12/16/16.
 */
abstract public class Activation {

    @NotNull
    public final Concept src;
    final MutableFloat linkOverflow = new MutableFloat(0);

    @NotNull
    protected final NAR nar;
    final float minScale; //cut-off limit for recursive spread
    protected final Budgeted in;

    public Activation(@NotNull Budgeted in, float scale, @NotNull Concept src, @NotNull NAR nar) {
        this.nar = nar;
        this.in = in;
        this.minScale = Param.BUDGET_EPSILON / (scale * in.pri());
        this.src = src;
    }
}
