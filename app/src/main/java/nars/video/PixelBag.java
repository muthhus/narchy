package nars.video;

import jcog.random.XorShift128PlusRandom;
import nars.$;
import nars.NAgent;
import nars.concept.ActionConcept;
import nars.term.Term;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import static java.lang.Math.max;
import static jcog.Util.lerp;

/**
 * 2D flat Raytracing Retina
 */
public abstract class PixelBag implements Bitmap2D {

    private final int px;
    private final int py;

    final Random rng = new XorShift128PlusRandom(1);

    /**
     * Z = 0: zoomed in all the way
     * = 1: zoomed out all the way
     */
    float X = 0f;
    float Y = 0f;
    public float Z;

//    public float minX = 0f;
//    public float maxX = 1f;
//    public float minY = 0f;
//    public float maxY = 1f;

    public final float[][] pixels;

    /* > 0 */
    float minZoomOut = 0.01f;

    /**
     * increase >1 to allow zoom out beyond input size (ex: thumbnail size)
     */
    float maxZoomOut = 1f;

    public boolean vflip = false;
    public List<ActionConcept> actions;
    private float fr = 1f;
    private float fg = 1f;
    private float fb = 1f;
    float minClarity = 0.15f, maxClarity = 1f;
    private boolean inBounds = false;


    public static PixelBag of(Supplier<BufferedImage> bb, int px, int py) {
        return new PixelBag(px, py) {

            public BufferedImage b;

            @Override
            public int sw() {
                return b.getWidth();
            }

            @Override
            public int sh() {
                return b.getHeight();
            }

            @Override
            public void update(float frameRate) {
                b = bb.get();
                if (b != null)
                    super.update(frameRate);

            }

            @Override
            public int rgb(int sx, int sy) {
                return b.getRGB(sx, sy);
            }
        };
    }

    public PixelBag(int px, int py) {
        this.px = px;
        this.py = py;
        this.pixels = new float[px][py];

        this.Z = 1f;
    }

    /**
     * source width, in pixels
     */
    abstract public int sw();

    /**
     * source height, in pixels
     */
    abstract public int sh();

    @Override
    public void update(float frameRate) {

        int sw = sw();
        int sh = sh();

        float ew, eh;
        ew = max(Z * sw * maxZoomOut, sw * minZoomOut);
        eh = max(Z * sh * maxZoomOut, sh * minZoomOut);


        float minX, maxX, minY, maxY;
        if (inBounds) {
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
            minX = (X * mw);
            maxX = minX + ew;
            minY = (Y * mh);
            maxY = minY + eh;
        } else {
            minX = (X * sw) - ew/2f;
            maxX = (X * sw) + ew/2f;
            minY = (Y * sh) - eh/2f;
            maxY = (Y * sh) + eh/2f;
        }



//        System.out.println(X + "," + Y + "," + Z + ": [" + (minX+maxX)/2f + "@" + minX + "," + maxX + "::"
//                                                         + (minY+maxY)/2f + "@" + minY + "," + maxY + "] <- aspect=" + eh + "/" + ew);

        float cx = px / 2f;
        float cy = py / 2f;


        //not perfect calculation, because it doesnt account for max/min min/max differences due to non-square dimensions
        //but suffices for now
        float maxCenterDistanceSq = Math.max(cx, cy) * Math.max(cx, cy) * 2;

        float pxf = px - 1;
        float pyf = py - 1;

        float fr = this.fr, fg = this.fg, fb = this.fb;
        float fSum = fr + fg + fb;

        float xRange = maxX-minX;
        float yRange = maxY-minY;

        int supersampling = Math.min((int) Math.floor(xRange / px / 2f), (int) Math.floor(yRange / py / 2f));


        for (int ly = 0; ly < py; ly++) {
            float l = ly / pyf;
            int sy = Math.round(lerp(!vflip ? l : 1f - l, maxY, minY));

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
                float distFromCenterSq = dx * dx + yDistFromCenterSq; //manhattan distance from center

                float clarity = lerp(distFromCenterSq / maxCenterDistanceSq, minClarity, maxClarity);
                if (rng.nextFloat() > clarity)
                    continue;

                //project to the viewed image plane
                int sx = Math.round(lerp(lx / pxf, maxX, minX));

                int samples = 0;
                float R = 0, G = 0, B = 0;
                for (int esx = Math.max(0, sx - supersampling); esx <= Math.min(sw - 1, sx + supersampling); esx++) {

                    if (esx < 0 || esx >= sw )
                        continue;

                    for (int esy = Math.max(0, sy - supersampling); esy <= Math.min(sh - 1, sy + supersampling); esy++) {
                        if (esy < 0 || esy >= sh )
                            continue;

                        int RGB = rgb(esx, esy);
                        R += Bitmap2D.decodeRed(RGB);
                        G += Bitmap2D.decodeGreen(RGB);
                        B += Bitmap2D.decodeBlue(RGB);
                        samples++;
                    }
                }
                float v = (samples == 0) ? 0.5f : (fr * R + fg * G + fb * B) / fSum / samples;
                pixels[lx][ly] = v;
            }
        }
    }

    public void setClarity(float minClarity, float maxClarity) {
        this.minClarity = minClarity;
        this.maxClarity = maxClarity;
    }

    abstract public int rgb(int sx, int sy);

    @Override
    public void see(EachPixelRGB p) {
        throw new UnsupportedOperationException("yet");

    }

    public void setMinZoomOut(float minZoomOut) {
        this.minZoomOut = minZoomOut;
    }

    public void setMaxZoomOut(float maxZoomOut) {
        this.maxZoomOut = maxZoomOut;
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
        return f / 2f + 0.5f;
    }

    public boolean setZoom(float f) {
        Z = u(f);
        return true;
    }

    public boolean setYRelative(float f) {
        Y = u(f);
        return true;
    }

    public boolean setXRelative(float f) {
        X = u(f);
        return true;
    }

    public void setFilter(float r, float g, float b) {
        this.fr = r;
        this.fg = g;
        this.fb = b;
    }

    public boolean setRedFilter(float f) {
        this.fr = f;
        return true;
    }

    public boolean setGreenFilter(float f) {
        this.fr = f;
        return true;
    }

    public boolean setBlueFilter(float f) {
        this.fr = f;
        return true;
    }


    public PixelBag addActions(String termRootStr, NAgent a) {
        actions = $.newArrayList(3);

        Term termRoot = $.the(termRootStr);
        actions.add(a.actionBipolar($.func("see", termRoot, $.the("rx")), this::setXRelative));
        actions.add(a.actionBipolar($.func("see", termRoot, $.the("ry")), this::setYRelative));
        actions.add(a.actionBipolar($.func("see", termRoot, $.the("zoom")), this::setZoom));
//        actions.add( a.actionBipolar("see(" + termRoot + ",fr)", this::setRedFilter) );
//        actions.add( a.actionBipolar("see(" + termRoot + ",fg)", this::setGreenFilter) );
//        actions.add( a.actionBipolar("see(" + termRoot + ",fb)", this::setBlueFilter) );
        return this;
    }

}

