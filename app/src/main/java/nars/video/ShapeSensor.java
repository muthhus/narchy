package nars.video;

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.shapes.ShapeFittingOps;
import boofcv.struct.ConnectRule;
import boofcv.struct.PointIndex_I32;
import boofcv.struct.image.GrayU8;
import georegression.geometry.UtilPolygons2D_I32;
import georegression.struct.shapes.Polygon2D_I32;
import georegression.struct.shapes.Rectangle2D_I32;
import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.Task;
import nars.control.CauseChannel;
import nars.task.ITask;
import nars.task.NALTask;
import nars.term.Term;
import nars.truth.Truth;
import nars.util.signal.Bitmap2D;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

import static nars.Op.BELIEF;

//import boofcv.gui.feature.VisualizeShapes;
//import boofcv.gui.image.ImagePanel;
//import boofcv.gui.image.ScaleOptions;
//import boofcv.gui.image.ShowImages;

public class ShapeSensor implements Runnable {

    private final Bitmap2D input;
    private final CauseChannel<ITask> in;
    private final Term id;
    private final NAR nar;
    //GrayF32 img = null;
    GrayU8 img;

    final static boolean debug = true;

    // Polynomial fitting tolerances
    static double splitFraction = 0.05;
    static double minimumSideFraction = 0.1;

//    static ImagePanel  gui = debug ? new ImagePanel(400,200) : null;

    private final float R = 1f;
    private final float G = 1f;
    private final float B = 1f;

    public ShapeSensor(Term id, Bitmap2D input, NAgent a) {
        this.id = id;
        this.input = input;

//        if (debug) {
//            ShowImages.showWindow(gui, "poly", false);
//            gui.setScaling(ScaleOptions.ALL);
//        }

        in = a.nar.newCauseChannel(this);
        this.nar = a.nar;

//        a.actionUnipolar($.p(id, $.the("R")), (v) -> {
//           return R = v;
//        });
//        a.actionUnipolar($.p(id, $.the("G")), (v) -> {
//           return G = v;
//        });
//        a.actionUnipolar($.p(id, $.the("B")), (v) -> {
//           return B = v;
//        });
        a.onFrame(this);
    }

    public static boolean isConvex(List<PointIndex_I32> poly) {

        int n = poly.size();
        if (n < 4)
            return true;

        boolean sign = false;
        for (int i = 0; i < n; i++) {
            PointIndex_I32 a = poly.get((i + 2) % n);
            PointIndex_I32 b = poly.get((i + 1) % n);
            double dx1 = a.x - b.x;
            double dy1 = a.y - b.y;
            PointIndex_I32 c = poly.get(i);
            double dx2 = c.x - b.x;
            double dy2 = c.y - b.y;
            double zcrossproduct = dx1 * dy2 - dy1 * dx2;
            if (i == 0)
                sign = zcrossproduct > 0;
            else if (sign != (zcrossproduct > 0))
                return false;
        }
        return true;
    }

    @Override
    public void run() {

        input.update(1f);

        if (img == null || img.width != input.width() || img.height != input.height()) {
            //img = new GrayF32(input.width(), input.height());
            img = new GrayU8(input.width(), input.height());
        }

        int w = img.width;
        int h = img.height;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                img.set(x, y, Math.round(256f * input.brightness(x, y, R, G, B)));
            }
        }

        //GrayU8 binary = new GrayU8(img.width, img.height);

        BufferedImage polygon;
        if (debug) {
            polygon = new BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_RGB);
        } else {
            polygon = null;
        }

        // the mean pixel value is often a reasonable threshold when creating a binary image
        int mean = (int) ImageStatistics.mean(img);

        // create a binary image by thresholding
        ThresholdImageOps.threshold(img, img,  mean, true);

        // reduce noise with some filtering
        GrayU8 filtered = img;
        //for (int i = 0; i < 2; i++) {
            filtered = BinaryImageOps.dilate8(filtered, 1, null);
            filtered = BinaryImageOps.erode8(filtered, 1, null);
        //}


        // Find the contour around the shapes

        List<Contour> contours = BinaryImageOps.contour(filtered, ConnectRule.EIGHT /*FOUR*/ /* EIGHT */, null);

//		// Fit a polygon to each shape and draw the results
        Graphics2D g2;
        if (debug) {
            g2 = polygon.createGraphics();
            g2.setStroke(new BasicStroke(2));
        } else {
            g2 = null;
        }

        int k = 0;
        for (Contour c : contours) {
            // Fit the polygon to the found external contour.  Note loop = true
            List<PointIndex_I32> p = ShapeFittingOps.fitPolygon(c.external, true,
                    splitFraction, minimumSideFraction, 100);


            if (debug) {
                g2.setColor(Color.getHSBColor(c.id/10f, 0.8f, 0.8f));
//                VisualizeShapes.drawPolygon(p,true,g2);
                //System.out.println(c + ": " + polygon);
            }

            input(k++, p, w, h);

//			// handle internal contours now
//			g2.setColor(Color.BLUE);
//			for( List<Point2D_I32> internal : c.internal ) {
//				vertexes = ShapeFittingOps.fitPolygon(internal,true, splitFraction, minimumSideFraction,100);
//			}
        }

        if (debug) {

//                gui.setBufferedImageSafe(polygon);


//            gui.reset();
//            gui.addImage(polygon, "shapes");
        }

    }

    private void input(int k, List<PointIndex_I32> polygon, float w, float h) {
        Polygon2D_I32 p = new Polygon2D_I32();
        for (PointIndex_I32 v : polygon)
            p.vertexes.add(v);
        Rectangle2D_I32 quad = new Rectangle2D_I32();
        UtilPolygons2D_I32.bounding(p, quad);
        float cx = ((quad.x0 + quad.x1)/2f)/w;
        float cy = ((quad.y0 + quad.y1)/2f)/h;
        float cw = quad.getWidth()/w;
        float ch = quad.getHeight()/h;
        Term pid = $.p(id, $.the(k));
        float conf = nar.confDefault(BELIEF);

        long now = nar.time();
        believe(now, $.inh(pid, $.the("x")), $.t(cx, conf));
        believe(now, $.inh(pid, $.the("y")), $.t(cy, conf));
        believe(now, $.inh(pid, $.the("w")), $.t(cw, conf));
        believe(now, $.inh(pid, $.the("h")), $.t(ch, conf));
    }

    private void believe(long now, Term term, Truth truth) {
        float pri = nar.priDefault(BELIEF);
        in.input((Task)new NALTask(term, BELIEF, truth, now, now, now, nar.time.nextInputStamp() ).pri(pri));
    }


    /**
     * Demonstration of how to convert a point sequence describing an objects outline/contour into a sequence of line
     * segments.  Useful when analysing shapes such as squares and triangles or when trying to simply the low level
     * pixel output.
     *
     * @author Peter Abeles
     */


//    /**
//     * Fits polygons to found contours around binary blobs.
//     */
//    public static void fitBinaryImage(GrayF32 input) {
//
//        GrayU8 binary = new GrayU8(input.width, input.height);
//        BufferedImage polygon = new BufferedImage(input.width, input.height, BufferedImage.TYPE_INT_RGB);
//
//        // the mean pixel value is often a reasonable threshold when creating a binary image
//        double mean = ImageStatistics.mean(input);
//
//        // create a binary image by thresholding
//        ThresholdImageOps.threshold(input, binary, (float) mean, true);
//
//        // reduce noise with some filtering
//        GrayU8 filtered = BinaryImageOps.erode8(binary, 1, null);
//        filtered = BinaryImageOps.dilate8(filtered, 1, null);
//
//        // Find the contour around the shapes
//
//        List<Contour> contours = BinaryImageOps.contour(filtered, ConnectRule.FOUR /* EIGHT */, null);
//
//        // Fit a polygon to each shape and draw the results
//        Graphics2D g2 = polygon.createGraphics();
//        g2.setStroke(new BasicStroke(3));
//
//        for (Contour c : contours) {
//            // Fit the polygon to the found external contour.  Note loop = true
//            List<PointIndex_I32> vertexes = ShapeFittingOps.fitPolygon(c.external, true,
//                    splitFraction, minimumSideFraction, 100);
//
//            g2.setColor(Color.RED);
//            VisualizeShapes.drawPolygon(vertexes, true, g2);
//            System.out.println(c + ": " + vertexes);
//
//            // handle internal contours now
//            g2.setColor(Color.BLUE);
//            for (List<Point2D_I32> internal : c.internal) {
//                vertexes = ShapeFittingOps.fitPolygon(internal, true, splitFraction, minimumSideFraction, 100);
//                VisualizeShapes.drawPolygon(vertexes, true, g2);
//            }
//        }
//
//        gui.addImage(polygon, "Binary Blob Contours");
//    }
//
//    /**
//     * Fits a sequence of line-segments into a sequence of points found using the Canny edge detector.  In this case
//     * the points are not connected in a loop. The canny detector produces a more complex tree and the fitted
//     * points can be a bit noisy compared to the others.
//     */
//    public static void fitCannyEdges(GrayF32 input) {
//
//        BufferedImage displayImage = new BufferedImage(input.width, input.height, BufferedImage.TYPE_INT_RGB);
//
//        // Finds edges inside the image
//        CannyEdge<GrayF32, GrayF32> canny =
//                FactoryEdgeDetectors.canny(2, true, true, GrayF32.class, GrayF32.class);
//
//        canny.process(input, 0.1f, 0.3f, null);
//        List<EdgeContour> contours = canny.getContours();
//
//        Graphics2D g2 = displayImage.createGraphics();
//        g2.setStroke(new BasicStroke(2));
//
//        // used to select colors for each line
//        Random rand = new Random(234);
//
//        for (EdgeContour e : contours) {
//            g2.setColor(new Color(rand.nextInt()));
//
//            for (EdgeSegment s : e.segments) {
//                // fit line segments to the point sequence.  Note that loop is false
//                List<PointIndex_I32> vertexes = ShapeFittingOps.fitPolygon(s.points, false,
//                        splitFraction, minimumSideFraction, 100);
//
//                VisualizeShapes.drawPolygon(vertexes, false, g2);
//            }
//        }
//
//        gui.addImage(displayImage, "Canny Trace");
//    }
//
//    /**
//     * Detects contours inside the binary image generated by canny.  Only the external contour is relevant. Often
//     * easier to deal with than working with Canny edges directly.
//     */
//    public static void fitCannyBinary(GrayF32 input) {
//
//        BufferedImage displayImage = new BufferedImage(input.width, input.height, BufferedImage.TYPE_INT_RGB);
//        GrayU8 binary = new GrayU8(input.width, input.height);
//
//        // Finds edges inside the image
//        CannyEdge<GrayF32, GrayF32> canny =
//                FactoryEdgeDetectors.canny(2, false, true, GrayF32.class, GrayF32.class);
//
//        canny.process(input, 0.1f, 0.3f, binary);
//
//        List<Contour> contours = BinaryImageOps.contour(binary, ConnectRule.EIGHT, null);
//
//        Graphics2D g2 = displayImage.createGraphics();
//        g2.setStroke(new BasicStroke(2));
//
//        // used to select colors for each line
//        Random rand = new Random(234);
//
//        for (Contour c : contours) {
//            // Only the external contours are relevant.
//            List<PointIndex_I32> vertexes = ShapeFittingOps.fitPolygon(c.external, true,
//                    splitFraction, minimumSideFraction, 100);
//
//            g2.setColor(new Color(rand.nextInt()));
//            VisualizeShapes.drawPolygon(vertexes, true, g2);
//        }
//
//        gui.addImage(displayImage, "Canny Contour");
//    }
//
//    public static void main(String args[]) {
//        // load and convert the image into a usable format
//        BufferedImage image = UtilImageIO.loadImage("/home/me/2017-02-17-123605_1920x1080_scrot.png");
//        GrayF32 input = ConvertBufferedImage.convertFromSingle(image, null, GrayF32.class);
//
//        gui.addImage(image, "Original");
//
//        fitCannyEdges(input);
//        fitCannyBinary(input);
//        fitBinaryImage(input);
//
//        ShowImages.showWindow(gui, "Polygon from Contour", true);
//    }
//
//
}

