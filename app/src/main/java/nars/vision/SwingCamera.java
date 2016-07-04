package nars.vision;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Captures a awt/swing component to a bitmap and scales it down, returning an image pixel by pixel
 */
public class SwingCamera implements PixelCamera {

    private final Container component;
    private BufferedImage big;
    private BufferedImage small;
    public int width;
    public int height;
    private Graphics2D smallGfx;

    public SwingCamera(Container component, int targetWidth, int targetHeight) {
        this.component = component;
        this.width = targetWidth;
        this.height = targetHeight;
    }

    public void updateMono(PerPixelMono m) {
        update((x,y,p)-> {
            int r = (p & 0x00ff0000) >> 16;
            int g = (p & 0x0000ff00) >> 8;
            int b = (p & 0x000000ff);

            m.pixel(x, y, ((r+g+b) / 256f)/3f);
        });
    }
    public void updateMono(PerIndexMono m) {
        updateMono((x,y,p)-> {
            m.pixel(width * y + x, p);
        });
    }

    @Override
    public void update(PerPixelRGB p) {

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
