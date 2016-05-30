package nars.op.uibot;

import boofcv.gui.image.ImageZoomPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.ConvertRaster;
import boofcv.struct.image.InterleavedU8;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import nars.util.FX;
import org.jfxvnc.net.rfb.render.RenderCallback;
import org.jfxvnc.net.rfb.render.rect.ImageRect;

import javax.swing.*;
import java.awt.image.BufferedImage;


public class RemoteDesktopView  {


    public static class ImageViewWindow extends Stage {

        private final ImageView imv;

        public ImageViewWindow(ImageView imv) {
            super();
            this.imv = imv;

            StackPane bp = new StackPane(imv);


            Scene scene = new Scene(bp, 600, 330);

            imv.fitWidthProperty().bind(scene.widthProperty());
            imv.fitHeightProperty().bind(scene.heightProperty());

            setScene(scene);
            show();

        }
    }

    public static void main(String[] args) {


        final ImageView imv = new ImageView();
        FX.run(()->{

            new ImageViewWindow(imv);

        });

        new Thread(() -> {
            try {
                RemoteConnection rc = new RemoteConnection("localhost", 5900, "vnc") {

                    //final ImageContext t1 = ImageContext.seq(this, new ImageDistortion());

                    @Override
                    public void resize(int frameWidth, int frameHeight) {
                        super.resize(frameWidth, frameHeight);
                        imv.setImage(image);

                    }

                    @Override
                    public void render(ImageRect rect, RenderCallback callback) {
                        super.render(rect, callback);


                        int W = (int) image.getWidth();
                        int H = (int) image.getHeight();
                        if (u8img==null || u8img.width!=W || u8img.height!=H) {
                            u8img = new InterleavedU8(W, H, 4);
                        }
                        image.getPixelReader().getPixels(0,0, W, H, WritablePixelFormat.getByteBgraInstance(), u8img.data, 0, W*4);


                        ImageContext t1 = ImageContext.seq(this, new ImageDistortion());
                        t1.apply(null);
                        int OW = t1.output.width;
                        int OH = t1.output.height;





                        callback.renderComplete();
                    }
                };


            } catch (InterruptedException e) {
                e.printStackTrace();
            }



        }).start();

    }


}
