/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes.video;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;

/**
 *
 * @author Andrew
 */
public abstract class Renderer {

    private final GraphicsConfiguration gc;
    int width = 256;
    int clip = 8;

    public Renderer() {
        gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
    }

    public abstract BufferedImage render(int[] nespixels, int[] bgcolors, boolean dotcrawl, BufferedImage lastFrame);
    
    public void setClip(int i){
        //how many lines to clip from top + bottom
        clip = i;
    }

    public BufferedImage getImageFromArray(final int[] bitmap, final int offset, final int width, final int height, BufferedImage image) {
        if (image==null || image.getWidth()!=width || image.getHeight()!=height) {
            //image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
            image = gc.createCompatibleImage(width, height);
        }

        final WritableRaster raster = image.getRaster();
        final int[] pixels = ((DataBufferInt) raster.getDataBuffer()).getData();
        System.arraycopy(bitmap, offset, pixels, 0, width * height);
        return image;
    }
}
