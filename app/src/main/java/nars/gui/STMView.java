package nars.gui;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.learn.gng.NeuralGasNet;
import jcog.learn.gng.impl.Centroid;
import jcog.pri.Pri;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.bag.BagClustering;
import nars.control.DurService;
import nars.gui.graph.ConceptWidget;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.render.Draw;
import spacegraph.space.ListSpace;

import java.util.function.Function;


/**
 * Created by me on 7/20/16.
 */
public class STMView {


//    private final BagChart inputBagChart;


    public static class GNGVis3D extends ListSpace<Task,ConceptWidget> {

        private final ConceptWidget[] centroids;
        protected int limit = -1;
        final NeuralGasNet net;

        /** local copy of the range data, buffering it because it changes rapidly */
        protected double[] range;

        protected float sx = 1, sy = 1;

        Function<Centroid, ConceptWidget> centroidVis = c -> {
            return new ConceptWidget($.the(c.id));
        };

        public GNGVis3D(NAR nar, BagClustering b) {
            super();//nar, null, b.bag.capacity(), b.bag.capacity(), 8, 16);

            this.net = b.net;


            this.centroids = Util.map(centroidVis, ConceptWidget[]::new, b.net.centroids);

            range = new double[net.dimension*2];

            DurService.build(nar, this::commit);
        }

        private void commit() {

            updateRange();

            float maxDimNorm = (float) (range[1]-range[0]); //start time span

            int C = net.centroids.length;
            for (int i = 0, coordLength = C; i < coordLength; i++) {
                Centroid ii = net.centroids[i];

                float e = (float) ii.localError();
                //draw(gl, centroidHue(ii.id), 0.1f, ii.getDataRef(), i, 0.5f + 0.5f/(1f+ e));
            }
        }

        static float centroidHue(int id) {
            return (id % 8) / 8f;
        }

        protected void updateRange() {
            System.arraycopy(net.rangeMinMax.read(), 0, range, 0, range.length);
            //override freq and conf dimensions
            range[4] = range[6] = 0;
            range[5] = range[7] = 1;
        }

        protected void draw(GL2 gl, float hue, float w, double[] c, int centroid, float v) {
            //float p = n.priSum()/size;
            float xs = sx * r(0, c[0]);
            float xe = sx * r(1, c[1]);
            float y = sy * r(2, c[2]);
            float z = r(3, c[3]);

            //TODO HACK dimension c > 1 ignored


            Draw.hsb( gl, hue, 0.5f, z, z *1f/(1f+w));
            Draw.rect(gl, xs - w / 2, y - w / 2, w+(xe-xs), w);
        }

        private float r(int d, double v) {
            double min = range[d * 2];
            double max = range[d * 2 + 1];
            float u = Util.equals(Math.abs(max-min), 0, Pri.EPSILON) ?
                    ((float) v) //as-is
                    :
                    (float) ((v - min) / (max - min));
            float scale = 4f;
            return u;
        }

    }
    public static class GNGVis extends Surface {

        protected int limit = -1;
        final NeuralGasNet net;

        /** local copy of the range data, buffering it because it changes rapidly */
        protected double[] range;

        protected float sx = 1, sy = 1;

        public GNGVis(NeuralGasNet net) {
            this.net = net;
            range = new double[net.dimension*2];
        }

        @Override
        protected void paint(GL2 gl) {
            super.paint(gl);

            updateRange();

            float maxDimNorm = (float) (range[1]-range[0]); //start time span

            int C = net.centroids.length;
            for (int i = 0, coordLength = C; i < coordLength; i++) {
                Centroid ii = net.centroids[i];

                float e = (float) ii.localError();
                draw(gl, centroidHue(ii.id), 0.1f, ii.getDataRef(), i, 0.5f + 0.5f/(1f+ e));
            }
        }

        static float centroidHue(int id) {
            return (id % 8) / 8f;
        }

        protected void updateRange() {
            System.arraycopy(net.rangeMinMax.read(), 0, range, 0, range.length);
            //override freq and conf dimensions
            range[4] = range[6] = 0;
            range[5] = range[7] = 1;
        }

        protected void draw(GL2 gl, float hue, float w, double[] c, int centroid, float v) {
            //float p = n.priSum()/size;
            float xs = sx * r(0, c[0]);
            float xe = sx * r(1, c[1]);
            float y = sy * r(2, c[2]);
            float z = r(3, c[3]);

            //TODO HACK dimension c > 1 ignored


            Draw.hsb( gl, hue, 0.5f, z, z *1f/(1f+w));
            Draw.rect(gl, xs - w / 2, y - w / 2, w+(xe-xs), w);
        }

        private float r(int d, double v) {
            double min = range[d * 2];
            double max = range[d * 2 + 1];
            float u = Util.equals(Math.abs(max-min), 0, Pri.EPSILON) ?
                    ((float) v) //as-is
                    :
                    (float) ((v - min) / (max - min));
            float scale = 4f;
            return u;
        }

    }

    public static class BagClusterVis extends GNGVis {

        private final BagClustering<?> bag;
        private final NAR nar;

        public BagClusterVis(NAR nar, BagClustering b) {
            super(b.net);
            this.bag = b;
            this.nar = nar;
        }

        @Override
        protected void updateRange() {
            super.updateRange();
            final int DURS = 8;
            long now = nar.time();
            int dur = nar.dur();
            range[0] = range[2] = now - dur * DURS;
            range[1] = range[3] = now + dur * DURS;

//            sy = 4;
//            sx = 0.5f;
        }

        @Override
        protected void paint(GL2 gl) {
            super.paint(gl);
            bag.bag.forEach(x -> {
                draw(gl, centroidHue(x.centroid), 0.05f * x.priElseZero(), x.coord, x.centroid, x.priElseZero());
            });
        }
    }
//    public STMView(BagClustering c) {
//        super();
//        this.c = c;
//
//        final float maxVolume = 64;
//
////        s.add(new Ortho(
////                    grid(
////                        inputBagChart = new BagChart<Task>(c.bag, -1) {
////                            @Override
////                            public void accept(Task task, ItemVis<Task> taskItemVis) {
////
////                            }
////
////                            @NotNull
////                            @Override
////                            protected ItemVis<Task> newItem(@NotNull BLink<Task> i) {
////                                @Nullable Task ii = i.get();
////                                String label;
////                                if (ii != null)
////                                    label =  ii.toStringWithoutBudget();
////                                else
////                                    label = "?";
////
////                                return new ItemVis<>(i, label(label, 16));
////                            }
////                        },
//
//
////                        new Stacking(
////                            new ConsoleSurface(new TopicTerminal<Task>(
////                                stm.generate,
////                                    (Task t) -> t.toStringWithoutBudget(),
////                                    (Task t) -> {
////                                        float p = t.pri() * 0.5f + 0.5f;
////                                        float c = t.conf() * 0.5f + 0.5f;
////                                        float f = t.freq();
////                                        return TextColor.rgb((1f - f) * p * c, f * p * c, p * t.qua());
////                                    },
////                                    null,  //(Task t) -> TextColor.hsb(t.term().volume()/maxVolume, 0.8f, 0.25f),
////                                    50, 24
////                            )),
////                            new BubbleChart()
////                        )
////                )).maximize());
////
////        c.nar.onFrame(xx -> {
////            update();
////        });
//
//
//    }

    public static void show(NAR n, BagClustering c, int w, int h) {
//        SpaceGraph<VirtualTerminal> s = new SpaceGraph<>() {
//            @Override
//            public void init(GL2 gl) {
//                super.init(gl);
//                //window.setUndecorated(true);
//            }
//        };
//        s.show(w, h);

        //STMView sv = new STMView(stm);

        GNGVis view = new BagClusterVis(n, c);
        SpaceGraph.window(view, w, h);
        //DurService.build(n, view::update);
    }

//    public void update() {
//        inputBagChart.update();
//
//        synchronized(state) {
//            state.update(c.net).normalize();
//        }
//
//    }


}