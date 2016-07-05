package nars.vision;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Captures a awt/swing component to a bitmap and scales it down, returning an image pixel by pixel
 */
public class SwingCamera implements PixelCamera {

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

        final int width = this.width;
        if (width == 0)
            return;
        final int height = this.height;
        if (height == 0)
            return;

        big = ScreenImage.get(component, big, input);




        if (out == null || out.getWidth()!=width || out.getHeight()!=height) {
            out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            outgfx = out.createGraphics(); //create a graphics object to paint to
            outgfx.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            outgfx.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            outgfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        final BufferedImage small = this.out;
        outgfx.fillRect(0, 0, width, height); //TODO add fade option
        outgfx.drawImage(big, 0, 0, width, height, null); //draw the image scaled


        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                p.pixel(x, y, small.getRGB(x, y));
            }
        }
    }

}
