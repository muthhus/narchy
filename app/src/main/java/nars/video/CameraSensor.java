package nars.video;

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

import java.util.List;
import java.util.function.Consumer;

/**
 * manages reading a camera to a pixel grid of SensorConcepts
 * monochrome
 */
public class CameraSensor<P extends Bitmap2D> extends Sensor2D<P> implements Consumer<NAR> {


    private final NAR nar;
    private final NAgent agent;

    private static final int radix = 4;
    float resolution = 0.01f;//Param.TRUTH_EPSILON;

    public CameraSensor(Atomic root, P src, NAgent agent, FloatToObjectFunction<Truth> brightnessToTruth) {
        super(src, src.width(), src.height());

        this.nar = agent.nar;
        this.agent = agent;

        agent.sensors.addAll(
                encode((x,y)->

                                $.func(root,
                                        //$.inh(
                                        //root,
                                        $.p(radix > 1 ?
                                                new Term[] { coord(x, width), coord(y, height) } :
                                                new Term[] { $.the(x), $.the(y) }
                                        ))

                        , brightnessToTruth));

        agent.nar.onCycle(this);
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
                SensorConcept sss;

                l.add(sss = new SensorConcept(cell, nar,
                        brightness,
                        brightnessToTruth
                ).resolution(
                        //distToResolution(cdist)
                        resolution
                ));
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
