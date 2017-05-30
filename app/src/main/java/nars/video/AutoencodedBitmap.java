package nars.video;

import jcog.learn.Autoencoder;
import jcog.random.XorShift128PlusRandom;

/**
 * Autoencoder dimensional reduction of bitmap input
 */
public class AutoencodedBitmap implements Bitmap2D {

    final Bitmap2D source;
    private final float[] output;

    public final float[] input;

    final Autoencoder ae;

    public AutoencodedBitmap(Bitmap2D b, int o) {
        this.source = b;
        int i = b.width() * b.height();
        this.input = new float[i];
        this.ae = new Autoencoder(i, o, new XorShift128PlusRandom(1));
        this.output = ae.output();
    }

    @Override
    public void update(float frameRate) {
        //image = ConvertBufferedImage.convertFrom(source.get(), image);
        int w = source.width();
        int h = source.height();
        int j = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                input[j++] = source.brightness(x, y);
            }
        }
        ae.train(input, 0.1f, 0.02f, 0, true);
    }

    @Override
    public int width() {
        return output.length;
    }

    @Override
    public int height() {
        return 1;
    }

    @Override
    public float brightness(int xx, int yy) {
        return output[xx];
    }



}
