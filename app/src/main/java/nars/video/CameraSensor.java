package nars.video;

import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.concept.SensorConcept;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.truth.Truth;
import nars.util.data.Mix;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static nars.Op.BELIEF;

/**
 * manages reading a camera to a pixel grid of SensorConcepts
 * monochrome
 */
public class CameraSensor<P extends Bitmap2D> extends Sensor2D<P> implements Consumer<NAgent>, Iterable<CameraSensor<P>.PixelConcept> {

    private final NAR nar;

    private static final int radix = 4;
    public final List<PixelConcept> pixels;
    private final Mix.MixStream in;

    float resolution = 0.01f;//Param.TRUTH_EPSILON;

    final int numPixels;

    public CameraSensor(Term root, P src, NAgent agent) {
        this(root, src, agent, (v) -> $.t(v, agent.nar.confDefault(BELIEF)));
    }

    public CameraSensor(Term root, P src, NAgent agent, FloatToObjectFunction<Truth> brightnessToTruth) {
        super(src, src.width(), src.height());

        this.nar = agent.nar;

        numPixels = src.width() * src.height();
        //sqrtNumPixels = (float)Math.sqrt(numPixels);

        this.in = nar.mix.stream(this);


        pixels = encode((x, y) ->
                        $.inh(
                                //$.inh(


                                //$.secte
                                    radix > 1 ?
                                        //$.pRecurse( zipCoords(coord(x, width), coord(y, height)) ) :
                                        $.p( zipCoords(coord(x, width), coord(y, height)) ) :
                                        //$.p(new Term[]{coord('x', x, width), coord('y', y, height)}) :
                                        //new Term[]{coord('x', x, width), coord('y', y, height)} :
                                        $.p( $.the(x), $.the(y) ),

                                root


                        )
                , brightnessToTruth);

        //System.out.println(Joiner.on('\n').join(pixels));

        agent.onFrame(this);

    }

    @NotNull
    @Override
    public Iterator<PixelConcept> iterator() {
        return pixels.iterator();
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
                xy = Atomic.the(levelPrefix + x[ix++].toString() + y[iy++].toString());
            } else if (i >= sx) {
                xy = Atomic.the(levelPrefix + x[ix++].toString() + "_");
            } else { //if (i < y.length) {
                xy = Atomic.the(levelPrefix + "_" + y[iy++].toString());
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



    public List<PixelConcept> encode(Int2Function<Compound> cellTerm, FloatToObjectFunction<Truth> brightnessToTruth) {
        List<PixelConcept> l = $.newArrayList();
        for (int x = 0; x < width; x++) {

            for (int y = 0; y < height; y++) {
                //TODO support multiple coordinate termizations
                Compound cell = cellTerm.get(x, y);


                PixelConcept sss = new PixelConcept(cell, brightnessToTruth, x, y);


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
        pixels.forEach(p -> {
        });
        return this;
    }

    public PixelConcept concept(int x, int y) {
        return pixels.get(y * width + x);
    }

    @Override
    public void accept(NAgent a) {

        //frameStamp();

        src.update(1);

        NAR nar = a.nar;

        in.input(pixels.stream()/*filter(PixelConcept::update).*/
                .map(c -> c.apply(nar)),
                y -> nar.input((Stream)y));
    }

    public void pri(float v) {
        in.setValue(v);
    }


    interface Int2Function<T> {
        T get(int x, int y);
    }

//    private long nextStamp;
//    private void frameStamp() {
//        nextStamp = nar.time.nextStamp();
//    }

    public class PixelConcept extends SensorConcept {

        //private final int x, y;

        public PixelConcept(Compound cell, FloatToObjectFunction<Truth> brightnessToTruth, int x, int y) {
            super(cell, nar, null, brightnessToTruth);
            //this.x = x;
            //this.y = y;
            setSignal(()->src.brightness(x, y));
        }

//        @Override
//        protected LongSupplier update(Truth currentBelief, @NotNull NAR nar) {
//            return ()->nextStamp;
//        }

    }

}
