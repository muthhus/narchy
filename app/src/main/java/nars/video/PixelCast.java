package nars.video;

import nars.util.Util;
import nars.util.data.random.XorShift128PlusRandom;

import java.awt.image.BufferedImage;
import java.util.Random;

import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.round;
import static nars.util.Util.clamp;

/**
 * 2D flat Raytracing Retina
 */
public class PixelCast implements PixelCamera {

    final BufferedImage source;
    private final int px;
    private final int py;
    float sampleRate = 0.35f;
    final Random rng = new XorShift128PlusRandom(1);

    /**
     * Z = 0: zoomed in all the way
     *   = 1: zoomed out all the way
     *
     */
    float X = 0.5f, Y = 0.5f, Z;

//    public float minX = 0f;
//    public float maxX = 1f;
//    public float minY = 0f;
//    public float maxY = 1f;

    final float[][] pixels;

    private int pixMin = 2;


    public PixelCast(BufferedImage b, int px, int py) {
        this.source = b;
        this.px = px;
        this.py = py;
        this.pixels = new float[px][py];

        this.Z = 1f;
    }

    @Override
    public void update() {



        final BufferedImage b = this.source;
        if (b == null)
            return;
        
        int sw = b.getWidth();
        int sh = b.getHeight();

        float ew = max(Z * sw, pixMin);
        float eh = max(ew * sh / sw, pixMin);

        float minX = X - ew/2f+sw/2;
        float maxX = X + ew/2f+sw/2;
        float minY = Y - eh/2f+sh/2;
        float maxY = Y + eh/2f+sh/2;

        System.out.println(X + "," + Y + "," + Z + ": [" + (minX+maxX)/2f + "@" + minX + "," + maxX + "::"
                                                         + (minY+maxY)/2f + "@" + minY + "," + maxY + "] <- aspect=" + eh + "/" + ew);

        float cx = px/2f;
        float cy = py/2f;

        float radiusHalfLife = (float) Math.ceil(max(px,py)/6); //distance before probability falls off to 50%

        float pxf = (float)px;
        float pyf = (float)py;
        for (int ly = 0; ly < py; ly++) {
            int sy = clamp(Util.lerp(maxY, minY, ly/pyf), 0, sh-1);
            float yDistFromCenter = Math.abs(ly - cy);
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
                int sx = clamp(Util.lerp(maxX, minX, lx/pxf), 0, sw-1);

                //pixel value
                int RGB = b.getRGB(sx, sy);
                float R = PixelCamera.decodeRed(RGB);
                float G = PixelCamera.decodeGreen(RGB);
                float B = PixelCamera.decodeBlue(RGB);
                pixels[lx][ly] = (R + G + B) / 3f;
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
        return pixels[xx][yy];
    }

    public void setZ(float f) {
        Z = u(f);
    }

    static float u(float f) {
        return f/2f+0.5f;
    }

    public void setY(float f) {
        Y = u(f);
    }

    public void setX(float f) {
        X = u(f);
    }
}
