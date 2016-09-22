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

import javax.swing.*;
import java.awt.image.BufferedImage;


public class Webcam extends ImageCamera implements WebcamListener {

    private final com.github.sarxos.webcam.Webcam webcam;
    private final boolean showPreview;
    private BufferedImage frame = null;
    public ImagePanel gui;

    /** opens the default system webcam and tries to resize to the specified dimensions */
    public Webcam(int desiredWidth, int desiredHeight, boolean showPreview) {
        this.showPreview = showPreview;
        webcam = com.github.sarxos.webcam.Webcam.getDefault();
        UtilWebcamCapture.adjustResolution(webcam, desiredWidth, desiredHeight);

        webcam.addWebcamListener(this);

        webcam.open(true);

    }

    @Override
    public void webcamOpen(WebcamEvent webcamEvent) {
        //System.out.println("Webcam start");

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
        //System.out.println("Webcam stop");
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

        BufferedImage i = webcamEvent.getImage();

        frame = i;
        update(i);

        if (gui!=null) {
            SwingUtilities.invokeLater(()-> {
                gui.setBufferedImage(i);
            });
        }


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
                addCamera("webcam", webcam, 128, 128, (v) -> $.t(v, alpha));
            }

            @Override
            protected float reward() {
                return 0;
            }
        }, 5000);

    }
}