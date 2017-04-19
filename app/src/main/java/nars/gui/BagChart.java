package nars.gui;

import com.jogamp.opengl.GL2;
import jcog.pri.PLink;
import nars.Narsese;
import nars.concept.Concept;
import nars.nar.Default;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;
import spacegraph.widget.meter.TreeChart;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * Created by me on 6/29/16.
 */
public class BagChart<X> extends TreeChart<PLink<X>> implements BiConsumer<PLink<X>, TreeChart.ItemVis<PLink<X>>> {


    //protected long now;
    final AtomicBoolean busy = new AtomicBoolean(false);
    private final @NotNull Iterable<PLink<X>> input;

    public static void main(String[] args) throws Narsese.NarseseException {
        Default d = new Default(1024,50, 2);
        d.input("(a --> b). (b --> c).  (c --> d).  (d-->e)! :|: ");

        BagChart<Concept> tc = new Vis.ConceptBagChart(d.focus(), 1024, d);


        SpaceGraph.window(tc, 800, 600);

        d.loop(30f);

    }

    public void update() {
        if (busy.compareAndSet(false, true)) {
            update(1f, 1f, input, this, i -> {
                @Nullable X ii = i.get();
                return ii != null ? newItem(i) : null;
            });
            busy.set(false);
        }
    }

    @NotNull protected ItemVis<PLink<X>> newItem(@NotNull PLink<X> i) {
        return new ItemVis<>(i, label(i.get(), 24));
    }


    //TODO
//    public BagChart(@NotNull Bag<X> b) {
//        this(b, -1);
//    }

    public BagChart(@NotNull Iterable<PLink<X>> b, int limit) {
        super();
        this.input = b;
        this.limit = limit;
        update();
    }

    @Override
    protected void paint(GL2 gl) {
        busy.set(true);
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
    public void accept(PLink<X> x, ItemVis<PLink<X>> y) {
        float p = x.pri();
        y.update(0, p, 0, 1f);
    }
}
