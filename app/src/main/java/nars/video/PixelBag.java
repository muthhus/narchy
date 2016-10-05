package nars.video;

import nars.NAgent;
import nars.util.Util;
import nars.util.data.random.XorShift128PlusRandom;

import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.function.Supplier;

import static java.lang.Math.max;
import static nars.util.Util.lerp;

/**
 * 2D flat Raytracing Retina
 */
public class PixelBag implements Bitmap2D {

    final Supplier<BufferedImage> source;
    private final int px;
    private final int py;

    final Random rng = new XorShift128PlusRandom(1);

    /**
     * Z = 0: zoomed in all the way
     *   = 1: zoomed out all the way
     *
     */
    float X = 0f;
    float Y = 0f;
    public float Z;

//    public float minX = 0f;
//    public float maxX = 1f;
//    public float minY = 0f;
//    public float maxY = 1f;

    public final float[][] pixels;

    private int pixMin = 3;

    /** increase >1 to allow zoom out beyond input size (ex: thumbnail size) */
    float maxZoomOut = 1;

    public boolean vflip = false;


    public PixelBag(BufferedImage b, int px, int py) {
        this(()->b, px, py);
    }

    public PixelBag(Supplier<BufferedImage> b, int px, int py) {
        this.source = b;
        this.px = px;
        this.py = py;
        this.pixels = new float[px][py];

        this.Z = 1f;
    }

    @Override
    public void update(float frameRate) {



        final BufferedImage b = this.source.get();
        if (b == null)
            return;
        
        int sw = b.getWidth();
        int sh = b.getHeight();

        float ew = max(Z * sw * maxZoomOut, pixMin);
        float eh = max(Z * sh * maxZoomOut, pixMin);


        //margin size
        float mw, mh;
        if (ew > sw) {
            mw = 0;
        } else {
            mw = sw - ew;
        }
        if (eh > sh) {
            mh = 0;
        } else {
            mh = sh - eh;
        }

        float minX = (X*mw);
        float maxX = minX + ew;
        float minY = (Y*mh);
        float maxY = minY + eh;

//        System.out.println(X + "," + Y + "," + Z + ": [" + (minX+maxX)/2f + "@" + minX + "," + maxX + "::"
//                                                         + (minY+maxY)/2f + "@" + minY + "," + maxY + "] <- aspect=" + eh + "/" + ew);

        float cx = px/2f;
        float cy = py/2f;


        //not perfect calculation, because it doesnt account for max/min min/max differences due to non-square dimensions
        //but suffices for now
        float maxCenterDistanceSq = Math.max(cx,cy) * Math.max(cx, cy) * 2;

        float pxf = px-1;
        float pyf = py-1;

        float minClarity = 0.05f/frameRate, maxClarity = 0.8f/frameRate;

        for (int ly = 0; ly < py; ly++) {
            float l = ly / pyf;
            int sy = Math.round(lerp(maxY, minY, !vflip ? l : 1f-l));

            float dy = Math.abs(ly - cy);
            float yDistFromCenterSq = dy * dy;

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
                float dx = Math.abs(lx - cx);
                float distFromCenterSq = dx*dx + yDistFromCenterSq; //manhattan distance from center

                float clarity = lerp(minClarity, maxClarity, distFromCenterSq/maxCenterDistanceSq);
                if (rng.nextFloat() > clarity)
                    continue;

                //project to the viewed image plane
                int sx = Math.round(lerp(maxX, minX, lx/pxf));

                float v;
                if (sx < 0 || sx > sw-1 || sy < 0 || sy > sh-1) {
                    v = 0;
                } else {
                    //pixel value
                    int RGB = b.getRGB(sx, sy);
                    float R = Bitmap2D.decodeRed(RGB);
                    float G = Bitmap2D.decodeGreen(RGB);
                    float B = Bitmap2D.decodeBlue(RGB);
                    v = (R + G + B) / 3f;
                }
                pixels[lx][ly] = v;
            }
        }
    }

    @Override
    public void see(EachPixelRGB p) {
        throw new UnsupportedOperationException("yet");

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

    static float u(float f) {
        return f/2f+0.5f;
    }

    public boolean setZ(float f) {
        Z = u(f);
        return true;
    }

    public boolean setY(float f) {
        Y = u(f);
        return true;
    }

    public boolean setX(float f) {
        X = u(f);
        return true;
    }

    public PixelBag addActions(String termRoot, NAgent a) {
        a.actionBipolar(termRoot + ":moveX", this::setX);
        a.actionBipolar(termRoot + ":moveY", this::setY);
        a.actionBipolar(termRoot + ":zoom", this::setZ);
        return this;
    }

}
