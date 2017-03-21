package nars.video;

import com.google.common.base.Joiner;
import jcog.Util;
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

import static nars.Op.BELIEF;

/**
 * manages reading a camera to a pixel grid of SensorConcepts
 * monochrome
 */
public class CameraSensor<P extends Bitmap2D> extends Sensor2D<P> implements Consumer<NAR>, Iterable<CameraSensor<P>.PixelConcept> {

    private final NAR nar;

    private static final int radix = 2;
    private final List<PixelConcept> pixels;

    float resolution = 0.01f;//Param.TRUTH_EPSILON;

    final int numPixels;

    /** total priority to be shared by any changed pixels */
    public final FloatParam totalPriority = new FloatParam(1f, 0f, 16f);

    private float priEach = 0;


    public CameraSensor(Term root, P src, NAR nar) {
        this(root, src, nar, (v) -> $.t(v, nar.confidenceDefault(BELIEF)));
    }

    public CameraSensor(Term root, P src, NAR nar, FloatToObjectFunction<Truth> brightnessToTruth) {
        super(src, src.width(), src.height());

        this.nar = nar;

        numPixels = src.width() * src.height();
        //sqrtNumPixels = (float)Math.sqrt(numPixels);

        pixels = encode((x, y) ->
                        $.inh(
                                //$.inh(
                                root,

                                //$.secte
                                    radix > 1 ?
                                        $.p( zipCoords(coord(x, width), coord(y, height)) ) :
                                        //$.p(new Term[]{coord('x', x, width), coord('y', y, height)}) :
                                        //new Term[]{coord('x', x, width), coord('y', y, height)} :
                                        $.p( $.the(x), $.the(y) )

                        )
                , brightnessToTruth);

        //System.out.println(Joiner.on('\n').join(pixels));


        nar.onCycle(this);
    }

    @NotNull
    @Override
    public Iterator<PixelConcept> iterator() {
        return pixels.iterator();
    }

    public void priTotal(float totalPri) {
        totalPriority.setValue(totalPri);
    }

    private static Term[] zipCoords(@NotNull Term[] x, @NotNull Term[] y) {
        int m = Math.max(x.length, y.length);
        Term[] r = new Term[m];
        int sx = m - x.length;
        int sy = m - y.length;
        int ix = 0, iy = 0;
        for (int i = 0; i < m; i++) {
            Term xy;
            char levelPrefix =
                (char)('a' + (m-1 - i)); //each level given a different scale prefix
                //'p';

            if (i >= sx && i >=sy) {
                xy = $.the(levelPrefix + x[ix++].toString() + y[iy++].toString());
            } else if (i >= sx) {
                xy = $.the(levelPrefix + x[ix++].toString() + "_");
            } else { //if (i < y.length) {
                xy = $.the(levelPrefix + "_" + y[iy++].toString());
            }
            r[i] = xy;
        }
        return r;
    }

    @NotNull
    public static Compound coord(char prefix, int n, int max) {
        //return $.pRecurseIntersect(prefix, $.radixArray(n, radix, max));
        //return $.pRecurse($.radixArray(n, radix, max));
        return $.p($.radixArray(n, radix, max));
    }

    @NotNull
    public static Term[] coord(int n, int max) {
        //return $.pRecurseIntersect(prefix, $.radixArray(n, radix, max));
        //return $.pRecurse($.radixArray(n, radix, max));
        return $.radixArray(n, radix, max);
    }

    @Override
    public void accept(NAR n) {

        src.update(1);

        int changed = 0;
        for (int i = 0, pixelsSize = pixels.size(); i < pixelsSize; i++) {
            if (pixels.get(i).update())
                changed++;
        }

        float totalPri = totalPriority.floatValue();
        if (changed > 0) {
            priEach = totalPri / changed;
        } else {
            priEach = totalPri / numPixels; //if none changed, use the value as if all changed
        }

    }

    public List<PixelConcept> encode(Int2Function<Compound> cellTerm, FloatToObjectFunction<Truth> brightnessToTruth) {
        List<PixelConcept> l = $.newArrayList();
        for (int x = 0; x < width; x++) {

            for (int y = 0; y < height; y++) {
                //TODO support multiple coordinate termizations
                Compound cell = cellTerm.get(x, y);


                PixelConcept sss = new PixelConcept(cell, brightnessToTruth, x, y);
                sss.pri(()-> priEach);
                sss.resolution(
                    //distToResolution(cdist)
                    resolution
                );

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



    public CameraSensor setResolution(float resolution) {
        this.resolution = resolution;
        pixels.forEach(p -> p.resolution(resolution));
        return this;
    }

    public PixelConcept concept(int x, int y) {
        return pixels.get(y * width + x);
    }


    interface Int2Function<T> {
        T get(int x, int y);
    }

    public class PixelConcept extends SensorConcept {

        private final int x, y;
        private float bufferedValue;

        public PixelConcept(Compound cell, FloatToObjectFunction<Truth> brightnessToTruth, int x, int y) {
            super(cell, nar, null, brightnessToTruth);
            this.x = x;
            this.y = y;
            this.bufferedValue = Float.NaN;
            setSignal(()->bufferedValue);
        }

        boolean update() {
            bufferedValue = src.brightness(x, y);
            return currentValue!=currentValue || !Util.equals(currentValue, bufferedValue, resolution);
        }


    }

}
