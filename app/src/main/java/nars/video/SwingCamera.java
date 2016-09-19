package nars.video;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Captures a awt/swing component to a bitmap and scales it down, returning an image pixel by pixel
 */
public class SwingCamera extends BufferedImageCamera {

    static final Logger logger = LoggerFactory.getLogger(SwingCamera.class);

    private final Container component;
    public BufferedImage in;
    private Graphics2D outgfx;
    public Rectangle input;

    public SwingCamera(Container component) {
        this(component, 0, 0);
    }

    public SwingCamera(Container component, int targetWidth, int targetHeight) {
        this.component = component;
        input(0, 0, component.getWidth(), component.getHeight());
        output(targetWidth, targetHeight);
        update();
    }

    public void update() {

        synchronized (component) {
            final int width = this.width;
            final int height = this.height;


            //logger.info("{} capturing {} scaled to {},{}", component.getClass().getSimpleName(), input, width, height);

            in = ScreenImage.get(component, in, input);

            if (out == null || out.getWidth() != width || out.getHeight() != height) {
                out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                outgfx = out.createGraphics(); //create a graphics object to paint to
                outgfx.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                outgfx.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                outgfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            }


            outgfx.fillRect(0, 0, width, height); //TODO add fade option
            outgfx.drawImage(in, 0, 0, width, height, null); //draw the image scaled
        }

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


    /** x and y in 0..1.0, w and h in 0..1.0 */
    public void input(float x, float y, float w, float h) {
        int px = Math.round(inWidth() * x);
        int py = Math.round(inHeight() * y);
        int pw = Math.round(inWidth() * w);
        int ph = Math.round(inWidth() * h);
        input(px, py, pw, ph);
    }
}
