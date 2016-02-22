package nars.video;

import boofcv.io.webcamcapture.UtilWebcamCapture;
import com.github.sarxos.webcam.Webcam;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import nars.data.Range;
import nars.guifx.NARfx;
import nars.guifx.util.NSlider;
import nars.guifx.util.POJOPane;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;

import static javafx.application.Platform.runLater;


public class WebcamFX extends StackPane implements Runnable {

    //private SourceDataLine mLine;
    // private ShortBuffer audioSamples;
    public ImageView view;
    public Webcam webcam = null;

    boolean running = true;

    final int fps = 15;

    @Range(min=0, max=1)
    public final MutableFloat cpuThrottle = new MutableFloat(0.5f);


    final static Logger logger = LoggerFactory.getLogger(WebcamFX.class);


    public WebcamFX() {

        maxWidth(Double.MAX_VALUE);
        maxHeight(Double.MAX_VALUE);

        logger.info("Webcam Devices: {} ", Webcam.getWebcams());

        try {
            // Open a webcam at a resolution close to 640x480
            webcam = UtilWebcamCapture.openDefault(800, 600);
            view = new ImageView();
            view.fitWidthProperty().bind(widthProperty());
            view.fitHeightProperty().bind(heightProperty());
            //view.prefWidth(webcam.getViewSize().getWidth());
            //view.prefHeight(webcam.getViewSize().getHeight());

//            view.maxWidth(Double.MAX_VALUE);
//            view.maxHeight(Double.MAX_VALUE);
            //setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

            //view.maxWidth(Double.MAX_VALUE);
            //view.maxHeight(Double.MAX_VALUE);
            getChildren().add(view);

        } catch (Exception e) {
            logger.error("{}", e);
            getChildren().add(new Label(e.toString()));
        }



        try {
            final int audioFPS = 10;
            WaveCapture au = new WaveCapture(new AudioSource(0, audioFPS), audioFPS);
            VBox mp = au.newMonitorPane();
            mp.setAlignment(Pos.BOTTOM_RIGHT);

            //widthProperty().multiply(0.5);
            //mp.setFillWidth(true);


            //mp.widthProperty()

            //mp.maxWidth(Double.MAX_VALUE);
            //mp.maxHeight(Double.MAX_VALUE);
            getChildren().add(mp);
        } catch (Exception e) {
            logger.error("{}", e);
            getChildren().add(new Label(e.toString()));
        }


        // Create the panel used to display the image and
        //ImagePanel gui = new ImagePanel();
        //gui.setPreferredSize(webcam.getViewSize());

        {
            BorderPane control = new POJOPane(this);
            //control.setStyle("-fx-background-color: gray");
            control.setStyle("-fx-text-fill: gray");
            control.setBlendMode(BlendMode.EXCLUSION);
            control.setOpacity(0.92);

            VBox wcon = new VBox(control);
            wcon.prefWidth(150);
            wcon.maxWidth(150);
            wcon.setFillWidth(false);
            wcon.setAlignment(Pos.CENTER_LEFT);
            //wcon.(150);

            getChildren().add(wcon);

        }
        try {

            new Thread(this).start();

        } catch (Exception e) {
            getChildren().add(new Label(e.toString()));
        }

    }

    public static void main(String[] args) {

        NARfx.run((a, b) -> {


            Pane bv = new WebcamFX();


            b.setWidth(1000);
            b.setHeight(1000);
            b.setScene(new Scene(bv));

            b.show();

            //b.sizeToScene();

        });


    }

    @Override
    public void run() {
        WritableImage image = null;
        while (running) {
            if (webcam.isOpen() && webcam.isImageNew()) {

                BufferedImage bimage = process(webcam.getImage());

                //TODO blit the image directly, this is likely not be the most efficient:
                image = SwingFXUtils.toFXImage(bimage, image);

                WritableImage finalImage = process(image);
                runLater(() -> view.setImage(finalImage));
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