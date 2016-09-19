package nars.video;


import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;

import static java.awt.Image.SCALE_FAST;

/**
 * Pan/Zoom filter for a BuferredImage source
 */
public class Scale extends ImageCamera {

    private final Supplier<BufferedImage> src;
    private Graphics2D outgfx;

    /**
     * output pixel width / height
     */
    public int pw, ph;

    public Scale(Supplier<BufferedImage> source, int pw, int ph) {
        super();

        this.src = source;
        this.pw = pw;
        this.ph = ph;
    }

    @Override
    public int width() {
        return pw;
    }

    @Override
    public int height() {
        return ph;
    }

    @Override
    public void update() {
        if (src instanceof PixelCamera)
            ((PixelCamera) src).update();

        Image in = src.get();
        if (in == null)
            return;

        if (out == null || out.getWidth() != pw || out.getHeight() != ph) {

            out = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_RGB);
            outgfx = out.createGraphics(); //create a graphics object to paint to
            outgfx.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
                    //RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
            );
            outgfx.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY
                    //RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED
            );
            outgfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                    //RenderingHints.VALUE_ANTIALIAS_OFF
            );
        }

        outgfx.setColor(Color.BLACK);
        outgfx.fillRect(0, 0, pw, ph); //TODO add fade option
        outgfx.drawImage(in, 0, 0, pw, ph, null); //draw the image scaled

    }

}
