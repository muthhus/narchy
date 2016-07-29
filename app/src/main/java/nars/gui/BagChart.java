package nars.gui;

import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import com.jogamp.opengl.GL2;
import nars.bag.Bag;
import nars.concept.Concept;
import nars.link.BLink;
import nars.nar.Default;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spacegraph.Facial;
import spacegraph.SpaceGraph;
import spacegraph.layout.TreeChart;
import spacegraph.obj.CrosshairSurface;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * Created by me on 6/29/16.
 */
public class BagChart<X> extends TreeChart<BLink<X>> implements BiConsumer<BLink<X>, TreeChart.ItemVis<BLink<X>>> {


    protected long now;
    final AtomicBoolean busy = new AtomicBoolean(false);
    private final Bag<X> bag;

    public static void main(String[] args) {
        Default d = new Default();
        d.input("(a --> b). (b --> c).  (c --> d).");

        show(d);

        d.loop(5f);

    }

    public static void show(Default d) {
        show(d, -1);
    }

    public static void show(Default d, int count) {
        BagChart<Concept> tc = newBagChart(d, count);
        SpaceGraph<VirtualTerminal> s = new SpaceGraph<>();


        s.show(1400, 800);


        s.add(new Facial(tc).maximize());
        s.add(new Facial(new CrosshairSurface(s)));
    }

    public static BagChart<Concept> newBagChart(final Default d, final int count) {
        BagChart<Concept> tc = new BagChart<Concept>(d.core.concepts, count) {
            @Override
            public void accept(BLink<Concept> x, ItemVis<BLink<Concept>> y) {
                float p = x.pri();
                float ph = 0.25f + 0.75f * p;


                Concept c = x.get();
                float r, g, b;
                if (c instanceof Atomic) {
                    r = g = b = ph * 0.5f;
                } else {
                    float belief = c.hasBeliefs() ? c.beliefs().truth(now).conf() : 0f;
                    float goal = c.hasGoals() ? c.goals().truth(now).conf() : 0f;
                    r = 0;
                    g = belief;
                    b = goal;
                }

                y.update(p, r, g, b);


            }
        };

        d.onFrame(xx -> {

            //if (s.window.isVisible()) {
            tc.now = xx.time();
            tc.update();
            //}
        });

        return tc;
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
        return new ItemVis<>(i, label(i.get(), 16));
    }


    public BagChart(@NotNull Bag<X> b, int limit) {
        super();
        this.bag = b;
        this.limit = limit;
        update();
    }

    @Override
    protected void paint(GL2 gl) {
        busy.set(false);
        super.paint(gl);
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
        y.update(p, ph, ph * x.dur(), ph * x.qua());
    }
}
