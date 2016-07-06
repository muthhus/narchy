/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes.video;

import java.awt.image.BufferedImage;

/**
 *
 * @author Andrew
 */
public class RGBRenderer extends Renderer {

    @Override
    public BufferedImage render(int[] nespixels, int[] bgcolors, boolean dotcrawl, BufferedImage lastFrame) {
        //and now replace the nes color numbers with rgb colors (respecting color emph bits)
        nespixels = nespixels.clone();
        for (int i = 0; i < nespixels.length; ++i) {
            nespixels[i] = NesColors.col[(nespixels[i] & 0x1c0) >> 6][nespixels[i] & 0x3f];
        }
        return getImageFromArray(nespixels, 256 * clip, 256, 240 - 2 * clip, lastFrame);
    }
}
