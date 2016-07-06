package nars.vision;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Captures a awt/swing component to a bitmap and scales it down, returning an image pixel by pixel
 */
public class SwingCamera implements PixelCamera {

    static final Logger logger = LoggerFactory.getLogger(SwingCamera.class);

    private final Container component;
    private BufferedImage big;
    private BufferedImage out;
    public int width;
    public int height;
    private Graphics2D outgfx;
    public Rectangle input;

    public SwingCamera(Container component) {
        this(component, 0, 0);
    }

    public SwingCamera(Container component, int targetWidth, int targetHeight) {
        this.component = component;
        input(0, 0, component.getWidth(), component.getHeight());
        output(targetWidth, targetHeight);
    }

    public void output(int targetWidth, int targetHeight) {
        this.width = targetWidth;
        this.height = targetHeight;
    }

    public void input(int x, int y, int w, int h) {
        this.input = new Rectangle(x, y, w, h);
    }


    public int inWidth() {
        return component.getWidth();
    }

    public int inHeight() {
        return component.getHeight();
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public void update(PerPixelRGB p) {

        if (!update())
            return;

        updateBuffered(p);
    }

    public synchronized boolean update() {
        final int width = this.width;
        if (width == 0)
            return false;
        final int height = this.height;
        if (height == 0)
            return false;


        //logger.info("{} capturing {} scaled to {},{}", component.getClass().getSimpleName(), input, width, height);

        big = ScreenImage.get(component, big, input);

        if (out == null || out.getWidth() != width || out.getHeight() != height) {
            out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            outgfx = out.createGraphics(); //create a graphics object to paint to
            outgfx.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            outgfx.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            outgfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }


        outgfx.fillRect(0, 0, width, height); //TODO add fade option
        outgfx.drawImage(big, 0, 0, width, height, null); //draw the image scaled
        return true;
    }

    public void updateBuffered(PerPixelRGB p) {
        final BufferedImage small = this.out;
        if (small == null)
            return;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                p.pixel(x, y, small.getRGB(x, y));
            }
        }
    }

    public void updateBuffered(PerPixelRGBf m) {
        updateBuffered(
                (x, y, p) -> {
                    intToFloat(m, x, y, p);
                }
        );
    }

    public float red(int x, int y) {
        return outsideBuffer(x, y) ? Float.NaN : decodeRed(out.getRGB(x, y));
    }
    public float green(int x, int y) {
        return outsideBuffer(x, y) ? Float.NaN : decodeGreen(out.getRGB(x,y));
    }
    public float blue(int x, int y) {
        return outsideBuffer(x, y) ? Float.NaN : decodeBlue(out.getRGB(x,y));
    }

    public boolean outsideBuffer(int x, int y) {
        return out == null || (x < 0) || (y < 0) || (x >= out.getWidth()) || (y >= out.getHeight());
    }



}
