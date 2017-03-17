package nars.video;

import jcog.data.FloatParam;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.concept.SensorConcept;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * manages reading a camera to a pixel grid of SensorConcepts
 * monochrome
 */
public class CameraSensor<P extends Bitmap2D> extends Sensor2D<P> implements Consumer<NAR>, Iterable<SensorConcept> {

    private final NAR nar;
    private final NAgent agent;

    private static final int radix = 4;
    private final List<SensorConcept> pixels;
    private final float sqrtNumPixels;
    float resolution = 0.01f;//Param.TRUTH_EPSILON;

    final int numPixels;

    /** total priority to be shared (in proportion to sqrt # pixels, in order to represent an area -> priority relationship ) */
    public final FloatParam totalPriority = new FloatParam(1f, 0f, 64f);

    private FloatSupplier pixelPriority;

    public CameraSensor(Atomic root, P src, NAgent agent, FloatToObjectFunction<Truth> brightnessToTruth) {
        super(src, src.width(), src.height());

        this.nar = agent.nar;
        this.agent = agent;

        numPixels = src.width() * src.height();
        sqrtNumPixels = (float)Math.sqrt(numPixels);
        pixelPriority = () -> totalPriority.floatValue() / sqrtNumPixels;

        pixels = encode((x, y) ->
                        $.func(root,
                                //$.inh(
                                //root,
                                $.p(radix > 1 ?
                                        new Term[]{coord(x, width), coord(y, height)} :
                                        new Term[]{$.the(x), $.the(y)}
                                ))
                , brightnessToTruth);


        agent.nar.onCycle(this);
    }

    @NotNull
    @Override
    public Iterator<SensorConcept> iterator() {
        return pixels.iterator();
    }

    public void priTotal(float totalPri) {
        totalPriority.setValue(totalPri);
    }

    @NotNull
    public static Compound coord(int n, int max) {
        return $.pRecurse($.radixArray(n, radix, max));
        //return $.p($.radixArray(n, radix, max));
    }

    public List<SensorConcept> encode(Int2Function<Compound> cellTerm, FloatToObjectFunction<Truth> brightnessToTruth) {
        List<SensorConcept> l = $.newArrayList();
        for (int x = 0; x < width; x++) {
            int xx = x;
            for (int y = 0; y < height; y++) {
                //TODO support multiple coordinate termizations
                Compound cell = cellTerm.get(x, y);

                int yy = y;

                //monochrome only for now
                FloatSupplier brightness = () -> src.brightness(xx, yy);

//                float dx = Math.abs(x - width/2f);
//                float dy = Math.abs(y - height/2f);
//                float cdist = (float) (Math.sqrt( dx*dx + dy*dy )-1) / (Math.max(width,height)/2f);
                SensorConcept sss = new SensorConcept(cell, nar,
                    brightness,
                    brightnessToTruth
                );
                sss.resolution(
                    //distToResolution(cdist)
                    resolution
                );
                sss.pri(pixelPriority);

                l.add(sss);

                matrix[x][y] = sss;
            }
        }
        return l;
    }

//    private float distToResolution(float dist) {
//
//        float r = Util.lerp(minResolution, maxResolution, dist);
//
//        return r;
//    }

    @Override
    public void accept(NAR n) {

        src.update(1);



    }

    public CameraSensor setResolution(float resolution) {
        this.resolution = resolution;
        return this;
    }


    interface Int2Function<T> {
        T get(int x, int y);
    }
}
