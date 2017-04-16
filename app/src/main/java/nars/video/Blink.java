package nars.video;

import jcog.data.FloatParam;
import jcog.random.XorShift128PlusRandom;

import java.util.Random;

/**
 * stochastic blinking filter
 */
public class Blink implements Bitmap2D {

    private final Bitmap2D in;

    /**
     * percentage of duty cycle during which input is visible
     */
    private FloatParam visibleProb = new FloatParam(0, 0, 1f);

    final Random rng = new XorShift128PlusRandom(1);
    boolean blinked = false;

    public Blink(Bitmap2D in, float rate) {
        this.in = in;
        this.visibleProb.setValue(rate);
    }

    @Override
    public void update(float frameRate) {
        blinked = rng.nextFloat() < visibleProb.floatValue();
        if (!blinked) {
            in.update(frameRate);
        }
    }

    @Override
    public int width() {
        return in.width();
    }

    @Override
    public int height() {
        return in.height();
    }

    @Override
    public float brightness(int xx, int yy) {
        return blinked ? in.brightness(xx, yy) : 0f;
    }
}
