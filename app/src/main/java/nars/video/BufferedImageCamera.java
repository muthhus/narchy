package nars.video;

import java.awt.image.BufferedImage;

import static nars.video.PixelCamera.*;

/**
 * exposes a buffered image as a camera video source
 */
public abstract class BufferedImageCamera implements PixelCamera {
    public BufferedImage out;
    public int width;
    public int height;

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }


    public void see(EachPixelRGB p) {
        final BufferedImage b = this.out;
        if (b == null)
            return;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                p.pixel(x, y, b.getRGB(x, y));
            }
        }
    }

    @Override public float brightness(int xx, int yy) {
        return decodeRed(out.getRGB(xx, yy));
    }

    public void updateBuffered(EachPixelRGBf m) {
        see(
                (x, y, p) -> {
                    intToFloat(m, x, y, p);
                }
        );
    }

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

}
