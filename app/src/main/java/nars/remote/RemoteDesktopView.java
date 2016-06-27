package nars.remote;

import boofcv.struct.image.InterleavedU8;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import nars.util.FX;
import nars.vision.ImageContext;
import nars.vision.ImageCrop;
import org.jfxvnc.net.rfb.render.RenderCallback;
import org.jfxvnc.net.rfb.render.rect.ImageRect;

import java.nio.ByteBuffer;


public class RemoteDesktopView  {




    public static class ImageViewWindow extends Stage {

        private final StackPane bp;
        private final Scene scene;
        private ImageView[] imv;

        public ImageViewWindow() {
            this(null);
        }
        public ImageViewWindow(ImageView... imv) {
            super();

            bp = new StackPane();

            scene = new Scene(bp, 600, 330);

            bp.prefWidthProperty().bind(scene.widthProperty());
            bp.prefHeightProperty().bind(scene.widthProperty());


            setScene(scene);

            setImages(imv);



            show();

        }

        private void setImages(ImageView... imv) {
            this.imv = imv;

            if (imv!=null) {
                for (ImageView i : imv) {
                    i.fitWidthProperty().bind(scene.widthProperty());
                    i.fitHeightProperty().bind(scene.heightProperty());
                }

                bp.getChildren().setAll(imv);
            } else {
                bp.getChildren().clear();
            }
        }
    }

    public static void main(String[] args) {


        final ImageView imv = new ImageView();
        final ImageView retina = new ImageView();

        FX.run(()->{

            new ImageViewWindow(imv);
            new ImageViewWindow(retina);

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
                        WritablePixelFormat<ByteBuffer> format = PixelFormat.getByteBgraInstance();
                        image.getPixelReader().getPixels(0,0, W, H, format, u8img.data, 0, W*4);


                        ImageContext t1 = ImageContext.seq(this, new ImageCrop());
                        t1.apply(null);
                        InterleavedU8 output = t1.output;
                        int OW = output.width;
                        int OH = output.height;
                        if (retina.getImage() == null || retina.getImage().getWidth()!=OW || retina.getImage().getHeight()!=OH) {
                            retina.setImage(new WritableImage(OW, OH));
                        }
                        ((WritableImage)retina.getImage()).getPixelWriter()
                                .setPixels(0,0,OW,OH,format, output.data, output.getStartIndex() , output.getStride());





                        callback.renderComplete();
                    }
                };


            } catch (InterruptedException e) {
                e.printStackTrace();
            }



        }).start();

    }


}
