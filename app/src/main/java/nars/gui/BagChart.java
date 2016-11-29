package nars.gui;

import com.jogamp.opengl.GL2;
import nars.bag.Bag;
import nars.link.BLink;
import nars.nar.Default;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;
import spacegraph.layout.TreeChart;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * Created by me on 6/29/16.
 */
public class BagChart<X> extends TreeChart<BLink<X>> implements BiConsumer<BLink<X>, TreeChart.ItemVis<BLink<X>>> {


    //protected long now;
    final AtomicBoolean busy = new AtomicBoolean(false);
    private final Bag<X> bag;

    public static void main(String[] args) {
        Default d = new Default(1024,50,2,2);
        d.input("(a --> b). (b --> c).  (c --> d).  (d-->e)! :|: ");

        SpaceGraph.window(Vis.conceptsTreeChart(d, 1024), 800, 600);

        d.loop(30f);

    }

    public void update() {
        if (busy.compareAndSet(false, true)) {
            update(1f, 1f, bag.size(), bag, this, i -> {
                @Nullable X ii = i.get();
                return ii != null ? newItem(i) : null;
            });
        }
    }

    @NotNull protected ItemVis<BLink<X>> newItem(@NotNull BLink<X> i) {
        return new ItemVis<>(i, label(i.get(), 13));
    }


    public BagChart(@NotNull Bag<X> b, int limit) {
        super();
        this.bag = b;
        this.limit = limit;
        update();
    }

    @Override
    protected void paint(GL2 gl) {
        super.paint(gl);
        busy.set(false);
    }


    protected static <X> String label(@NotNull X i, int MAX_LEN) {
        String s = i.toString();
        if (s.length() > MAX_LEN)
            s = s.substring(0, MAX_LEN);
        return s;
    }

    @Override
    public void accept(BLink<X> x, ItemVis<BLink<X>> y) {
        float p = x.pri();
        float ph = 0.25f + 0.75f * p;
        float q = x.qua();
        y.update(p, ph, ph * (1f - q), ph * q);
    }
}
