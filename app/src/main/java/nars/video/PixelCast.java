package nars.video;

import nars.util.Util;
import nars.util.data.random.XorShift128PlusRandom;

import java.awt.image.BufferedImage;
import java.util.Random;

import static java.lang.Math.floor;
import static java.lang.Math.round;

/**
 * 2D flat Raytracing Retina
 */
public class PixelCast implements PixelCamera {

    final BufferedImage source;
    private final int px;
    private final int py;
    float sampleRate = 0.35f;
    final Random rng = new XorShift128PlusRandom(1);

    public float minX = 0f;
    public float maxX = 1f;
    public float minY = 0f;
    public float maxY = 1f;

    final float[][] w;


    public PixelCast(BufferedImage b, int px, int py) {
        this.source = b;
        this.px = px;
        this.py = py;
        this.w = new float[px][py];
    }

    @Override
    public void update() {
        final BufferedImage b = this.source;
        if (b == null)
            return;

        //System.out.println(maxX + " " + minX + ":" + maxY + " " + minY);

        int cx = px/2;
        int cy = py/2;
        int sw = b.getWidth();
        int sh = b.getHeight();

        float radiusHalfLife = (float) Math.ceil(Math.max(px,py)/8); //distance before probability falls off to 50%

        float pxf = (float)px;
        float pyf = (float)py;
        for (int ly = 0; ly < py; ly++) {
            int yDistFromCenter = Math.abs(ly - cy);
            for (int lx = 0; lx < px; lx++) {
//                //choose a virtual retina pixel
//                float x =
//                        //rng.nextFloat();
//                        Util.clamp(((float) rng.nextGaussian() + 1.0f) / 2.0f); //resolves the center more clearly
//                float y =
//                        //rng.nextFloat();
//                        Util.clamp(((float) rng.nextGaussian() + 1.0f) / 2.0f);

                //project from the local retina plane
//                int lx = round((px - 1) * x);
//                int ly = round((py - 1) * y);
                float distFromCenter = Math.abs(lx - cx) + yDistFromCenter; //manhattan distance from center

                float clarity = 1f/(1f+distFromCenter/radiusHalfLife);
                if (rng.nextFloat() > clarity)
                    continue;

                //project to the viewed image plane
                int sx = (int) floor((sw - 1) * Util.lerp(maxX, minX, lx/pxf));
                int sy = (int) floor((sh - 1) * Util.lerp(maxY, minY, ly/pyf));

                //pixel value
                int RGB = b.getRGB(sx, sy);
                float R = PixelCamera.decodeRed(RGB);
                float G = PixelCamera.decodeGreen(RGB);
                float B = PixelCamera.decodeBlue(RGB);
                w[lx][ly] = (R + G + B) / 3f;
                //p.pixel(lx, ly, );
            }
        }
    }

    @Override
    public void see(EachPixelRGB p) {


    }

    @Override
    public int width() {
        return px;
    }

    @Override
    public int height() {
        return py;
    }

    @Override
    public float brightness(int xx, int yy) {
        return w[xx][yy];
    }
}
