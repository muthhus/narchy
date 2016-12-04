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
import boofcv.gui.d3.PointCloudTiltPanel;
import boofcv.gui.feature.AssociationPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.gui.stereo.RectifiedPairPanel;
import boofcv.io.image.ConvertRaster;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.DoNothingTransform_F64;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.*;
import com.github.sarxos.webcam.Webcam;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import nars.util.Util;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelManager;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DenseMatrix64F;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


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
    private static final int minDisparity = 15;
    private static final int maxDisparity = 100;

    public static void main(String args[]) {
        // specify location of images and calibration
//        String calibDir = UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_Chess/");
//        String imageDir = UtilIO.pathExample("stereo/");

        // Camera parameters
        IntrinsicParameters intrinsic = new IntrinsicParameters(); //CalibrationIO.load(new File(calibDir , "intrinsic.yaml"));
        intrinsic.setFx(701.0116882676376);
        intrinsic.setFy(698.6537946928421);


        intrinsic.setCx(308.4551818095542);
        intrinsic.setCy(246.84300560315452);
        intrinsic.setWidth(640);
        intrinsic.setHeight(480);
        intrinsic.setSkew(0);
        intrinsic.setRadial(new double[] { -0.25559248570886445, 0.09997127476560613 });
        intrinsic.setT1(0);
        intrinsic.setT2(0);


//
//		// Input images from the camera moving left to right
//		BufferedImage origLeft = UtilImageIO.loadImage(imageDir , "mono_wall_01.jpg");
//		BufferedImage origRight = UtilImageIO.loadImage(imageDir, "mono_wall_02.jpg");

        Webcam w = Webcam.getDefault();
        w.open(false);

        InterleavedU8 origLeft = new InterleavedU8((int)w.getViewSize().getWidth(), (int)w.getViewSize().getHeight(), 3);
        InterleavedU8 origRight = new InterleavedU8((int)w.getViewSize().getWidth(), (int)w.getViewSize().getHeight(), 3);

        System.out.println("taking left");
        w.getImageBytes(ByteBuffer.wrap(origLeft.data));

        Util.sleep(1000);

        System.out.println("taking right");
        w.getImageBytes(ByteBuffer.wrap(origRight.data));

        // Input images with lens distortion

        GrayU8 distortedLeft = ConvertImage.average(origLeft, null);
        GrayU8 distortedRight = ConvertImage.average(origRight, null);

        // matched features between the two images
        List<AssociatedPair> matchedFeatures = ExampleFundamentalMatrix.computeMatches(
                ConvertImage.convert(distortedLeft,(GrayF32)null),
                ConvertImage.convert(distortedRight,(GrayF32)null)
        );

        // convert from pixel coordinates into normalized image coordinates
        List<AssociatedPair> matchedCalibrated = convertToNormalizedCoordinates(matchedFeatures, intrinsic);

        // Robustly estimate camera motion
        List<AssociatedPair> inliers = new ArrayList<>();
        Se3_F64 leftToRight = estimateCameraMotion(intrinsic, matchedCalibrated, inliers);

        drawInliers(origLeft, origRight, intrinsic, inliers);

        // Rectify and remove lens distortion for stereo processing
        DenseMatrix64F rectifiedK = new DenseMatrix64F(3, 3);
        GrayU8 rectifiedLeft = distortedLeft.createSameShape();
        GrayU8 rectifiedRight = distortedRight.createSameShape();

        rectifyImages(distortedLeft, distortedRight, leftToRight, intrinsic, rectifiedLeft, rectifiedRight, rectifiedK);

        // compute disparity
        StereoDisparity<GrayS16, GrayF32> disparityAlg =
                FactoryStereoDisparity.regionSubpixelWta(DisparityAlgorithms.RECT_FIVE,
                        minDisparity, maxDisparity, 5, 5, 20, 1, 0.1, GrayS16.class);

        // Apply the Laplacian across the image to add extra resistance to changes in lighting or camera gain
        GrayS16 derivLeft = new GrayS16(rectifiedLeft.width, rectifiedLeft.height);
        GrayS16 derivRight = new GrayS16(rectifiedLeft.width, rectifiedLeft.height);
        LaplacianEdge.process(rectifiedLeft, derivLeft);
        LaplacianEdge.process(rectifiedRight, derivRight);

        // process and return the results
        disparityAlg.process(derivLeft, derivRight);
        GrayF32 disparity = disparityAlg.getDisparity();

        // show results
        BufferedImage visualized = VisualizeImageData.disparity(disparity, null, minDisparity, maxDisparity, 0);

        BufferedImage outLeft = new BufferedImage(rectifiedLeft.getWidth(), rectifiedLeft.getHeight(), BufferedImage.TYPE_INT_RGB);
        grayToBuffered(rectifiedLeft, outLeft);

        BufferedImage outRight = new BufferedImage(rectifiedRight.getWidth(), rectifiedRight.getHeight(), BufferedImage.TYPE_INT_RGB);
        grayToBuffered(rectifiedRight, outRight);

        ShowImages.showWindow(new RectifiedPairPanel(true, outLeft, outRight), "Rectification");
        ShowImages.showWindow(visualized, "Disparity");

        showPointCloud(disparity, outLeft, leftToRight, rectifiedK, minDisparity, maxDisparity);

        System.out.println("Total found " + matchedCalibrated.size());
        System.out.println("Total Inliers " + inliers.size());
    }


    public static void grayToBuffered(GrayU8 src, BufferedImage dst) {

        final int width = dst.getWidth();
        final int height = dst.getHeight();

        byte[] data = src.data;
        for (int y = 0; y < height; y++) {
            int indexSrc = src.startIndex + src.stride * y;

            for (int x = 0; x < width; x++) {
                int v = data[indexSrc++] & 0xFF;

                int argb = v << 16 | v << 8 | v;

                dst.setRGB(x, y, argb);
            }
        }

    }

    /**
     * Estimates the camera motion robustly using RANSAC and a set of associated points.
     *
     * @param intrinsic   Intrinsic camera parameters
     * @param matchedNorm set of matched point features in normalized image coordinates
     * @param inliers     OUTPUT: Set of inlier features from RANSAC
     * @return Found camera motion.  Note translation has an arbitrary scale
     */
    public static Se3_F64 estimateCameraMotion(IntrinsicParameters intrinsic,
                                               List<AssociatedPair> matchedNorm, List<AssociatedPair> inliers) {
        ModelMatcher<Se3_F64, AssociatedPair> epipolarMotion =
                FactoryMultiViewRobust.essentialRansac(new ConfigEssential(intrinsic), new ConfigRansac(200, 0.5));

        if (!epipolarMotion.process(matchedNorm))
            throw new RuntimeException("Motion estimation failed");

        // save inlier set for debugging purposes
        inliers.addAll(epipolarMotion.getMatchSet());

        return epipolarMotion.getModelParameters();
    }

    /**
     * Convert a set of associated point features from pixel coordinates into normalized image coordinates.
     */
    public static List<AssociatedPair> convertToNormalizedCoordinates(List<AssociatedPair> matchedFeatures, IntrinsicParameters intrinsic) {

        PointTransform_F64 p_to_n = LensDistortionOps.transformPoint(intrinsic).undistort_F64(true, false);

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
     */
    public static void rectifyImages(GrayU8 distortedLeft,
                                     GrayU8 distortedRight,
                                     Se3_F64 leftToRight,
                                     IntrinsicParameters intrinsic,
                                     GrayU8 rectifiedLeft,
                                     GrayU8 rectifiedRight,
                                     DenseMatrix64F rectifiedK) {
        RectifyCalibrated rectifyAlg = RectifyImageOps.createCalibrated();

        // original camera calibration matrices
        DenseMatrix64F K = PerspectiveOps.calibrationMatrix(intrinsic, null);

        rectifyAlg.process(K, new Se3_F64(), K, leftToRight);

        // rectification matrix for each image
        DenseMatrix64F rect1 = rectifyAlg.getRect1();
        DenseMatrix64F rect2 = rectifyAlg.getRect2();

        // New calibration matrix,
        rectifiedK.set(rectifyAlg.getCalibrationMatrix());

        // Adjust the rectification to make the view area more useful
        RectifyImageOps.allInsideLeft(intrinsic, rect1, rect2, rectifiedK);

        // undistorted and rectify images
        ImageDistort<GrayU8, GrayU8> distortLeft =
                RectifyImageOps.rectifyImage(intrinsic, rect1, BorderType.SKIP, distortedLeft.getImageType());
        ImageDistort<GrayU8, GrayU8> distortRight =
                RectifyImageOps.rectifyImage(intrinsic, rect2, BorderType.SKIP, distortedRight.getImageType());

        distortLeft.apply(distortedLeft, rectifiedLeft);
        distortRight.apply(distortedRight, rectifiedRight);
    }

    /**
     * Draw inliers for debugging purposes.  Need to convert from normalized to pixel coordinates.
     */
    public static void drawInliers(InterleavedU8 left, InterleavedU8 right, IntrinsicParameters intrinsic,
                                   List<AssociatedPair> normalized) {
        PointTransform_F64 n_to_p = LensDistortionOps.transformPoint(intrinsic).distort_F64(false, true);

        List<AssociatedPair> pixels = new ArrayList<>();

        for (AssociatedPair n : normalized) {
            AssociatedPair p = new AssociatedPair();

            n_to_p.compute(n.p1.x, n.p1.y, p.p1);
            n_to_p.compute(n.p2.x, n.p2.y, p.p2);

            pixels.add(p);
        }

        // display the results
        AssociationPanel panel = new AssociationPanel(20);
        panel.setAssociation(pixels);


        //panel.setImages(new BufferedImage(left.data), right);

        ShowImages.showWindow(panel, "Inlier Features");
    }

    /**
     * Show results as a point cloud
     */
    public static void showPointCloud(ImageGray disparity, BufferedImage left,
                                      Se3_F64 motion, DenseMatrix64F rectifiedK,
                                      int minDisparity, int maxDisparity) {
        PointCloudTiltPanel gui = new PointCloudTiltPanel();

        double baseline = motion.getT().norm();

        gui.configure(baseline, rectifiedK, new DoNothingTransform_F64(), minDisparity, maxDisparity);
        gui.process(disparity, left);
        gui.setPreferredSize(new Dimension(left.getWidth(), left.getHeight()));

        ShowImages.showWindow(gui, "Point Cloud");
    }

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
        public static List<AssociatedPair> computeMatches(GrayF32 left, GrayF32 right) {
            DetectDescribePoint detDesc = FactoryDetectDescribe.surfStable(
                    new ConfigFastHessian(1, 2, 200, 1, 9, 4, 4), null, null, GrayF32.class);
//		DetectDescribePoint detDesc = FactoryDetectDescribe.sift(null,new ConfigSiftDetector(2,0,200,5),null,null);

            ScoreAssociation<BrightFeature> scorer = FactoryAssociation.scoreEuclidean(BrightFeature.class, true);
            AssociateDescription<BrightFeature> associate = FactoryAssociation.greedy(scorer, 1, true);

            ExampleAssociatePoints<GrayF32, BrightFeature> findMatches =
                    new ExampleAssociatePoints<>(detDesc, associate, GrayF32.class);

            findMatches.associate(left, right);

            List<AssociatedPair> matches = new ArrayList<>();
            FastQueue<AssociatedIndex> matchIndexes = associate.getMatches();

            for (int i = 0; i < matchIndexes.size; i++) {
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
        public void associate( T inputA , T inputB )
        {
//            T inputA = ConvertBufferedImage.convertFromSingle(imageA, null, imageType);
//            T inputB = ConvertImage.convert(imageB, null, imageType);

            // stores the location of detected interest points
            pointsA = new ArrayList<>();
            pointsB = new ArrayList<>();

            // stores the description of detected interest points
            FastQueue<TD> descA = UtilFeature.createQueue(detDesc,100);
            FastQueue<TD> descB = UtilFeature.createQueue(detDesc,100);

            // describe each image using interest points
            describeImage(inputA,pointsA,descA);
            describeImage(inputB,pointsB,descB);

            // Associate features between the two images
            associate.setSource(descA);
            associate.setDestination(descB);
            associate.associate();

            // display the results
            AssociationPanel panel = new AssociationPanel(20);
            panel.setAssociation(pointsA,pointsB,associate.getMatches());
            //panel.setImages(imageA,imageB);

            ShowImages.showWindow(panel,"Associated Features",true);
        }

        /**
         * Detects features inside the two images and computes descriptions at those points.
         */
        private void describeImage(T input, List<Point2D_F64> points, FastQueue<TD> descs )
        {
            detDesc.detect(input);

            for( int i = 0; i < detDesc.getNumberOfFeatures(); i++ ) {
                points.add( detDesc.getLocation(i).copy() );
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
}