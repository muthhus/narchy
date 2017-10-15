package nars.gui;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.bag.impl.ArrayBag;
import jcog.learn.gng.NeuralGasNet;
import jcog.learn.gng.impl.Centroid;
import jcog.list.FasterList;
import jcog.pri.Pri;
import jcog.pri.VLink;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.bag.BagClustering;
import nars.gui.graph.ConceptWidget;
import nars.term.Termed;
import spacegraph.SimpleSpatial;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.phys.Dynamic;
import spacegraph.render.Draw;
import spacegraph.space.Cuboid;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Created by me on 7/20/16.
 */
public class STMView {


//    private final BagChart inputBagChart;


    public static class GNGVis3D<X extends Termed> extends NARSpace<X, SimpleSpatial<X>> {

        private final ConceptWidget[] centroids;
        private final ArrayBag<X, VLink<X>> bag;

        protected int limit = -1;
        final NeuralGasNet net;

        /**
         * local copy of the range data, buffering it because it changes rapidly
         */
        protected double[] range;

        protected float sx = 100, sy = 100;

        private AtomicBoolean busy = new AtomicBoolean(false);

        public GNGVis3D(NAR nar, BagClustering<X> b) {
            super();//nar, null, b.bag.capacity(), b.bag.capacity(), 8, 16);

            this.net = b.net;
            this.bag = b.bag;

            range = new double[net.dimension * 2];

            this.centroids = new ConceptWidget[net.centroids.length];


//            new Loop() {
//                @Override public boolean next() {
//                    GNGVis3D.this.commit();
//                    return true;
//                }
//            }.runFPS(20f);
        }


        static float centroidHue(int id) {
            return (id % 8) / 8f;
        }

        protected void updateRange() {
        }

        protected SimpleSpatial<X> task(SimpleSpatial<X> w, double[] c) {
            position(w, c);
            w.scale(1, 1, 1);
            return w;
        }

        protected SimpleSpatial position(SimpleSpatial w, double[] c) {
            //float p = n.priSum()/size;

            float x = sx * r(0, c[0]);
            //float xe = sx * r(1, c[1]);
            float f = sy * r(1, c[1]);
            //float c = r(3, c[3]);

            w.move(x, f, 0);

            return w;

            //TODO HACK dimension c > 1 ignored


            //Draw.hsb( gl, hue, 0.5f, z, z *1f/(1f+w));
            //Draw.rect(gl, xs - w / 2, y - w / 2, w+(xe-xs), w);
        }

        private float r(int d, double v) {
            double min = range[d * 2];
            double max = range[d * 2 + 1];
            float u = Util.equals(Math.abs(max - min), 0, Pri.EPSILON) ?
                    ((float) v) //as-is
                    :
                    (float) ((v - min) / (max - min));
            return u;
        }


        @Override
        protected List<SimpleSpatial<X>> get() {

            updateRange();

            float maxDimNorm = (float) (range[1] - range[0]); //start time span

            int C = net.centroids.length;
            for (int i = 0, coordLength = C; i < coordLength; i++) {
                Centroid ii = net.centroids[i];
                ConceptWidget cc = centroids[i];

                if (cc == null) {
                    cc = new ConceptWidget($.the(ii.id)) {
                        @Override
                        public Dynamic newBody(boolean collidesWithOthersLikeThis) {
                            return super.newBody(false);
                        }
                    };
                    cc.scale(1, 1, 1);
                    position(cc, ii.getDataRef());
                    centroids[i] = cc;
                }


                float z = (float) ii.localError();
                cc.color(1f / (1f + z), 0.5f, 0.5f);

                float r = 1f / (1 + C);
                cc.scale(r, r, r);

                position(cc, ii.getDataRef());
            }

//            for (ConceptWidget c : centroids) {
//                if (c != null)
//                    displayNext.add(c);
//            }
//
//            this.bag.forEach(t -> {
//                if (t.isDeleted())
//                    return;
//                displayNext.add(task(widgetize(t.id), t.coord));
//            });
            return new FasterList(centroids);

        }


        private Cuboid widgetize(X t) {
            return space.getOrAdd(t, (tt) -> {
                return new Cuboid(tt, 1f, 1f) {
                    @Override
                    public Dynamic newBody(boolean collidesWithOthersLikeThis) {
                        return super.newBody(false);
                    }
                };

//                TermWidget tw = new TermWidget(tt, 1, 1) {
//                    @Override
//                    public boolean collidable() {
//                        return false; //super.collidable();
//                    }
//
//                };
//                return tw;
            });
        }
    }

    public static class GNGVis extends Surface {

        protected long now;
        protected int limit = -1;
        final NeuralGasNet net;

        /**
         * local copy of the range data, buffering it because it changes rapidly
         */
        protected double[] range;

        protected float sy = 5;
        float tx = -0.5f, ty = -0.5f;
        protected double dur;

        public GNGVis(NeuralGasNet net) {
            this.net = net;
            this.range = net.rangeMinMax;
            this.now = 0;
            this.dur = 1;
        }

        @Override
        protected void paint(GL2 gl) {
            if (range[0] != range[0])
                return; //nothing to show

            gl.glLineWidth(5f);

            int C = net.centroids.length;
            for (int i = 0, coordLength = C; i < coordLength; i++) {
                Centroid ii = net.centroids[i];

                float e = (float) ii.localError();
                draw(ii, gl, centroidHue(ii.id), ii.getDataRef(), i, (0.5f + 0.5f / (1f + e)));
            }
        }

        static float centroidHue(int id) {
            return (id % 8) / 8f;
        }


        protected void draw(Object o, GL2 gl, float hue, double[] c, int centroid, float v) {
            //float xe = sx * r(1, c[1]);
            float f = 0f; //c.length > 1 ? (float) (sy * c[1]) : 0.5f;
            //float c = r(3, c[3]);


            //TODO HACK dimension c > 1 ignored


            if (o instanceof Centroid) {
                f = 0.1f;
                float H = 0.15f;
                float cw = (float) (c[1]/2f+1f);
                float xl = x((float) c[0] - cw);
                float xr = x((float) c[0] + cw);
                Draw.hsb(gl, hue, 0.5f, 0.5f, v);
                Draw.rectStroke(gl, xl, ty + f - H / 2, xr-xl, H);
            } else if (o instanceof Task) {
                float H = 0.1f;

                f = -0.1f;
                Task t = (Task) o;
                float xl = x( t.start() - 0.5f);
                float xr = x( t.end() + 0.5f);
                if (xr < xl) {
                    float xx = xr;
                    xr = xl;
                    xl = xx;
                }
                float xw = (xr-xl);
                if (centroid >= 0)
                    Draw.hsb(gl, hue, 0.5f, 0.5f, v);
                else
                    gl.glColor4f(0.5f, 0.5f, 0.5f, v); //gray: unassigned

                Draw.rect(gl, xl - (xw/2f), ty + f - H / 2, xw, H);
            }
        }

        private float x( double v) {
            return (float) ((v - now)/dur);
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
        protected void paint(GL2 gl) {

            //temporary
            //bag.commit(1);

            this.now = nar.time();
            this.dur = nar.dur()*100f;

            super.paint(gl);

            bag.sorted.read().forEach(x -> {

                int ci = x.centroid;
                draw(x.get(), gl, centroidHue(ci), x.coord, ci,
                        0.1f + 0.5f * x.priElseZero());
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

    public static void show3D(NAR n, BagClustering c, int w, int h) {
        new SpaceGraph(new GNGVis3D(n, c))
                .camPos(0, 0, 90)
                .show(w, h);
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