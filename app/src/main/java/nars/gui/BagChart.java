package nars.gui;

import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import nars.bag.Bag;
import nars.concept.Concept;
import nars.link.BLink;
import nars.nar.Default;
import spacegraph.Facial;
import spacegraph.SpaceGraph;
import spacegraph.layout.treechart.ItemVis;
import spacegraph.layout.treechart.TreemapChart;

import java.util.function.BiConsumer;

import static spacegraph.layout.treechart.TreemapChart.WeightedString.w;

/**
 * Created by me on 6/29/16.
 */
public class BagChart<X> extends TreemapChart<BLink<X>> implements BiConsumer<BLink<X>, ItemVis<BLink<X>>> {


    public static void main(String[] args) {
        Default d = new Default();
        d.input("(a --> b). (b --> c).  (c --> d).");

        show(d);

        d.loop(5f);

    }

    public static void show(Default d) {
        BagChart<Concept> tc = new BagChart(d.core.concepts, 1400, 800);

        d.onFrame(xx -> {
            tc.update();
        });

        SpaceGraph<VirtualTerminal> s = new SpaceGraph<>();
        s.show(1400, 800);


        s.add(new Facial(tc));
    }

    public void update() {
        update(width, height);
    }

    private final Bag<X> bag;

    public BagChart(Bag<X> b, float w, float h) {
        super();
        this.bag = b;
        update(w, h);
    }

    protected void update(double w, double h) {
        update(w, h, bag.size(), bag, this);
    }

    @Override
    public void accept(BLink<X> x, ItemVis<BLink<X>> y) {
        float p = x.pri();
        float ph = 0.25f + 0.75f * p;
        y.update(x, x.get().toString(), p,
                ph, ph * x.dur(), ph * x.qua());
    }
}
