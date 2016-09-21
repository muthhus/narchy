package nars.video;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;

import static nars.video.PixelCamera.*;

/**
 * exposes a buffered image as a camera video source
 */
public class ImageCamera implements PixelCamera, Supplier<BufferedImage> {

    Supplier<BufferedImage> source;
    public BufferedImage out;


    public ImageCamera() {

    }
//
//    public ImageCamera(Supplier<BufferedImage> i) {
//        this.source = i;
//        update();
//    }

    @Override
    public int width() {
        return out.getWidth();
    }

    @Override
    public int height() {
        return out.getHeight();
    }


    @Override
    public void update() {
        if (this.source!=null) //get next frame
            out = source.get();
    }

    public void see(EachPixelRGB p) {
        final BufferedImage b = this.out;
        if (b == null)
            return;

        int height = height();
        int width = width();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                p.pixel(x, y, b.getRGB(x, y));
            }
        }
    }

    @Override public float brightness(int xx, int yy) {
        return out!=null ? decodeRed(out.getRGB(xx, yy)) : Float.NaN;
    }

//    public void updateBuffered(EachPixelRGBf m) {
//        see(
//                (x, y, p) -> {
//                    intToFloat(m, x, y, p);
//                }
//        );
//    }

    public float red(int x, int y) {
        return outsideBuffer(x, y) ? Float.NaN : decodeRed(out.getRGB(x, y));
    }
    public float green(int x, int y) {
        return outsideBuffer(x, y) ? Float.NaN : decodeGreen(out.getRGB(x, y));
    }
    public float blue(int x, int y) { return outsideBuffer(x, y) ? Float.NaN : decodeBlue(out.getRGB(x,y)); }

    public boolean outsideBuffer(int x, int y) {
        return out == null || (x < 0) || (y < 0) || (x >= out.getWidth()) || (y >= out.getHeight());
    }

    /** for chaining these together */
    @Override public BufferedImage get() {
        return out;
    }
}
