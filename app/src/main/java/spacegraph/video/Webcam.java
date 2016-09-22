package spacegraph.video;

import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import com.github.sarxos.webcam.WebcamEvent;
import com.github.sarxos.webcam.WebcamListener;
import nars.$;
import nars.remote.SwingAgent;
import nars.video.CameraSensor;
import nars.video.ImageCamera;

import java.awt.image.BufferedImage;


public class Webcam extends ImageCamera {

    private final com.github.sarxos.webcam.Webcam webcam;
    private BufferedImage frame = null;

    /** opens the default system webcam and tries to resize to the specified dimensions */
    public Webcam(int desiredWidth, int desiredHeight, boolean showPreview) {
        webcam = com.github.sarxos.webcam.Webcam.getDefault();
        UtilWebcamCapture.adjustResolution(webcam, desiredWidth, desiredHeight);

        webcam.addWebcamListener(new WebcamListener() {
            public ImagePanel gui;

            @Override
            public void webcamOpen(WebcamEvent webcamEvent) {
                System.out.println("Webcam start");

                if (showPreview) {
                    synchronized (webcam) {
                        assert(gui==null);
                        gui = new ImagePanel();
                        gui.setPreferredSize(webcam.getViewSize());
                    }
                }


                ShowImages.showWindow(gui, "CAM");
            }

            @Override
            public void webcamClosed(WebcamEvent webcamEvent) {
                System.out.println("Webcam stop");
                synchronized(webcam) {
                    if (gui!=null) {
                        gui.setVisible(false);
                        gui.removeAll();
                        gui = null;
                    }
                }
            }

            @Override
            public void webcamDisposed(WebcamEvent webcamEvent) {

            }

            @Override
            public void webcamImageObtained(WebcamEvent webcamEvent) {
                //System.out.println(webcamEvent);
                //System.out.println(webcamEvent.getImage());

                //BufferedImage image = webcam.getImage();

                BufferedImage i = webcamEvent.getImage();
                if (gui!=null)
                    gui.setBufferedImage(i);

                frame = i;
                update(i);

                //gui.setBufferedImageSafe();
            }
        });
        webcam.open(true);

    }

    @Override
    public BufferedImage get() {
        return frame;
    }

    protected void update(BufferedImage nextFrame) {

    }

    public static void main(String[] args) {

        SwingAgent.run(n -> new SwingAgent(n, 0) {

            {
                Webcam webcam = new Webcam(800,600, true);
                addCamera("webcam", webcam, 64,64, (v) -> $.t(v, alpha));
            }

            @Override
            protected float reward() {
                return 0;
            }
        }, 5000);

        // Open a webcam at a resolution close to 640x480
        //com.github.sarxos.webcam.Webcam webcam = UtilWebcamCapture.openDefault(640, 480);



//        while( true ) {
//
//            if (webcam.isImageNew()) {
//
//            }
//        }
    }
}