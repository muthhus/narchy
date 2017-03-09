package nars;

import jcog.Util;
import jcog.bag.Bag;
import jcog.random.XorShift128PlusRandom;
import nars.bag.impl.BLinkHijackBag;
import nars.budget.BLink;
import nars.budget.BudgetMerge;
import nars.budget.RawBLink;
import nars.gui.HistogramChart;
import nars.gui.Vis;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.math.Color3f;
import spacegraph.space.widget.FloatSlider;

import java.util.Arrays;
import java.util.List;

import static spacegraph.space.layout.Grid.col;
import static spacegraph.space.layout.Grid.row;

/**
 * Created by me on 11/29/16.
 */
public class BagLab  {

    final Bag<Integer,BLink<Integer>> bag;
    private final List<FloatSlider> inputSliders;
    private final int uniques;

    double[] selectionHistogram = new double[16];

    public BagLab(Bag<Integer,BLink<Integer>> bag) {
        super();
        this.bag = bag;

        this.uniques = bag.capacity()*2;

        int inputs = 10;
        inputSliders = $.newArrayList(inputs);
        for (int i = 0; i < inputs; i++)
            inputSliders.add(new FloatSlider(0, 0, 1));


    }

    public Surface surface() {
        return row(
                col(inputSliders),
                //new GridSurface(VERTICAL,
                col(
                    Vis.pane("Bag Selection Distribution (0..1)", new HistogramChart(
                            ()->selectionHistogram, new Color3f(0.5f, 0.25f, 0f), new Color3f(1f, 0.5f, 0.1f))),
                    Vis.pane("Bag Content Distribution (0..1)", new HistogramChart(
                            ()->bag.priHistogram(new double[10]), new Color3f(0f, 0.25f, 0.5f), new Color3f(0.1f, 0.5f, 1f)))
                )
        );
    }


    public static void main(String[] arg) {
        BagLab bagLab = new BagLab(
                //new CurveBag(256, plusBlend, new XorShift128PlusRandom(1), new HashMap())
                new BLinkHijackBag(512, 4, BudgetMerge.maxBlend, new XorShift128PlusRandom(1))
        );

        SpaceGraph.window(
            bagLab.surface(), 800, 800);

        long delayMS = 50;
        new Thread(()->{
            while (true) {
                bagLab.update();
                Util.sleep(delayMS);
            }
        }).start();
    }

    private void update() {



        int n = inputSliders.size();
        for (int i = 0; i < n; i++) {
            if (Math.random() < inputSliders.get(i).value()) {
                float p = (i + (float)Math.random()) / (n - 1);
                float q = (float)Math.random(); //random quality
                bag.put(new RawBLink<>((int) Math.floor(Math.random() * uniques), p, q));
            }
        }

        bag.commit();


        int bins = selectionHistogram.length;
        float sampleBatches = 16;
        int batchSize = 16;
        Arrays.fill(selectionHistogram, 0);
        List<BLink<Integer>> sampled = $.newArrayList();
        for (int i = 0; i < (int)sampleBatches ; i++) {
            sampled.clear();
            bag.sample(batchSize, sampled::add );

            //BLink<Integer> sample = bag.sample();
            for (BLink<Integer> sample : sampled) {
                if (sample != null) {
                    float p = sample.priSafe(0);
                    selectionHistogram[Util.bin(p, bins - 1)]++;
                } else {
                    break;
                }
            }
        }

    }

}
