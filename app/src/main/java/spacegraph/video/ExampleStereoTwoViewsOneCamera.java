package spacegraph.video;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.disparity.StereoDisparity;
import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.abst.geo.fitting.DistanceFromModelResidual;
import boofcv.abst.geo.fitting.GenerateEpipolarMatrix;
import boofcv.abst.geo.fitting.ModelManagerEpipolarMatrix;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.filter.derivative.LaplacianEdge;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.f.FundamentalResidualSampson;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.core.image.ConvertImage;
import boofcv.core.image.border.BorderType;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.feature.disparity.DisparityAlgorithms;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.factory.geo.*;
import boofcv.gui.d3.ColorPoint3D;
import boofcv.gui.feature.AssociationPanel;
import boofcv.gui.image.ShowImages;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.DoNothing2Transform2_F64;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.InterleavedU8;
import com.github.sarxos.webcam.Webcam;
import com.jogamp.opengl.GL2;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import nars.util.Util;
import nars.util.list.CircularArrayList;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelManager;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DenseMatrix64F;
import spacegraph.AbstractSpatial;
import spacegraph.SpaceGraph;
import spacegraph.render.Draw;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;


/**
 * Example demonstrating how to use to images taken from a single calibrated camera to create a stereo disparity image,
 * from which a dense 3D point cloud of the scene can be computed.  For this technique to work the camera's motion
 * needs to be approximately tangential to the direction the camera is pointing.  The code below assumes that the first
 * image is to the left of the second image.
 *
 * @author Peter Abeles
 */
public class ExampleStereoTwoViewsOneCamera {

    // Disparity calculation parameters
    private static final int minDisparity = 3;
    private static final int maxDisparity = 50;

    static class Frame {
        InterleavedU8 cam;
        GrayF32 distorted;
        BufferedImage out;

        public Frame(int pw, int ph, Webcam w) {
            cam = new InterleavedU8(pw, ph, 3);
            w.getImageBytes(ByteBuffer.wrap(cam.data));
            //distorted = ConvertImage.average(cam, null);
            distorted = ConvertImage.convert(ConvertImage.average(cam, null), (GrayF32) null);

        }
    }

    public static void main(String args[]) {


        // Camera parameters
        CameraPinholeRadial intrinsic = new CameraPinholeRadial(); //CalibrationIO.load(new File(calibDir , "intrinsic.yaml"));

        intrinsic.setWidth(176);
        intrinsic.setHeight(144);

        intrinsic.setCx(intrinsic.getWidth()/2f);
        intrinsic.setCy(intrinsic.getHeight()/2f);

        intrinsic.setFx(intrinsic.getWidth());
        intrinsic.setFy(intrinsic.getHeight());
//        intrinsic.setFx(701.0116882676376);
//        intrinsic.setFy(698.6537946928421);
        //intrinsic.setCx(308.4551818095542);
        //intrinsic.setCy(246.84300560315452);


        intrinsic.setSkew(0);
        intrinsic.setRadial(new double[]{-0.25559248570886445, 0.09997127476560613});
        intrinsic.setT1(0);
        intrinsic.setT2(0);

//        RectifiedPairPanel rpp = new RectifiedPairPanel(true);
//        rpp.setSize(500, 500);
//        rpp.setPreferredSize(new Dimension(800, 800));
//        ShowImages.showWindow(rpp, "Rectification");

        //BufferedImage visualized = VisualizeImageData.disparity(disparity, null, minDisparity, maxDisparity, 0);
        //ShowImages.showWindow(visualized, "Disparity");

        PointCloudTiltPanel gui = new PointCloudTiltPanel();
        gui.setSize(800, 800);
        gui.setPreferredSize(new Dimension(800, 800));
        ShowImages.showWindow(gui, "Point Cloud");

        // display the results
        AssociationPanel assoPanel = new AssociationPanel(20);
        assoPanel.setPreferredSize(new Dimension(800, 200));
        ShowImages.showWindow(assoPanel, "Inlier Features");

        Webcam w = Webcam.getDefault();
        w.setViewSize(new Dimension(intrinsic.getWidth(),intrinsic.getHeight()));
        w.open(false);

        int history = 1;
        final Deque<Frame> frames = new ArrayDeque();





        //BufferedImage outPrev = null, outNext = null;

        ModelMatcher<Se3_F64, AssociatedPair> epipolarMotion =
                FactoryMultiViewRobust.essentialRansac(new ConfigEssential(intrinsic),
                        new ConfigRansac(100, 0.5));

        while (true) {
            synchronized (frames) {
                if (w.isImageNew()) {

                    boolean diffed = false;


                    int pw = (int) w.getViewSize().getWidth();
                    int ph = (int) w.getViewSize().getHeight();
//                    intrinsic.setWidth(pw);
//                    intrinsic.setHeight(ph);

                    //if (next == null || next.getWidth()!=pw || next.getHeight()!=ph)
                    Frame next = new Frame(pw, ph, w);


                    for (Frame prev : frames) {


                        // Input images with lens distortion
                        //GrayU8 distortedPrev = ConvertImage.average(prev, null);
                        //GrayU8 distortedNext = ConvertImage.average(next, null);

                        // matched features between the two images
                        List<AssociatedPair> matchedFeatures = ExampleFundamentalMatrix.computeMatches(
                                prev.distorted,
                                next.distorted,
                                assoPanel);

                        // convert from pixel coordinates into normalized image coordinates
                        List<AssociatedPair> matchedCalibrated = convertToNormalizedCoordinates(matchedFeatures, intrinsic);

                        // Robustly estimate camera motion
                        //List<AssociatedPair> inliers = new ArrayList<>();

                        //Se3_F64 leftToRight = estimateCameraMotion(intrinsic, matchedCalibrated, inliers);

                        Se3_F64 leftToRight;
                        if (!epipolarMotion.process(matchedCalibrated)) {
                            leftToRight = null; ///throw new RuntimeException("Motion estimation failed");
                        } else {
                            // save inlier set for debugging purposes
                            //inliers.addAll(epipolarMotion.getMatchSet());

                            leftToRight = epipolarMotion.getModelParameters();
                        }

                        if (leftToRight != null) {

                            if (prev.cam!=null && next.cam!=null)
                                drawInliers(assoPanel, prev.cam, next.cam, intrinsic, epipolarMotion.getMatchSet());


                            // Rectify and remove lens distortion for stereo processing
                            DenseMatrix64F rectifiedK = new DenseMatrix64F(3, 3);
                            GrayF32 rectifiedPrev = prev.distorted.createSameShape();
                            GrayF32 rectifiedNext = next.distorted.createSameShape();


                            rectifyImages(prev.distorted, next.distorted, leftToRight, intrinsic, rectifiedPrev, rectifiedNext, rectifiedK);

                            // compute disparity
                            StereoDisparity<GrayF32, GrayF32> disparityAlg =
                                    FactoryStereoDisparity.regionSubpixelWta(DisparityAlgorithms.RECT_FIVE,
                                            minDisparity, maxDisparity, 3, 3, 40, 1, 0.1, GrayF32.class);

                            // Apply the Laplacian across the image to add extra resistance to changes in lighting or camera gain
                            GrayF32 derivPrev = new GrayF32(rectifiedPrev.width, rectifiedPrev.height);
                            LaplacianEdge.process(rectifiedPrev, derivPrev);
                            GrayF32 derivNext = new GrayF32(rectifiedNext.width, rectifiedNext.height);
                            LaplacianEdge.process(rectifiedNext, derivNext);

                            disparityAlg.process(derivPrev, derivNext);

                            prev.out = grayToBuffered(rectifiedPrev, prev.out);
                            next.out = grayToBuffered(rectifiedNext, next.out);
//                            rpp.setImages(outPrev, outNext);
//                            rpp.repaint();

                            double baseline = leftToRight.getT().norm();


                            gui.configure(baseline, rectifiedK, new DoNothing2Transform2_F64(), minDisparity, maxDisparity);
                            gui.process(disparityAlg.getDisparity(), prev.out);

                            diffed = true;
                            //showPointCloud(disparity, outPrev, leftToRight, rectifiedK, minDisparity, maxDisparity);

                            System.out.println("Total found " + matchedCalibrated.size() + "\t" + " cloud points=" + gui.view.cloud.size());
                            //System.out.println(Arrays.toString(gui.view.cloud.data));


                        }



                    }

                    if (diffed || frames.isEmpty()) {

                        gui.render();

                        System.out.println("snap");

                        while (frames.size() + 1 > history)
                            frames.removeFirst();

                        frames.addLast(next);
                    }
                }

                Util.sleep(20);

            }
        }


    }

    public static BufferedImage grayToBuffered(GrayF32 src, BufferedImage dst) {

        if (dst == null || dst.getWidth() != src.getWidth() || dst.getHeight() != src.getHeight())
            dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);

        final int width = dst.getWidth();
        final int height = dst.getHeight();

        float[] data = src.data;
        for (int y = 0; y < height; y++) {
            int indexSrc = src.startIndex + src.stride * y;
            for (int x = 0; x < width; x++) {
                int v = (int)(data[indexSrc++]*256);
                dst.setRGB(x, y, v << 16 | v << 8 | v);
            }
        }

        return dst;
    }
//    public static BufferedImage grayToBuffered(GrayU8 src, BufferedImage dst) {
//
//        if (dst == null || dst.getWidth() != src.getWidth() || dst.getHeight() != src.getHeight())
//            dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
//
//        final int width = dst.getWidth();
//        final int height = dst.getHeight();
//
//        byte[] data = src.data;
//        for (int y = 0; y < height; y++) {
//            int indexSrc = src.startIndex + src.stride * y;
//
//            for (int x = 0; x < width; x++) {
//                int v = data[indexSrc++] & 0xFF;
//
//                int argb = v << 16 | v << 8 | v;
//
//                dst.setRGB(x, y, argb);
//            }
//        }
//
//        return dst;
//    }
//
//    /**
//     * Estimates the camera motion robustly using RANSAC and a set of associated points.
//     *
//     * @param intrinsic   Intrinsic camera parameters
//     * @param matchedNorm set of matched point features in normalized image coordinates
//     * @param inliers     OUTPUT: Set of inlier features from RANSAC
//     * @return Found camera motion.  Note translation has an arbitrary scale
//     */
//    public static Se3_F64 estimateCameraMotion(IntrinsicParameters intrinsic,
//                                               List<AssociatedPair> matchedNorm, List<AssociatedPair> inliers) {
//
//    }

    /**
     * Convert a set of associated point features from pixel coordinates into normalized image coordinates.
     */
    public static List<AssociatedPair> convertToNormalizedCoordinates(List<AssociatedPair> matchedFeatures, CameraPinholeRadial intrinsic) {

        Point2Transform2_F64 p_to_n = LensDistortionOps.transformPoint(intrinsic).undistort_F64(true, false);

        List<AssociatedPair> calibratedFeatures = new ArrayList<>();

        for (AssociatedPair p : matchedFeatures) {
            AssociatedPair c = new AssociatedPair();

            p_to_n.compute(p.p1.x, p.p1.y, c.p1);
            p_to_n.compute(p.p2.x, p.p2.y, c.p2);

            calibratedFeatures.add(c);
        }

        return calibratedFeatures;
    }

    /**
     * Remove lens distortion and rectify stereo images
     *
     * @param distortedLeft  Input distorted image from left camera.
     * @param distortedRight Input distorted image from right camera.
     * @param leftToRight    Camera motion from left to right
     * @param intrinsic      Intrinsic camera parameters
     * @param rectifiedLeft  Output rectified image for left camera.
     * @param rectifiedRight Output rectified image for right camera.
     * @param rectifiedK     Output camera calibration matrix for rectified camera
     * @param model
     */
    public static void rectifyImages(GrayF32 distortedLeft,
                                     GrayF32 distortedRight,
                                     Se3_F64 leftToRight,
                                     CameraPinholeRadial intrinsic,
                                     GrayF32 rectifiedLeft,
                                     GrayF32 rectifiedRight,
                                     DenseMatrix64F rectifiedK) {
        RectifyCalibrated rectifyAlg = RectifyImageOps.createCalibrated();

        // original camera calibration matrices
        DenseMatrix64F K = PerspectiveOps.calibrationMatrix(intrinsic, null);

        Se3_F64 motion = new Se3_F64();
        rectifyAlg.process(K, motion, K, leftToRight);

        // rectification matrix for each image
        DenseMatrix64F rect1 = rectifyAlg.getRect1();
        DenseMatrix64F rect2 = rectifyAlg.getRect2();

        // New calibration matrix,
        rectifiedK.set(rectifyAlg.getCalibrationMatrix());

        // Adjust the rectification to make the view area more useful
        RectifyImageOps.allInsideLeft(intrinsic, rect1, rect2, rectifiedK);

        // undistorted and rectify images
        ImageDistort<GrayF32, GrayF32> distortLeft =
                RectifyImageOps.rectifyImage(intrinsic, rect1, BorderType.SKIP, distortedLeft.getImageType());
        ImageDistort<GrayF32, GrayF32> distortRight =
                RectifyImageOps.rectifyImage(intrinsic, rect2, BorderType.SKIP, distortedRight.getImageType());

        distortLeft.apply(distortedLeft, rectifiedLeft);
        distortRight.apply(distortedRight, rectifiedRight);
    }

    /**
     * Draw inliers for debugging purposes.  Need to convert from normalized to pixel coordinates.
     */
    public static void drawInliers(AssociationPanel panel, InterleavedU8 left, InterleavedU8 right, CameraPinholeRadial intrinsic,
                                   List<AssociatedPair> normalized) {
        Point2Transform2_F64 n_to_p = LensDistortionOps.transformPoint(intrinsic).distort_F64(false, true);

        List<AssociatedPair> pixels = new ArrayList<>();

        for (AssociatedPair n : normalized) {
            AssociatedPair p = new AssociatedPair();

            n_to_p.compute(n.p1.x, n.p1.y, p.p1);
            n_to_p.compute(n.p2.x, n.p2.y, p.p2);

            pixels.add(p);
        }


        panel.setAssociation(pixels);
        panel.setImages(interleavedToBuffered(left, null), interleavedToBuffered(right, null));
        panel.repaint();

    }

    public static BufferedImage interleavedToBuffered(InterleavedU8 src, BufferedImage dst) {

        if (src.getNumBands() != 3)
            throw new IllegalArgumentException("src must have three bands");

        if (dst == null || dst.getWidth() != src.getWidth() || dst.getHeight() != src.getHeight()) {
            dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        }

        final int width = dst.getWidth();
        final int height = dst.getHeight();

        for (int y = 0; y < height; y++) {
            int indexSrc = src.startIndex + src.stride * y;

            for (int x = 0; x < width; x++) {
                int c1 = src.data[indexSrc++] & 0xFF;
                int c2 = src.data[indexSrc++] & 0xFF;
                int c3 = src.data[indexSrc++] & 0xFF;

                int argb = c1 << 16 | c2 << 8 | c3;

                dst.setRGB(x, y, argb);
            }
        }

        return dst;
    }
//    /**
//     * Show results as a point cloud
//     */
//    public static void showPointCloud(ImageGray disparity, BufferedImage left,
//                                      Se3_F64 motion, DenseMatrix64F rectifiedK,
//                                      int minDisparity, int maxDisparity) {
//        PointCloudTiltPanel gui = new PointCloudTiltPanel();
//
//        double baseline = motion.getT().norm();
//
//        gui.configure(baseline, rectifiedK, new DoNothingTransform_F64(), minDisparity, maxDisparity);
//        gui.process(disparity, left);
//        gui.setPreferredSize(new Dimension(left.getWidth(), left.getHeight()));
//
//        ShowImages.showWindow(gui, "Point Cloud");
//    }

	/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


    /**
     * A Fundamental matrix describes the epipolar relationship between two images.  If two points, one from
     * each image, match, then the inner product around the Fundamental matrix will be zero.  If a fundamental
     * matrix is known, then information about the scene and its structure can be extracted.
     * <p>
     * Below are two examples of how a Fundamental matrix can be computed using different.
     * The robust technique attempts to find the best fit Fundamental matrix to the data while removing noisy
     * matches, The simple version just assumes that all the matches are correct.  Similar techniques can be used
     * to fit various other types of motion or structural models to observations.
     * <p>
     * The input image and associated features are displayed in a window.  In another window, inlier features
     * from robust model fitting are shown.
     *
     * @author Peter Abeles
     */
    static class ExampleFundamentalMatrix {

        /**
         * Given a set of noisy observations, compute the Fundamental matrix while removing
         * the noise.
         *
         * @param matches List of associated features between the two images
         * @param inliers List of feature pairs that were determined to not be noise.
         * @return The found fundamental matrix.
         */
        public static DenseMatrix64F robustFundamental(List<AssociatedPair> matches,
                                                       List<AssociatedPair> inliers) {

            // used to create and copy new instances of the fit model
            ModelManager<DenseMatrix64F> managerF = new ModelManagerEpipolarMatrix();
            // Select which linear algorithm is to be used.  Try playing with the number of remove ambiguity points
            Estimate1ofEpipolar estimateF = FactoryMultiView.computeFundamental_1(EnumEpipolar.FUNDAMENTAL_7_LINEAR, 2);
            // Wrapper so that this estimator can be used by the robust estimator
            GenerateEpipolarMatrix generateF = new GenerateEpipolarMatrix(estimateF);

            // How the error is measured
            DistanceFromModelResidual<DenseMatrix64F, AssociatedPair> errorMetric =
                    new DistanceFromModelResidual<>(new FundamentalResidualSampson());

            // Use RANSAC to estimate the Fundamental matrix
            ModelMatcher<DenseMatrix64F, AssociatedPair> robustF =
                    new Ransac<>(123123, managerF, generateF, errorMetric, 6000, 0.1);

            // Estimate the fundamental matrix while removing outliers
            if (!robustF.process(matches))
                throw new IllegalArgumentException("Failed");

            // save the set of features that were used to compute the fundamental matrix
            inliers.addAll(robustF.getMatchSet());

            // Improve the estimate of the fundamental matrix using non-linear optimization
            DenseMatrix64F F = new DenseMatrix64F(3, 3);
            ModelFitter<DenseMatrix64F, AssociatedPair> refine =
                    FactoryMultiView.refineFundamental(1e-8, 400, EpipolarError.SAMPSON);
            if (!refine.fitModel(inliers, robustF.getModelParameters(), F))
                throw new IllegalArgumentException("Failed");

            // Return the solution
            return F;
        }

        /**
         * If the set of associated features are known to be correct, then the fundamental matrix can
         * be computed directly with a lot less code.  The down side is that this technique is very
         * sensitive to noise.
         */
        public static DenseMatrix64F simpleFundamental(List<AssociatedPair> matches) {
            // Use the 8-point algorithm since it will work with an arbitrary number of points
            Estimate1ofEpipolar estimateF = FactoryMultiView.computeFundamental_1(EnumEpipolar.FUNDAMENTAL_8_LINEAR, 0);

            DenseMatrix64F F = new DenseMatrix64F(3, 3);
            if (!estimateF.process(matches, F))
                throw new IllegalArgumentException("Failed");

            // while not done here, this initial linear estimate can be refined using non-linear optimization
            // as was done above.
            return F;
        }

        /**
         * Use the associate point feature example to create a list of {@link AssociatedPair} for use in computing the
         * fundamental matrix.
         */
        public static List<AssociatedPair> computeMatches(GrayF32 left, GrayF32 right, AssociationPanel panel) {
            DetectDescribePoint detDesc = FactoryDetectDescribe.surfStable(
                    new ConfigFastHessian(1, 2, 200, 1, 9, 4, 4), null, null, GrayF32.class);
//		DetectDescribePoint detDesc = FactoryDetectDescribe.sift(null,new ConfigSiftDetector(2,0,200,5),null,null);

            ScoreAssociation<BrightFeature> scorer = FactoryAssociation.scoreEuclidean(BrightFeature.class, true);
            AssociateDescription<BrightFeature> associate = FactoryAssociation.greedy(scorer, 1, true);

            ExampleAssociatePoints<GrayF32, BrightFeature> findMatches =
                    new ExampleAssociatePoints<>(detDesc, associate, GrayF32.class);

            findMatches.associate(panel, left, right);

            FastQueue<AssociatedIndex> matchIndexes = associate.getMatches();

            int mis = matchIndexes.size;
            List<AssociatedPair> matches = new ArrayList<>(mis);
            for (int i = 0; i < mis; i++) {
                AssociatedIndex a = matchIndexes.get(i);
                AssociatedPair p = new AssociatedPair(findMatches.pointsA.get(a.src), findMatches.pointsB.get(a.dst));
                matches.add(p);
            }

            return matches;
        }

//        public static void main(String args[]) {
//
//            String dir = UtilIO.pathExample("structure/");
//
//            BufferedImage imageA = UtilImageIO.loadImage(dir, "undist_cyto_01.jpg");
//            BufferedImage imageB = UtilImageIO.loadImage(dir, "undist_cyto_02.jpg");
//
//            List<AssociatedPair> matches = computeMatches(imageA, imageB);
//
//            // Where the fundamental matrix is stored
//            DenseMatrix64F F;
//            // List of matches that matched the model
//            List<AssociatedPair> inliers = new ArrayList<>();
//
//            // estimate and print the results using a robust and simple estimator
//            // The results should be difference since there are many false associations in the simple model
//            // Also note that the fundamental matrix is only defined up to a scale factor.
//            F = robustFundamental(matches, inliers);
//            System.out.println("Robust");
//            F.print();
//
//            F = simpleFundamental(matches);
//            System.out.println("Simple");
//            F.print();
//
//            // display the inlier matches found using the robust estimator
//            AssociationPanel panel = new AssociationPanel(20);
//            panel.setAssociation(inliers);
//            panel.setImages(imageA, imageB);
//
//            ShowImages.showWindow(panel, "Inlier Pairs");
//        }
    }

    /**
     * After interest points have been detected in two images the next step is to associate the two
     * sets of images so that the relationship can be found.  This is done by computing descriptors for
     * each detected feature and associating them together.  In the code below abstracted interfaces are
     * used to allow different algorithms to be easily used.  The cost of this abstraction is that detector/descriptor
     * specific information is thrown away, potentially slowing down or degrading performance.
     *
     * @author Peter Abeles
     */
    public static class ExampleAssociatePoints<T extends ImageGray, TD extends TupleDesc> {

        // algorithm used to detect and describe interest points
        DetectDescribePoint<T, TD> detDesc;
        // Associated descriptions together by minimizing an error metric
        AssociateDescription<TD> associate;

        // location of interest points
        public List<Point2D_F64> pointsA;
        public List<Point2D_F64> pointsB;

        Class<T> imageType;

        public ExampleAssociatePoints(DetectDescribePoint<T, TD> detDesc,
                                      AssociateDescription<TD> associate,
                                      Class<T> imageType) {
            this.detDesc = detDesc;
            this.associate = associate;
            this.imageType = imageType;
        }

        /**
         * Detect and associate point features in the two images.  Display the results.
         */
        public void associate(AssociationPanel panel, T inputA, T inputB) {
//            T inputA = ConvertBufferedImage.convertFromSingle(imageA, null, imageType);
//            T inputB = ConvertImage.convert(imageB, null, imageType);

            // stores the location of detected interest points
            int initialSize = 100;

            pointsA = new ArrayList<>(initialSize);
            pointsB = new ArrayList<>(initialSize);

            // stores the description of detected interest points
            FastQueue<TD> descA = UtilFeature.createQueue(detDesc, initialSize);
            FastQueue<TD> descB = UtilFeature.createQueue(detDesc, initialSize);

            // describe each image using interest points
            describeImage(inputA, pointsA, descA);
            describeImage(inputB, pointsB, descB);

            // Associate features between the two images
            associate.setSource(descA);
            associate.setDestination(descB);
            associate.associate();

            // display the results
            panel.setAssociation(pointsA, pointsB, associate.getMatches());
            panel.repaint();
            //panel.setImages(grayToBuffered(imageA,null);,imageB);

            //ShowImages.showWindow(panel,"Associated Features",true);
        }

        /**
         * Detects features inside the two images and computes descriptions at those points.
         */
        private void describeImage(T input, List<Point2D_F64> points, FastQueue<TD> descs) {
            detDesc.detect(input);

            for (int i = 0; i < detDesc.getNumberOfFeatures(); i++) {
                points.add(detDesc.getLocation(i));//.copy());
                descs.grow().setTo(detDesc.getDescription(i));
            }
        }

//        public static void main( String args[] ) {
//
//            Class imageType = GrayF32.class;
////		Class imageType = GrayU8.class;
//
//            // select which algorithms to use
//            DetectDescribePoint detDesc = FactoryDetectDescribe.
//                    surfStable(new ConfigFastHessian(1, 2, 300, 1, 9, 4, 4), null,null, imageType);
////				sift(new ConfigCompleteSift(0,5,600));
//
//            ScoreAssociation scorer = FactoryAssociation.defaultScore(detDesc.getDescriptionType());
//            AssociateDescription associate = FactoryAssociation.greedy(scorer, Double.MAX_VALUE, true);
//
//            // load and match images
//            ExampleAssociatePoints app = new ExampleAssociatePoints(detDesc,associate,imageType);
//
//            BufferedImage imageA = UtilImageIO.loadImage(UtilIO.pathExample("stitch/kayak_01.jpg"));
//            BufferedImage imageB = UtilImageIO.loadImage(UtilIO.pathExample("stitch/kayak_03.jpg"));
//
//            app.associate(imageA,imageB);
//        }
    }


    /**
     * <p>
     * Renders a 3D point cloud using a perspective pin hole camera model.
     * </p>
     * <p>
     * <p>
     * Rendering speed is improved by first rendering onto a grid and only accepting the highest
     * (closest to viewing camera) point as being visible.
     * </p>
     *
     * @author Peter Abeles
     */
    public static class DisparityPointCloudViewer extends AbstractSpatial {

        final int capacity = 8000;
        final CircularArrayList<ColorPoint3D> cloud = new CircularArrayList<ColorPoint3D>(capacity);

        // distance between the two camera centers
        double baseline;

        // intrinsic camera parameters
        DenseMatrix64F K;
        double focalLengthX;
        double focalLengthY;
        double centerX;
        double centerY;

        // minimum disparity
        int minDisparity;
        // maximum minus minimum disparity
        int rangeDisparity;

        // How far out it should zoom.
        double range = 1;

        // view offset
        double offsetX;
        double offsetY;

        // Data structure that contains the visible point at each pixel
        // size = width*height, row major format
        //Pixel data[] = new Pixel[0];

        // tilt angle in degrees
        public int tiltAngle;
        public double radius = 5;

        // converts from rectified pixels into color image pixels
        Point2Transform2_F64 rectifiedToColor;
        // storage for color image coordinate
        Point2D_F64 colorPt = new Point2D_F64();
        private int w, h;

        public DisparityPointCloudViewer() {
            super(UUID.randomUUID());
        }


        /**
         * Stereo and intrinsic camera parameters
         *
         * @param baseline         Stereo baseline (world units)
         * @param K                Intrinsic camera calibration matrix of rectified camera
         * @param rectifiedToColor Transform from rectified pixels to the color image pixels.
         * @param minDisparity     Minimum disparity that's computed (pixels)
         * @param maxDisparity     Maximum disparity that's computed (pixels)
         */
        public void configure(double baseline,
                              DenseMatrix64F K,
                              Point2Transform2_F64 rectifiedToColor,
                              int minDisparity, int maxDisparity) {
            this.K = K;
            this.rectifiedToColor = rectifiedToColor;
            this.baseline = baseline;
            this.focalLengthX = K.get(0, 0);
            this.focalLengthY = K.get(1, 1);
            this.centerX = K.get(0, 2);
            this.centerY = K.get(1, 2);
            this.minDisparity = minDisparity;

            this.rangeDisparity = maxDisparity - minDisparity;


        }

//        /**
//         * Given the disparity image compute the 3D location of valid points and save pixel colors
//         * at that point
//         *
//         * @param disparity Disparity image
//         * @param color     Color image of left camera
//         */
//        public void process(GrayF32 disparity, BufferedImage color) {
////            if (disparity instanceof GrayU8)
////                process((GrayU8) disparity, color);
////            else
//                process((GrayF32) disparity, color);
//        }

//        private void process(GrayU8 disparity, BufferedImage img) {
//
//            trim();
//
//            synchronized(cloud) {
//                for (int y = 0; y < disparity.height; y++) {
//                    int index = disparity.startIndex + disparity.stride * y;
//
//                    for (int x = 0; x < disparity.width; x++) {
//                        int value = disparity.data[index++] & 0xFF;
//
//                        if (value >= rangeDisparity)
//                            continue;
//
//                        value += minDisparity;
//
//                        if (value == 0)
//                            continue;
//
//                        double zz = baseline * focalLengthX / value;
//                        if (zz <= 0)
//                            continue;
//
//                        ColorPoint3D p = cloud.size() > capacity ? cloud.removeFirst() : new ColorPoint3D();
//
//                        // Note that this will be in the rectified left camera's reference frame.
//                        // An additional rotation is needed to put it into the original left camera frame.
//                        p.z = zz;
//                        p.x = p.z * (x - centerX) / focalLengthX;
//                        p.y = p.z * (y - centerY) / focalLengthY;
//
//                        getColor(disparity, img, x, y, p);
//
//                        cloud.addLast(p);
//
//                    }
//                }
//            }
//        }

        private void trim() {
            //cloud.reset();
//            int toRemove = cloud.size() - maxPoints;
//            for (int i = 0; i < toRemove; i++) {
//                cloud.removeFirst();
//            }
        }


        @Override
        public void forEachBody(Consumer c) {

        }

        private void process(GrayF32 disparity, BufferedImage img) {
            w = img.getWidth();
            h = img.getHeight();

            trim();

            for (int y = 0; y < disparity.height; y++) {
                int index = disparity.startIndex + disparity.stride * y;

                for (int x = 0; x < disparity.width; x++) {
                    float value = disparity.data[index++];

                    if (value >= rangeDisparity)
                        continue;

                    value += minDisparity;

                    if (value == 0)
                        continue;

                    boolean cap = cloud.size() >= capacity;
                    ColorPoint3D p = cap ? cloud.removeFirst() : new ColorPoint3D();

                    // Note that this will be in the rectified left camera's reference frame.
                    // An additional rotation is needed to put it into the original left camera frame.
                    p.z = baseline * focalLengthX / value;
                    p.x = p.z * (x - centerX) / focalLengthX;
                    p.y = p.z * (y - centerY) / focalLengthY;

                    getColor(disparity, img, x, y, p);

                    cloud.addLast(p);
                }
            }
        }

        private void getColor(ImageBase disparity, BufferedImage color, int x, int y, ColorPoint3D p) {
            rectifiedToColor.compute(x, y, colorPt);
            //if (BoofMiscOps.checkInside(disparity, colorPt.x, colorPt.y, 0)) {
                p.rgb = color.getRGB((int) colorPt.x, (int) colorPt.y);
            //} else {
              //  p.rgb = 0x000000;
            //}
        }


        @Override
        public void renderAbsolute(GL2 gl) {


            if ((w > 0) && (h > 0)) {
                /*synchronized (cloud)*/ {
                    float scale = 1f/100f;
                    float dw = 1f / w;
                    float dh = 1f / h;
                    cloud.forEach(p->{

                        int rgb = p.rgb;
                        gl.glColor3f( 0.5f + 0.5f *(rgb & 0xff)/256f,
                                ((rgb & 0xff00) >> 8)/256f,
                                ((rgb & 0xff0000) >> 16)/256f);
                        Draw.rect(gl, scale * (float) p.x, scale * (float) p.y, dw,  dh
                                ,scale * (float) p.z);

                    });
                }
            }

//            int r = 2;
//            int w = r * 2 + 1;


//            int index = 0;
//            for (int y = 0; y < h; y++) {
//                for (int x = 0; x < w; x++) {
//                    Pixel p = d[index++];
//                    if (p.rgb == -1)
//                        continue;
//
//                    Draw.rect(gl, );
//                    g2.setColor(new Color(p.rgb));
//                    g2.fillRect(x - r, y - r, w, w);
//                }
//            }
        }

//        private void projectScene() {
//
//
//            int N = w * h;
//
//            Pixel[] data = this.data;
//            if (data.length < N) {
//                data = this.data = new Pixel[N];
//                for (int i = 0; i < N; i++)
//                    data[i] = new Pixel();
//            } else {
//                for (int i = 0; i < N; i++)
//                    data[i].fade();
//            }
//
//            Se3_F64 pose = createWorldToCamera();
//            Point3D_F64 cameraPt = new Point3D_F64();
//            Point2D_F64 pixel = new Point2D_F64();
//
//            synchronized(cloud) {
//                for (ColorPoint3D p : cloud) {
//
//                    SePointOps_F64.transform(pose, p, cameraPt);
//                    double cz = cameraPt.z;
//
//                    pixel.x = cameraPt.x / cz;
//                    pixel.y = cameraPt.y / cz;
//
//                    GeometryMath_F64.mult(K, pixel, pixel);
//
//                    int x = (int) pixel.x;
//                    int y = (int) pixel.y;
//
//                    if (x < 0 || y < 0 || x >= w || y >= h)
//                        continue;
//
//                    Pixel d = data[y * w + x];
//                    if (d.height > cz) {
//                        d.height = cz;
//                        d.rgb = p.rgb;
//                    }
//                }
//            }
//
//            //this.data = data;
//        }

        public Se3_F64 createWorldToCamera() {
            // pick an appropriate amount of motion for the scene
            double z = baseline * focalLengthX / (minDisparity + rangeDisparity);

            double adjust = baseline / 20.0;

            Vector3D_F64 rotPt = new Vector3D_F64(offsetX * adjust, offsetY * adjust, z * range);

            double radians = tiltAngle * Math.PI / 180.0;
            DenseMatrix64F R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, radians, 0, 0, null);

            Se3_F64 a = new Se3_F64(R, rotPt);

            return a;
        }



        /**
         * Contains information on visible pixels
         */
        private static class Pixel {
            // the pixel's height.  used to see if it is closer to the  camera or not
            public double height;
            // Color of the pixel
            public int rgb;

            private Pixel() {
                reset();
            }

            public void reset() {
                height = Double.MAX_VALUE;
                rgb = -1;
            }

            public void fade() {
                reset();
            }
        }
    }

    /**
     * Provides a simplified set of controls for changing the view in a {@link DisparityPointCloudViewer}.
     * Uses the mouse to move the camera in its local X,Y plane.  Widgets in the control bar
     * allow the user to change the camera's tilt and distance from the origin.
     *
     * @author Peter Abeles
     */
    public static class PointCloudTiltPanel extends JPanel
            implements ActionListener, ChangeListener, MouseListener, MouseMotionListener {
        // Point cloud viewer
        DisparityPointCloudViewer view;

        // when pressed sets the view to "home"
        JButton homeButton;
        // Adjusts the amount of zoom
        JSpinner rangeSpinner;
        // Tilts the camera up and down
        JSlider tiltSlider;

        // bounds on scale adjustment
        double minRange;
        double maxRange = 20;

        // previous mouse location
        int prevX;
        int prevY;

        public PointCloudTiltPanel() {
            super(new BorderLayout());

            addMouseListener(this);
            addMouseMotionListener(this);

            view = new DisparityPointCloudViewer();
            //view.setSize(800, 800);

            JToolBar toolBar = createToolBar();

            add(toolBar, BorderLayout.PAGE_START);

            SpaceGraph.window(view, 800, 800);
            //add(view, BorderLayout.CENTER);
        }

        private JToolBar createToolBar() {
            JToolBar toolBar = new JToolBar("Controls");

            homeButton = new JButton("Home");
            homeButton.addActionListener(this);

            rangeSpinner = new JSpinner(new SpinnerNumberModel(view.range, minRange, maxRange, 0.2));

            rangeSpinner.addChangeListener(this);
            rangeSpinner.setMaximumSize(rangeSpinner.getPreferredSize());

            tiltSlider = new JSlider(JSlider.HORIZONTAL,
                    -120, 120, view.tiltAngle);
            tiltSlider.addChangeListener(this);
            tiltSlider.setMajorTickSpacing(60);
            tiltSlider.setPaintLabels(true);

            toolBar.add(homeButton);
            toolBar.add(new JToolBar.Separator(new Dimension(10, 1)));
            toolBar.add(new JLabel("Range:"));
            toolBar.add(rangeSpinner);
            toolBar.add(new JToolBar.Separator(new Dimension(10, 1)));
            toolBar.add(new JLabel("Tilt Angle:"));
            toolBar.add(tiltSlider);

            return toolBar;
        }

        /**
         * Specified intrinsic camera parameters and disparity settings
         *
         * @param baseline Stereo baseline
         * @param K        rectified camera calibration matrix
         */
        public void configure(double baseline,
                              DenseMatrix64F K,
                              Point2Transform2_F64 rectifiedToColor,
                              int minDisparity, int maxDisparity) {
            view.configure(baseline, K, rectifiedToColor, minDisparity, maxDisparity);
        }

        /**
         * Updates the view, must be called in a GUI thread
         */
        public void process(GrayF32 disparity, BufferedImage color) {

            view.process(disparity, color);


        }

        public void render() {
            tiltSlider.removeChangeListener(this);
            tiltSlider.setValue(view.tiltAngle);
            tiltSlider.addChangeListener(this);

            update();
            repaint();
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            if (e.getSource() == homeButton) {
                view.offsetX = 0;
                view.offsetY = 0;
                view.tiltAngle = 0;
                view.range = 1;

                tiltSlider.removeChangeListener(this);
                tiltSlider.setValue(view.tiltAngle);
                tiltSlider.addChangeListener(this);

                rangeSpinner.removeChangeListener(this);
                rangeSpinner.setValue(view.range);
                rangeSpinner.addChangeListener(this);
            }

            update();
        }

        public void update() {
//            view.projectScene();
//            view.repaint();
        }

        @Override
        public void stateChanged(ChangeEvent e) {

            if (e.getSource() == rangeSpinner) {
                view.range = ((Number) rangeSpinner.getValue()).doubleValue();
            } else if (e.getSource() == tiltSlider) {
                view.tiltAngle = ((Number) tiltSlider.getValue()).intValue();
            }
            update();
        }

        @Override
        public synchronized void mouseClicked(MouseEvent e) {

            double range = view.range;
            if (e.isShiftDown())
                range *= 0.75;
            else
                range *= 1.25;

            if (range < minRange) range = minRange;
            if (range > maxRange) range = maxRange;
            rangeSpinner.setValue(range);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            prevX = e.getX();
            prevY = e.getY();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            final int deltaX = e.getX() - prevX;
            final int deltaY = e.getY() - prevY;

            view.offsetX += deltaX;
            view.offsetY += deltaY;

            prevX = e.getX();
            prevY = e.getY();

            update();
        }

        @Override
        public void mouseMoved(MouseEvent e) {
        }
    }

}