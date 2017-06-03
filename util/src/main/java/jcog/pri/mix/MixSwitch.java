package jcog.pri.mix;

import jcog.pri.Priority;

import java.util.function.Consumer;
import java.util.function.ToIntFunction;

/** a switch-like mix where an input
 * from all input channels are classified
 * to exactly one destination.
 *
 */
public class MixSwitch<Y extends Priority> extends Mix<String,Y> implements Consumer<Y> {


    public final PSink[] outs;

    public final ToIntFunction<Y> selector;

    public MixSwitch(Consumer<Y> target, ToIntFunction<Y> selector, String... outs) {
        super(target);

        int i = 0;
        this.outs = new PSink[outs.length];
        for (String s : outs) {
            (this.outs[i++] = stream(s)).setValue(1f);
        }

        this.selector = selector;
    }

    @Override
    public void accept(Y y) {
        PSink[] o = outs;
        if (o!=null) {
            int c = selector.applyAsInt(y);
            if (c != -1) {
                outs[c].accept(y);
            }
        }
    }

}
