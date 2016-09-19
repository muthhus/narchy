package nars.video;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;

import static nars.video.PixelCamera.*;

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
    }

    @Override
    public void update(PerPixelRGB p) {

        if (!update())
            return;

        super.update(p);
    }


    public synchronized boolean update() {
        final int width = this.width;
        if (width == 0)
            return false;
        final int height = this.height;
        if (height == 0)
            return false;


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
        return true;
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


}
