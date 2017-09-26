package jcog.pri.op;

import jcog.pri.Priority;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * decreases priority at a specified rate which is diminished in proportion to a budget's quality
 * so that high quality results in slower priority loss
 */
public class PriForget<P extends Priority> implements Consumer<P> {

    public static final float FORGET_TEMPERATURE_DEFAULT = 0.1f;

    public final float priFactor;

    public PriForget(float priFactor) {
        this.priFactor = priFactor;
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
    public static Consumer forget(int s, int c, float pressure, float mass, float temperature, float priEpsilon, FloatToObjectFunction<Consumer> f) {

        if ((s > 0) && (pressure > 0) && (c > 0) && temperature > 0) {

            float priFactor =
                    Math.max(0, 1f - Math.min(1f,
                            temperature * (pressure)/(c+(c-s /* free space */)))
                    );
            if (priFactor < 1 - 2 * priEpsilon)
                 return f.valueOf(priFactor);
        }
        return null;
    }

    @Override
    public void accept(P b) {
        b.priMult(priFactor);
        //b.priSub(avgToBeRemoved);

//        b.priSub(avgToBeRemoved
//            ,0.5f //50% retained
////            //,(1f - b.priElseZero())  //retained inversely proportional to existing pri, so higher burden on higher priority
////            //,0.5f * (1f - b.priElseZero())  //retained inversely proportional to existing pri, so higher burden on higher priority
//        );
    }

}