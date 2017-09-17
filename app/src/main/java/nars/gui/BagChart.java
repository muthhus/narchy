package nars.gui;

import com.jogamp.opengl.GL2;
import jcog.pri.Prioritized;
import org.jetbrains.annotations.NotNull;
import spacegraph.widget.meter.TreeChart;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * Created by me on 6/29/16.
 */
abstract public class BagChart<X extends Prioritized> extends TreeChart<X> implements BiConsumer<X, TreeChart.ItemVis<X>> {

    final AtomicBoolean busy = new AtomicBoolean(false);
    public final @NotNull Iterable<? extends X> input;

    public void update() {
        if (busy.compareAndSet(false, true)) {
            try {
                update(1f, 1f, input, this, ii -> {
                    return ii != null ? newItem(ii) : null;
                });
            } finally {
                busy.set(false);
            }
        }
    }

    @NotNull protected ItemVis<X> newItem(@NotNull X i) {
        return new ItemVis<>(i, label(i, 24));
    }


    //TODO
//    public BagChart(@NotNull Bag<X> b) {
//        this(b, -1);
//    }

    public BagChart(@NotNull Iterable<X> b) {
        super();
        this.input = b;
        update();
    }

    @Override
    protected void paint(GL2 gl) {
        if (busy.compareAndSet(false,true)) {
            try {
                super.paint(gl);
            } finally {
                busy.set(false);
            }
        }
    }


    protected static <X> String label(@NotNull X i, int MAX_LEN) {
        String s = i.toString();
        if (s.length() > MAX_LEN)
            s = s.substring(0, MAX_LEN);
        return s;
    }

//    @Override
//    public void accept(X x, ItemVis<X> y) {
//        float p = x.priElseZero();
//        y.update(p, p, 0, 1f);
//    }
}
