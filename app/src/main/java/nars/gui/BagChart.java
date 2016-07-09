package nars.gui;

import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import com.jogamp.opengl.GL2;
import nars.bag.Bag;
import nars.concept.Concept;
import nars.link.BLink;
import nars.nar.Default;
import nars.term.atom.Atomic;
import spacegraph.Facial;
import spacegraph.SpaceGraph;
import spacegraph.layout.treechart.ItemVis;
import spacegraph.layout.treechart.TreemapChart;
import spacegraph.obj.CrosshairSurface;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * Created by me on 6/29/16.
 */
public class BagChart<X> extends TreemapChart<BLink<X>> implements BiConsumer<BLink<X>, ItemVis<BLink<X>>> {


    private static long now;
    final AtomicBoolean busy = new AtomicBoolean(false);

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
        BagChart<Concept> tc = new BagChart<Concept>(d.core.concepts, count, 1400, 800) {
            @Override
            public void accept(BLink<Concept> x, ItemVis<BLink<Concept>> y) {
                float p = x.pri();
                float ph = 0.25f + 0.75f * p;


                Concept c = x.get();
                float r, g, b;
                if (c instanceof Atomic) {
                    r = g = b = ph * 0.5f;
                } else {
                    float belief = c.hasBeliefs() ? c.beliefs().top(now).conf() : 0f;
                    float goal = c.hasGoals() ? c.goals().top(now).conf() : 0f;
                    r = 0;
                    g = belief;
                    b = goal;
                }

                y.update(p, r, g, b);


            }
        };

        d.onFrame(xx -> {
            now = xx.time();
            if (tc.busy.compareAndSet(false, true)) {
                tc.update();
            }
        });

        SpaceGraph<VirtualTerminal> s = new SpaceGraph<>();
        s.show(1400, 800);


        s.add(new Facial(tc));
        s.add(new Facial(new CrosshairSurface(s)));
    }

    public void update() {
        update(width, height);
    }

    private final Bag<X> bag;

    public BagChart(Bag<X> b, int limit, float w, float h) {
        super();
        this.bag = b;
        this.limit = limit;
        update(w, h);
    }

    @Override
    protected void paint(GL2 gl) {
        busy.set(false);
        super.paint(gl);
    }

    protected void update(double w, double h) {

        update(w, h, bag.size(), bag, this, i -> new ItemVis<>(i, label(i.get(), 16) ));
    }

    protected static <X> String label(X i, int MAX_LEN) {
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
