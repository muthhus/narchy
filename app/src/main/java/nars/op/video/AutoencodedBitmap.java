package nars.op.video;

import jcog.learn.Autoencoder;
import jcog.math.random.XorShift128PlusRandom;
import nars.util.signal.Bitmap2D;

/**
 * Autoencoder dimensional reduction of bitmap input
 */
public class AutoencodedBitmap implements Bitmap2D {

    final Bitmap2D source;
    private final float[] output;

    public final float[] input;

    final Autoencoder ae;
    private final int ox, oy, sx, sy;
    private final int w;
    private final int h;

    public AutoencodedBitmap(Bitmap2D b, int sx, int sy, int ox, int oy) {
        this.source = b;

        int w = b.width();
        int h = b.height();
        this.ox = ox;
        this.sx = sx;
        this.oy = oy;
        this.sy = sy;


        int i = sx * sy;
        this.input = new float[i];
        this.ae = new Autoencoder(i, ox * oy, new XorShift128PlusRandom(1));

        this.w = w / sx * ox;
        this.h = h / sy * oy;
        this.output = new float[this.w * this.h];
    }

    @Override
    public void update(float frameRate) {
        source.update(frameRate);

        //image = ConvertBufferedImage.convertFrom(source.get(), image);
        int w = source.width();
        int h = source.height();

        int k = 0;
        for (int Y = 0; Y < h; Y+=sy) {
            for (int X = 0; X < w; X+=sx) {

                int j = 0;
                for (int y = 0; y < sy; y++) {
                    for (int x = 0; x < sx; x++) {
                        float b = source.brightness(X + x, Y + y);
                        if (b!=b)
                            b = 0.5f;
                        input[j++] = b;
                    }
                }
                assert(j==input.length);

                ae.put(input, 0.05f, 0.002f, 0, true);

                float[] o = ae.output();
                for (float anO : o) {
                    output[k++] = anO;
                }
            }
        }
        assert(k == output.length);

    }

    @Override
    public int width() {
        return w;
    }

    @Override
    public int height() {
        return h;
    }

    @Override
    public float brightness(int xx, int yy) {
        return output[yy * w + xx];
    }



}
