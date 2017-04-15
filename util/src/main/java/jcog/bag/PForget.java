package jcog.bag;

import org.jetbrains.annotations.NotNull;

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

    @Override
    public void accept(@NotNull X b) {
        b.priSub(avgToBeRemoved);
//        float p = b.priSafe(-1);
//        if (p > 0) {
//            b.priMult(gain);
//        }
    }

}