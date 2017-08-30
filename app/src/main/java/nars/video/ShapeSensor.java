package nars.video;

import boofcv.alg.feature.detect.edge.CannyEdge;
import boofcv.alg.feature.detect.edge.EdgeContour;
import boofcv.alg.feature.detect.edge.EdgeSegment;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.shapes.ShapeFittingOps;
import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.ConnectRule;
import boofcv.struct.PointIndex_I32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_I32;
import nars.NAgent;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;

public class ShapeSensor implements Runnable {

    private final Bitmap2D input;
    GrayF32 img = null;

    public ShapeSensor(Bitmap2D input, NAgent a) {
        this.input = input;
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
        if (img == null || img.width != input.width() || img.height != input.height()) {
            img = new GrayF32(input.width(), input.height());
        }
        int w = img.width;
        int h = img.height;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                img.set(x, y, input.brightness(x, y));
            }
        }

        GrayU8 binary = new GrayU8(img.width, img.height);
        BufferedImage polygon = new BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_RGB);

        // the mean pixel value is often a reasonable threshold when creating a binary image
        double mean = ImageStatistics.mean(img);

        // create a binary image by thresholding
        ThresholdImageOps.threshold(img, binary, (float) mean, true);

        // reduce noise with some filtering
        GrayU8 filtered = BinaryImageOps.erode8(binary, 1, null);
        filtered = BinaryImageOps.dilate8(filtered, 1, null);

        // Find the contour around the shapes

        List<Contour> contours = BinaryImageOps.contour(filtered, ConnectRule.FOUR /* EIGHT */, null);

//		// Fit a polygon to each shape and draw the results
//		Graphics2D g2 = polygon.createGraphics();
//		g2.setStroke(new BasicStroke(3));

        for (Contour c : contours) {
            // Fit the polygon to the found external contour.  Note loop = true
            List<PointIndex_I32> vertexes = ShapeFittingOps.fitPolygon(c.external, true,
                    splitFraction, minimumSideFraction, 100);


            //VisualizeShapes.drawPolygon(vertexes,true,g2);
            System.out.println(c + ": " + vertexes);

//			// handle internal contours now
//			g2.setColor(Color.BLUE);
//			for( List<Point2D_I32> internal : c.internal ) {
//				vertexes = ShapeFittingOps.fitPolygon(internal,true, splitFraction, minimumSideFraction,100);
//			}
        }

    }


    /**
     * Demonstration of how to convert a point sequence describing an objects outline/contour into a sequence of line
     * segments.  Useful when analysing shapes such as squares and triangles or when trying to simply the low level
     * pixel output.
     *
     * @author Peter Abeles
     */


    // Polynomial fitting tolerances
    static double splitFraction = 0.05;
    static double minimumSideFraction = 0.1;

    static ListDisplayPanel gui = new ListDisplayPanel();

    /**
     * Fits polygons to found contours around binary blobs.
     */
    public static void fitBinaryImage(GrayF32 input) {

        GrayU8 binary = new GrayU8(input.width, input.height);
        BufferedImage polygon = new BufferedImage(input.width, input.height, BufferedImage.TYPE_INT_RGB);

        // the mean pixel value is often a reasonable threshold when creating a binary image
        double mean = ImageStatistics.mean(input);

        // create a binary image by thresholding
        ThresholdImageOps.threshold(input, binary, (float) mean, true);

        // reduce noise with some filtering
        GrayU8 filtered = BinaryImageOps.erode8(binary, 1, null);
        filtered = BinaryImageOps.dilate8(filtered, 1, null);

        // Find the contour around the shapes

        List<Contour> contours = BinaryImageOps.contour(filtered, ConnectRule.FOUR /* EIGHT */, null);

        // Fit a polygon to each shape and draw the results
        Graphics2D g2 = polygon.createGraphics();
        g2.setStroke(new BasicStroke(3));

        for (Contour c : contours) {
            // Fit the polygon to the found external contour.  Note loop = true
            List<PointIndex_I32> vertexes = ShapeFittingOps.fitPolygon(c.external, true,
                    splitFraction, minimumSideFraction, 100);

            g2.setColor(Color.RED);
            VisualizeShapes.drawPolygon(vertexes, true, g2);
            System.out.println(c + ": " + vertexes);

            // handle internal contours now
            g2.setColor(Color.BLUE);
            for (List<Point2D_I32> internal : c.internal) {
                vertexes = ShapeFittingOps.fitPolygon(internal, true, splitFraction, minimumSideFraction, 100);
                VisualizeShapes.drawPolygon(vertexes, true, g2);
            }
        }

        gui.addImage(polygon, "Binary Blob Contours");
    }

    /**
     * Fits a sequence of line-segments into a sequence of points found using the Canny edge detector.  In this case
     * the points are not connected in a loop. The canny detector produces a more complex tree and the fitted
     * points can be a bit noisy compared to the others.
     */
    public static void fitCannyEdges(GrayF32 input) {

        BufferedImage displayImage = new BufferedImage(input.width, input.height, BufferedImage.TYPE_INT_RGB);

        // Finds edges inside the image
        CannyEdge<GrayF32, GrayF32> canny =
                FactoryEdgeDetectors.canny(2, true, true, GrayF32.class, GrayF32.class);

        canny.process(input, 0.1f, 0.3f, null);
        List<EdgeContour> contours = canny.getContours();

        Graphics2D g2 = displayImage.createGraphics();
        g2.setStroke(new BasicStroke(2));

        // used to select colors for each line
        Random rand = new Random(234);

        for (EdgeContour e : contours) {
            g2.setColor(new Color(rand.nextInt()));

            for (EdgeSegment s : e.segments) {
                // fit line segments to the point sequence.  Note that loop is false
                List<PointIndex_I32> vertexes = ShapeFittingOps.fitPolygon(s.points, false,
                        splitFraction, minimumSideFraction, 100);

                VisualizeShapes.drawPolygon(vertexes, false, g2);
            }
        }

        gui.addImage(displayImage, "Canny Trace");
    }

    /**
     * Detects contours inside the binary image generated by canny.  Only the external contour is relevant. Often
     * easier to deal with than working with Canny edges directly.
     */
    public static void fitCannyBinary(GrayF32 input) {

        BufferedImage displayImage = new BufferedImage(input.width, input.height, BufferedImage.TYPE_INT_RGB);
        GrayU8 binary = new GrayU8(input.width, input.height);

        // Finds edges inside the image
        CannyEdge<GrayF32, GrayF32> canny =
                FactoryEdgeDetectors.canny(2, false, true, GrayF32.class, GrayF32.class);

        canny.process(input, 0.1f, 0.3f, binary);

        List<Contour> contours = BinaryImageOps.contour(binary, ConnectRule.EIGHT, null);

        Graphics2D g2 = displayImage.createGraphics();
        g2.setStroke(new BasicStroke(2));

        // used to select colors for each line
        Random rand = new Random(234);

        for (Contour c : contours) {
            // Only the external contours are relevant.
            List<PointIndex_I32> vertexes = ShapeFittingOps.fitPolygon(c.external, true,
                    splitFraction, minimumSideFraction, 100);

            g2.setColor(new Color(rand.nextInt()));
            VisualizeShapes.drawPolygon(vertexes, true, g2);
        }

        gui.addImage(displayImage, "Canny Contour");
    }

    public static void main(String args[]) {
        // load and convert the image into a usable format
        BufferedImage image = UtilImageIO.loadImage("/home/me/2017-02-17-123605_1920x1080_scrot.png");
        GrayF32 input = ConvertBufferedImage.convertFromSingle(image, null, GrayF32.class);

        gui.addImage(image, "Original");

        fitCannyEdges(input);
        fitCannyBinary(input);
        fitBinaryImage(input);

        ShowImages.showWindow(gui, "Polygon from Contour", true);
    }


}

