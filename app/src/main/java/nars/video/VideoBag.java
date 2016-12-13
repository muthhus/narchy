package nars.video;


import boofcv.abst.flow.DenseOpticalFlow;
import boofcv.alg.color.ColorRgb;
import boofcv.factory.flow.FactoryDenseOpticalFlow;
import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.InterleavedU8;
import com.jogamp.opengl.GL2;
import jcog.Util;
import nars.$;
import nars.bag.impl.ArrayBag;
import nars.budget.merge.BudgetMerge;
import nars.gui.BagChart;
import nars.link.BLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.index.RTree;
import spacegraph.index.Rect1D;
import spacegraph.render.Draw;
import spacegraph.video.WebCam;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static jcog.Util.unitize;

/**
 * Created by me on 12/12/16.
 */
public class VideoBag {

    final static Logger logger = LoggerFactory.getLogger(VideoBag.class);

    final RTree<Frame> rtree;

    final ArrayBag<Frame> bag;

    public static class Frame extends Rect1D {

        public final long t;
        public final ImageBase image;

        public Frame(long when, ImageBase i) {
            this.t = when;
            this.image = i;
        }

        @Override
        public String toString() {
            return new Date(t).toString() + ": " +  image.toString();
        }

        @Override
        public double from() {
            return t;
        }

        @Override
        public double to() {
            return t;
        }
    }

    public VideoBag() {
        bag = new ArrayBag<>(16, BudgetMerge.avgBlend, new ConcurrentHashMap<>()) {
            @Override
            public void onAdded(BLink<Frame> v) {
                rtree.add(v.get());
            }

            @Override
            public void onRemoved(BLink<Frame> v) {
                rtree.remove(v.get());
            }
        };
        rtree = new RTree<>((l)->l, 2, 8, RTree.Split.LINEAR);


    }

    InterleavedU8 prev = null;
    final AtomicBoolean busy = new AtomicBoolean();


    DenseOpticalFlow<GrayU8> flow = FactoryDenseOpticalFlow.
            //hornSchunck(null, GrayU8.class);
                    broxWarping(null, GrayU8.class);

    public boolean put(InterleavedU8 current) {

        boolean added;
        if (busy.compareAndSet(false,true)) {

            long when = System.currentTimeMillis();
            //                    ImageBase di =
            //                            //        null;
            //                            ImageBase.createFromData(c.width, c.height, false, true, ByteBuffer.wrap(current.data));

            final Frame frame = new Frame(when, current);
            //System.out.println(current.data + " " + bag.rtree.collectStats());

            float scale;
            if (prev != null) {


                final int cw = current.width;
                final int ch = current.height;
                GrayU8 prevU = new GrayU8(cw, ch);
                ColorRgb.rgbToGray_Weighted(prev, prevU);

                GrayU8 nextU = new GrayU8(cw, ch);
                ColorRgb.rgbToGray_Weighted(current, nextU);

                            /*GrayU8 xor = new GrayU8(current.width, current.height);
                            BinaryImageOps.logicXor(prevU, nextU, xor);
                            System.out.println("sum xor: " + ImageStatistics.sum(xor));*/

                ImageFlow ff = new ImageFlow(cw, ch);
                flow.process(prevU, nextU, ff);


                float totalFlow = 0;
                for (ImageFlow.D d : ff.data) {
                    if (d.isValid()) {
                        totalFlow += Math.sqrt(Util.sqr(d.x) + Util.sqr(d.y));
                    }
                }
                float avgFlow = unitize(totalFlow / (cw*ch));
                logger.info("avg flow per pixel: {} ", avgFlow);

                //scale = Util.sigmoid(avgFlow);

                //                    BufferedImage visualized = new BufferedImage(current.width,current.height, BufferedImage.TYPE_INT_RGB);
                //VisualizeOpticalFlow.colorized(ff, 10, visualized);
                //
                //                    gui.setContentPane(new ImagePanel(visualized));
                //                    gui.doLayout();
                //                    gui.invalidate();
                //                    gui.repaint();


                scale = unitize(avgFlow);

            } else {

                scale = 0.5f;
            }

            bag.commit();
            bag.put(frame, $.b(1f, 0.5f + 0.5f * scale), 1f, null);

            prev = current;
            added = true;

            return true;
        }
        else {
            logger.info("frame dropped");

            added = false;

        }

        busy.set(false);

        return added;
    }

    public static void main(String[] args) {
        VideoBag bag = new VideoBag();
        final BagChart s = new BagChart(bag.bag, 16);


//        JFrame gui = new JFrame();
//        gui.setSize(400,400);
//        gui.setVisible(true);


        final WebCam cam;
        cam = new WebCam(320, 200);
        cam.loop(16f);
        cam.eventChange.on(new Consumer<WebCam>() {


            final ExecutorService exe = Executors.newSingleThreadExecutor();

            @Override
            public void accept(WebCam c) {

                    //DDSImage di = DDSImage.createFromData(DDSImage.D3DFMT_R8G8B8, width, height, new ByteBuffer[]{image.asReadOnlyBuffer()});
                    //JPEGImage di = JPEGImage.read(new ByteBufferInputStream(image.asReadOnlyBuffer()));


                    s.update();

                    InterleavedU8 current = new InterleavedU8(c.width, c.height, 3);
                    current.data = c.image.array().clone();

                    exe.execute(()->{
                        bag.put(current);
                    });



            }
        });

        SpaceGraph.window(s, 800, 800);
        SpaceGraph.window(new EventTimeline(bag), 800, 800);
    }

    static class EventTimeline extends Surface {

        public final VideoBag bag;

        float scale = 0.25f; //seconds to visual units

        EventTimeline(VideoBag bag) {
            this.bag = bag;
        }

        @Override
        protected void paint(GL2 gl) {
            super.paint(gl);
            long now = System.currentTimeMillis();
            bag.bag.forEach((ff)->{
                Frame f = ff.get();
                float xx = scale * (f.t - now)/1000f;
                Draw.colorPolarized(gl, 0.25f + 0.75f * ff.priIfFiniteElseZero());
                Draw.rect(gl, xx, 0, scale, scale);
            });
        }
    }
}
