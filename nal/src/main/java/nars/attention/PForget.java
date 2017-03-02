package nars.attention;

import jcog.bag.Bag;
import jcog.bag.Priority;
import nars.Param;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * decreases priority at a specified rate which is diminished in proportion to a budget's quality
 * so that high quality results in slower priority loss
 */
public class PForget<X extends Priority> implements Consumer<X> {

    public final float gain;

    public PForget(float rate) {
        this.gain = 1f-rate;
    }

    @Override
    public void accept(@NotNull X b) {
        float p = b.priSafe(-1);
        if (p > 0) {
            b.priMult(gain);
        }
    }

}