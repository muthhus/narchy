package spacegraph.video;

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.abst.sfm.AccessPointTracks3D;
import boofcv.abst.sfm.d3.MonocularPlaneVisualOdometry;
import boofcv.abst.sfm.d3.VisualOdometry;
import boofcv.alg.color.ColorRgb;
import boofcv.alg.tracker.klt.PkltConfig;
import boofcv.factory.feature.tracker.FactoryPointTracker;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.calib.MonoPlaneParameters;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.InterleavedU8;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;

/**
 * Bare bones example showing how to estimate the camera's ego-motion using a single camera and a known
 * plane. Additional information on the scene can be optionally extracted from the algorithm,
 * if it implements AccessPointTracks3D.
 *
 * @author Peter Abeles
 */
public class ExampleVisualOdometryMonocularPlane {

    public static void main(String args[]) {

        //MediaManager media = DefaultMediaManager.INSTANCE;

        //String directory = UtilIO.pathExample("vo/drc/");

        WebCam w = new WebCam(320, 200);


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

        // load camera description and the video sequence
        MonoPlaneParameters calibration =
                new MonoPlaneParameters(intrinsic, new Se3_F64());

        //UtilIO.loadXML(media.openFile(directory + "mono_plane.xml"));
        SimpleImageSequence<GrayU8> video = //media.openVideo(directory + "left.mjpeg", ImageType.single(GrayU8.class));
                new SimpleImageSequence<GrayU8>() {
                    @Override
                    public int getNextWidth() {
                        return w.width;
                    }

                    @Override
                    public int getNextHeight() {
                        return w.height;
                    }

                    @Override
                    public boolean hasNext() {
                        //return w.sequence...
                        return true;
                    }

                    @Override
                    public GrayU8 next() {
                        InterleavedU8 i = new InterleavedU8(w.width, w.height, 3);
                        i.data = w.iimage.data;

                        GrayU8 o = new GrayU8(w.width, w.height);
                        ColorRgb.rgbToGray_Weighted(i, o);

                        n++;

                        return o;
                    }

                    @Override
                    public <InternalImage> InternalImage getGuiImage() {
                        return null;
                    }

                    @Override
                    public void close() {

                    }

                    int n = 0;

                    @Override
                    public int getFrameNumber() {
                        return n;
                    }

                    @Override
                    public void setLoop(boolean loop) {

                    }

                    @Override
                    public ImageType<GrayU8> getImageType() {
                        return ImageType.single(GrayU8.class);
                    }

                    @Override
                    public void reset() {

                    }
                };

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
        while (video.hasNext()) {
            GrayU8 image = video.next();

            if (!visualOdometry.process(image)) {
                System.out.println("Fault!");
                visualOdometry.reset();
            }

            Se3_F64 leftToWorld = visualOdometry.getCameraToWorld();
            Vector3D_F64 T = leftToWorld.getT();

            System.out.printf("Location %8.2f %8.2f %8.2f      inliers %s\n", T.x, T.y, T.z, inlierPercent(visualOdometry));
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