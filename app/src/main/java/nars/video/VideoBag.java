package nars.video;


import boofcv.abst.flow.DenseOpticalFlow;
import boofcv.alg.color.ColorRgb;
import boofcv.factory.flow.FactoryDenseOpticalFlow;
import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.InterleavedU8;
import com.jogamp.opengl.util.texture.spi.TGAImage;
import jcog.Util;
import nars.$;
import nars.bag.impl.ArrayBag;
import nars.budget.Budget;
import nars.budget.merge.BudgetMerge;
import nars.gui.BagChart;
import nars.link.BLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.SpaceGraph;
import spacegraph.index.RTree;
import spacegraph.index.Rect1D;
import spacegraph.video.WebCam;

import javax.swing.*;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Created by me on 12/12/16.
 */
public class VideoBag {

    final static Logger logger = LoggerFactory.getLogger(VideoBag.class);

    final RTree<Frame> rtree;

    final ArrayBag<Frame> bag;

    public static class Frame extends Rect1D {

        public final long t;
        public final TGAImage image;

        public Frame(long when, TGAImage i) {
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
        rtree = new RTree<Frame>((l)->l, 2, 8, RTree.Split.LINEAR);


    }

    public void put(Frame frame, Budget b, float scale) {

        bag.put(frame, b, scale, null);

    }

    public static void main(String[] args) {
        VideoBag bag = new VideoBag();
        final BagChart s = new BagChart(bag.bag, 16);


        DenseOpticalFlow<GrayU8> flow = FactoryDenseOpticalFlow.hornSchunck(null, GrayU8.class);


        JFrame gui = new JFrame();
        gui.setSize(400,400);
        gui.setVisible(true);


        final WebCam cam;
        cam = new WebCam(320, 200);
        cam.loop(8f);
        cam.eventChange.on(new Consumer<WebCam>() {

            InterleavedU8 prev = null;
            final AtomicBoolean busy = new AtomicBoolean();

            @Override
            public void accept(WebCam c) {

                if (busy.compareAndSet(false,true)) {
                    //DDSImage di = DDSImage.createFromData(DDSImage.D3DFMT_R8G8B8, width, height, new ByteBuffer[]{image.asReadOnlyBuffer()});
                    //JPEGImage di = JPEGImage.read(new ByteBufferInputStream(image.asReadOnlyBuffer()));
                    long when = System.currentTimeMillis();
                    TGAImage di =
                        //        null;
                        TGAImage.createFromData(c.width, c.height, false, true, c.image.asReadOnlyBuffer());

                    final Frame frame = new Frame(when, di);



                    s.update();

                    InterleavedU8 current = new InterleavedU8(c.width, c.height, 3);
                    current.data = c.image.array().clone();

                    //System.out.println(current.data + " " + bag.rtree.collectStats());

                    float scale;
                    if (prev != null) {


                        GrayU8 prevU = new GrayU8(prev.width, prev.height);
                        ColorRgb.rgbToGray_Weighted(prev, prevU);

                        GrayU8 nextU = new GrayU8(current.width, current.height);
                        ColorRgb.rgbToGray_Weighted(current, nextU);

                        /*GrayU8 xor = new GrayU8(current.width, current.height);
                        BinaryImageOps.logicXor(prevU, nextU, xor);
                        System.out.println("sum xor: " + ImageStatistics.sum(xor));*/

                        ImageFlow ff = new ImageFlow(c.width, c.height);
                        flow.process(prevU, nextU, ff);


                        float totalFlow = 0;
                        for (ImageFlow.D d : ff.data) {
                            if (d.isValid()) {
                                totalFlow += Util.sqr(d.x) + Util.sqr(d.y);
                            }
                        }
                        float avgFlow = totalFlow / (c.width * c.height);
                        logger.info("avg flow per pixel: {} ", avgFlow);

                        //scale = Util.sigmoid(avgFlow);

                        //                    BufferedImage visualized = new BufferedImage(current.width,current.height, BufferedImage.TYPE_INT_RGB);
                        //VisualizeOpticalFlow.colorized(ff, 10, visualized);
                        //
                        //                    gui.setContentPane(new ImagePanel(visualized));
                        //                    gui.doLayout();
                        //                    gui.invalidate();
                        //                    gui.repaint();


                        scale = avgFlow;

                    } else {

                        scale = 1f;
                    }

                    bag.bag.commit();
                    bag.put(frame, $.b(0.5f, 0.5f), scale);

                    prev = current;

                    busy.set(false);


                } else {
                    logger.info("frame dropped");
                }
            }
        });

        SpaceGraph.window(s, 800, 800);
    }
}
