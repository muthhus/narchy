package jcog.pri;

import jcog.bag.Bag;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * decreases priority at a specified rate which is diminished in proportion to a budget's quality
 * so that high quality results in slower priority loss
 */
public class PForget<X extends Priority> implements Consumer<X> {

    public final float avgToBeRemoved;

    public PForget(float avgToBeRemoved) {
        this.avgToBeRemoved = avgToBeRemoved;
    }

    @Nullable
    public static <X> Consumer<X> forget(int s, int c, float p, float m, FloatToObjectFunction<Consumer<X>> f) {
        return Bag.forget(s, c, p, m, 0.5f, Priority.EPSILON_DEFAULT, f);
    }

    @Override
    public void accept(@NotNull X b) {
        b.priSub(avgToBeRemoved
            ,0.5f //50% retained
            //,(1f - b.priSafe(0))  //retained inversely proportional to existing pri, so higher burden on higher priority
            //,0.5f * (1f - b.priSafe(0))  //retained inversely proportional to existing pri, so higher burden on higher priority
        );
    }

}