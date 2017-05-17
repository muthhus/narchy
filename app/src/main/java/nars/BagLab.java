package nars;

import jcog.Util;
import jcog.bag.Bag;
import jcog.bag.impl.HijackBag;
import jcog.bag.impl.hijack.DefaultHijackBag;
import jcog.pri.PLink;
import jcog.pri.PriMerge;
import jcog.pri.RawPLink;
import nars.gui.HistogramChart;
import nars.gui.Vis;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.math.Color3f;
import spacegraph.render.Draw;
import spacegraph.widget.meter.MatrixView;
import spacegraph.widget.slider.FloatSlider;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static jcog.bag.Bag.BagCursorAction.Next;
import static spacegraph.layout.Grid.col;
import static spacegraph.layout.Grid.row;

/**
 * Created by me on 11/29/16.
 */
public class BagLab  {

    public static final int BINS = 64;

    int histogramResetPeriod = 64;
    int iteration = 0;

    static final Bag.BagCursorAction sampling = Next;

    final Bag<Integer,PLink<Integer>> bag;
    private final List<FloatSlider> inputSliders;
    private final int uniques;

    double[] selectionHistogram = new double[BINS];

    public BagLab(Bag<Integer,PLink<Integer>> bag) {
        super();
        this.bag = bag;

        this.uniques = bag.capacity()*2;

        int inputs = 10;
        inputSliders = $.newArrayList(inputs);
        for (int i = 0; i < inputs; i++)
            inputSliders.add(new FloatSlider(0.5f, 0, 1));


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
                ),
                hijackVis((HijackBag)bag)
        );
    }

    private Surface hijackVis(HijackBag bag) {
        int c = bag.capacity();
        int w = (int) Math.ceil(Math.sqrt(1 + c));
        int h = c / w;
        return new MatrixView(w, h, (x,y, gl) -> {
            try {
                Object m = bag.map.get();
                float p = -1;
                if (m != null) {
                    int i = y * w + x;
                    if (i < c) {

                        Object val = ((AtomicReferenceArray) m).get(i);
                        if (val != null)
                            p = bag.priSafe(val, 0);
                    }
                }
                Draw.colorPolarized(gl, p);
            } catch (Exception e) {

            }
            return 0;
        } );

    }


    public static void main(String[] arg) {
        DefaultHijackBag<Integer> h = new DefaultHijackBag<>(
                PriMerge.plus, 1024, 8);
        BagLab bagLab = new BagLab(
                //new CurveBag(256, plusBlend, new XorShift128PlusRandom(1), new HashMap())
                h
        );

        SpaceGraph.window(
            bagLab.surface(), 1200, 800);

        long delayMS = 30;
        //new Thread(()->{
            while (true) {
                bagLab.update();
                Util.sleep(delayMS);
            }
        //}).start();
    }


    private synchronized void update() {



        int inputRate = 20;
        for (int j = 0; j < inputRate; j++) {
            int n = inputSliders.size();
            for (int i = 0; i < n; i++) {
                if (Math.random() < inputSliders.get(i).value()) {
                    float p = (i + (float) Math.random()) / (n - 1);
                    //float q = (float)Math.random(); //random quality
                    bag.put(new RawPLink<>((int) Math.floor(Math.random() * uniques), p));
                }
            }
        }

        bag.commit();


        int bins = selectionHistogram.length;
        float sampleBatches = 1;
        int batchSize = 32;

        if (iteration++ % histogramResetPeriod == 0)
            Arrays.fill(selectionHistogram, 0);

        //System.out.println(bag.size());

        List<PLink<Integer>> sampled = $.newArrayList(1024);
        for (int i = 0; i < (int)sampleBatches ; i++) {
            sampled.clear();

            bag.sample(batchSize, sampling, (v) -> {
                //System.out.println(h + " " + v);
                sampled.add(v);
            });

            //BLink<Integer> sample = bag.sample();
            for (PLink<Integer> sample : sampled) {
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
