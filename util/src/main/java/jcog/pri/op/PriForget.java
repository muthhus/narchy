package jcog.pri.op;

import jcog.pri.Priority;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * decreases priority at a specified rate which is diminished in proportion to a budget's quality
 * so that high quality results in slower priority loss
 */
public class PriForget<P extends Priority> implements Consumer<P> {

    public static final float DEFAULT_TEMP = 0.5f;
    public final float avgToBeRemoved;

    public PriForget(float avgToBeRemoved) {
        this.avgToBeRemoved = avgToBeRemoved;
    }


    /**
     * temperature parameter, in the range of 0..1.0 controls the target average priority that
     * forgetting should attempt to cause.
     * <p>
     * higher temperature means faster forgetting allowing new items to more easily penetrate into
     * the bag.
     * <p>
     * lower temperature means old items are forgotten more slowly
     * so new items have more difficulty entering.
     *
     * @return the update function to apply to a bag
     */
    @Nullable
    public static Consumer forget(int s, int c, float p, float m, float temperature, float priEpsilon, FloatToObjectFunction<Consumer> f) {

        if ((s > 0) && (p > 0)) {

//        float estimatedExcess = p/(m+p); //(m + p) - (c * (1f - temperature));
//        if (estimatedExcess > 0) {
//            float presentAndFutureExcess = estimatedExcess;
            //* 2f; /* x 2 to apply to both the existing pressure and estimated future pressure */
            float perMember = temperature * (p) / c;
            if (perMember >= priEpsilon)
                return f.valueOf(perMember);
//        }
        }
        return null;
    }

    @Override
    public void accept(@NotNull Priority b) {
        b.priSub(avgToBeRemoved);

//        b.priSub(avgToBeRemoved
//            ,0.5f //50% retained
////            //,(1f - b.priElseZero())  //retained inversely proportional to existing pri, so higher burden on higher priority
////            //,0.5f * (1f - b.priElseZero())  //retained inversely proportional to existing pri, so higher burden on higher priority
//        );
    }

}