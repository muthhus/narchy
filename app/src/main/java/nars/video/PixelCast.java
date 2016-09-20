package nars.video;

import nars.util.data.random.XorShift128PlusRandom;

import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * 2D flat Raytracing Retina
 */
public class PixelCast implements PixelCamera {

    final BufferedImage source;
    private final int px;
    private final int py;
    float sampleRate = 0.5f;
    final Random rng = new XorShift128PlusRandom(1);

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

        int sw = b.getWidth();
        int sh = b.getHeight();
        float samples = px*py*sampleRate;
        for (int i = 0; i < samples; i++) {
            //choose a virtual retina pixel
            float x = rng.nextFloat();
            float y = rng.nextFloat();

            //project from the local retina plane
            int lx = Math.round((px-1) * x);
            int ly = Math.round((py-1) * y);

            //project to the viewed image plane
            int sx = Math.round((sw-1) * x);
            int sy = Math.round((sh-1) * y);

            float v = PixelCamera.decodeRed(b.getRGB(sx, sy));
            w[lx][ly] = v;
            //p.pixel(lx, ly, );
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
