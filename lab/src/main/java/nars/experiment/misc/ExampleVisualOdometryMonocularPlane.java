package nars.experiment.misc;

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.abst.sfm.AccessPointTracks3D;
import boofcv.abst.sfm.d3.MonocularPlaneVisualOdometry;
import boofcv.abst.sfm.d3.VisualOdometry;
import boofcv.alg.tracker.klt.PkltConfig;
import boofcv.factory.feature.tracker.FactoryPointTracker;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.io.MediaManager;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.calib.MonoPlaneParameters;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import spacegraph.SpaceGraph;
import spacegraph.video.WebCam;

import java.io.StringReader;

/**
 * Bare bones example showing how to estimate the camera's ego-motion using a single camera and a known
 * plane. Additional information on the scene can be optionally extracted from the algorithm,
 * if it implements AccessPointTracks3D.
 *
 * @author Peter Abeles
 */
public class ExampleVisualOdometryMonocularPlane {

    public static void main(String args[]) {

        MediaManager media = DefaultMediaManager.INSTANCE;

        //String directory = UtilIO.pathExample("vo/drc/");

        //Webcam webcam = UtilWebcamCapture.openDefault(640, 480);
        WebCam webcam = new WebCam(640, 480);


        SpaceGraph.window(
                //new Cuboid(new WebcamSurface(320, 200),4,4), 1200, 1200);
                webcam.surface(), 1200, 1200);

        // load camera description and the video sequence
        Se3_F64 planeToCamera = new Se3_F64();
        planeToCamera.setTranslation(0, 0, 1);
        MonoPlaneParameters calibration = new MonoPlaneParameters(CalibrationIO.load(
                new StringReader("pinhole:\n" +
                        "  fx: 529.0340750592192\n" +
                        "  fy: 528.7980189489572\n" +
                        "  cx: 316.67722865086887\n" +
                        "  cy: 226.93146144997266\n" +
                        "  width: 640\n" +
                        "  height: 480\n" +
                        "  skew: 0.0\n" +
                        "model: pinhole_radial_tangential\n" +
                        "radial_tangential:\n" +
                        "  radial:\n" +
                        "  - -0.1287710526409107\n" +
                        "  - -0.10596851121196668\n" +
                        "  t1: 0.0\n" +
                        "  t2: 0.0")), planeToCamera);

        //media.openFile(directory + "mono_plane.yaml"));
        //SimpleImageSequence<GrayU8> video =
        //media.openVideo(directory + "left.mjpeg", ImageType.single(GrayU8.class));

        // specify how the image features are going to be tracked
        PkltConfig configKlt = new PkltConfig();
        configKlt.pyramidScaling = new int[]{1, 2, 4, 8};
        configKlt.templateRadius = 3;
        ConfigGeneralDetector configDetector = new ConfigGeneralDetector(600, 3, 1);

        PointTracker<GrayU8> tracker = FactoryPointTracker.klt(configKlt, configDetector, GrayU8.class, null);

        // declares the algorithm
        MonocularPlaneVisualOdometry<GrayU8> visualOdometry =
                FactoryVisualOdometry.monoPlaneInfinity(75, 2, 1.5, 200, tracker, ImageType.single(GrayU8.class));

        // Pass in intrinsic/extrinsic calibration.  This can be changed in the future.
        visualOdometry.setCalibration(calibration);

        // Process the video sequence and output the location plus number of inliers
        while (webcam.webcam.isOpen()) {
            GrayU8 image = //video.next();
                    ConvertBufferedImage.convertFrom(webcam.webcam.getImage(), (GrayU8) null);

            if (!visualOdometry.process(image)) {
                System.out.println("Fault!");
                visualOdometry.reset();
            }

            Se3_F64 leftToWorld = visualOdometry.getCameraToWorld();
            Vector3D_F64 T = leftToWorld.getT();

            System.out.printf("Location %8.2f %8.2f %8.2f      inliers %s\n", T.x, T.y, T.z, inlierPercent(visualOdometry));
            System.out.println(tracker.getNewTracks(null));
        }
    }

    /**
     * If the algorithm implements AccessPointTracks3D, then count the number of inlier features
     * and return a string.
     */
    public static String inlierPercent(VisualOdometry<?> alg) {
        if (!(alg instanceof AccessPointTracks3D))
            return "";

        AccessPointTracks3D access = (AccessPointTracks3D) alg;

        int count = 0;
        int N = access.getAllTracks().size();
        for (int i = 0; i < N; i++) {
            if (access.isInlier(i))
                count++;
        }

        return String.format("%%%5.3f", 100.0 * count / N);
    }
}