package spacegraph.video;

import boofcv.io.webcamcapture.UtilWebcamCapture;
import com.github.sarxos.webcam.Webcam;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.util.texture.Texture;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import nars.data.Range;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.obj.RectWidget;
import spacegraph.render.Draw;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import static javafx.application.Platform.runLater;

//TODO convert to SpaceGraph Surface
public class WebcamSurface extends Surface implements Runnable {

    //private SourceDataLine mLine;
    // private ShortBuffer audioSamples;
    public Webcam webcam;

    boolean running = true;

    final int fps = 15;

    @Range(min=0, max=1)
    public final MutableFloat cpuThrottle = new MutableFloat(0.5f);


    final static Logger logger = LoggerFactory.getLogger(WebcamSurface.class);
    private ByteBuffer image;
    private Dimension size;


    public WebcamSurface(int w, int h) {


        logger.info("Webcam Devices: {} ", Webcam.getWebcams());

        try {
            // Open a webcam at a resolution close to 640x480
            webcam = UtilWebcamCapture.openDefault(w, h);


            //getChildren().add(view);

        } catch (Exception e) {
            logger.error("{}", e);
            //getChildren().add(new Label(e.toString()));
        }



//        try {
//            final int audioFPS = 10;
//            WaveCapture au = new WaveCapture(new AudioSource(0, audioFPS), audioFPS);
//            Surface mp = au.newMonitorPane();
//            //mp.setAlignment(Pos.BOTTOM_RIGHT);
//
//            //widthProperty().multiply(0.5);
//            //mp.setFillWidth(true);
//
//
//            //mp.widthProperty()
//
//            //mp.maxWidth(Double.MAX_VALUE);
//            //mp.maxHeight(Double.MAX_VALUE);
//            //getChildren().add(mp);
//        } catch (Exception e) {
//            logger.error("{}", e);
//            //getChildren().add(new Label(e.toString()));
//        }


        // Create the panel used to display the image and
        //ImagePanel gui = new ImagePanel();
        //gui.setPreferredSize(webcam.getViewSize());

//        BorderPane control = new POJOPane(this);
//        //control.setStyle("-fx-background-color: gray");
//        control.setStyle("-fx-text-fill: gray");
//        //control.setBlendMode(BlendMode.EXCLUSION);
//        control.setOpacity(0.92);
//
//        VBox wcon = new VBox(control);
//        wcon.prefWidth(150);
//        wcon.maxWidth(150);
//        wcon.setFillWidth(false);
//        wcon.setAlignment(Pos.CENTER_LEFT);
//        //wcon.(150);
//
//        getChildren().add(wcon);

        try {

            new Thread(this).start();

        } catch (Exception e) {
            //getChildren().add(new Label(e.toString()));
        }

    }

    /** TODO use GL textures */
    @Override protected void paint(GL2 gl) {
        super.paint(gl);

        if (size == null)
            return;

        int w = (int)size.getWidth();
        int h = (int)size.getHeight();
        float y = 0;
        float dx = 1f/w;
        float dy = 1f/h;
        int k = 0;
        for (int i = 0; i < w; i++) {
            float x = 0;
            for (int j = 0; j < h; j++) {
                byte r = image.get(k++);
                byte g = image.get(k++);
                byte b = image.get(k++);

                gl.glColor3ub(r, g, b);
                Draw.rect(gl, x, y, dx, dy);
                x += dx;
            }
            y += dy;
        }



    }

    public static void main(String[] args) {

        new SpaceGraph(new RectWidget(new WebcamSurface(320,200), 16,8)).show(1200,1200);

//        NARfx.run((a, b) -> {
//
//
//            Pane bv = new WebcamFX();
//
//
//            b.setWidth(1000);
//            b.setHeight(1000);
//            b.setScene(new Scene(bv));
//
//            b.show();
//
//            //b.sizeToScene();
//
//        });


    }

    @Override
    public void run() {
        while (running) {
            if (webcam.isOpen() && webcam.isImageNew()) {
                image = webcam.getImageBytes();
                size = webcam.getViewSize();

//                BufferedImage bimage = process(webcam.getImage());
//
//                //TODO blit the image directly, this is likely not be the most efficient:
//                image = SwingFXUtils.toFXImage(bimage, image);
//
//                WritableImage finalImage = process(image);
//                runLater(() -> view.setImage(finalImage));
            }

            try {
                Thread.sleep((long) (1000.0 / fps));
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * after webcam input
     */
    protected BufferedImage process(BufferedImage img) {
        return img;
    }

    /**
     * before display output
     */
    protected WritableImage process(WritableImage finalImage) {
        return finalImage;
    }
}