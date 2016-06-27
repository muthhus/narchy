package nars.vision;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Captures a swing component to a bitmap and scales it down, returning an image pixel by pixel
 */
public class SwingCamera implements PixelCamera {

    private final JComponent component;
    private BufferedImage big;
    private BufferedImage small;
    int width, height;
    private Graphics2D smallGfx;

    public SwingCamera(JComponent component, int targetWidth, int targetHeight) {
        this.component = component;
        this.width = targetWidth;
        this.height = targetHeight;
    }

    @Override
    public void update(PerPixel p) {

        big = ScreenImage.get(component, big);

        final int width = this.width;
        final int height = this.height;

        if (small == null || small.getWidth()!=width || small.getHeight()!=height) {
            small = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            smallGfx = small.createGraphics(); //create a graphics object to paint to
            smallGfx.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        }

        final BufferedImage small = this.small;
        smallGfx.fillRect(0, 0, width, height); //TODO add fade option
        smallGfx.drawImage(big, 0, 0, width, height, null); //draw the image scaled


        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                p.pixel(x, y, small.getRGB(x, y));
            }
        }
    }

}
