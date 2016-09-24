package nars.video;

import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;
import nars.util.data.list.FasterList;
import nars.util.math.FloatSupplier;
import nars.concept.SensorConcept;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;

import java.util.List;

/**
 * manages reading a camera to a pixel grid of SensorConcepts
 * monochrome
 */
public class CameraSensor<P extends PixelCamera> extends MatrixSensor<P> {


    private final int radix = 3;
    private final NAR nar;
    float maxResolution = 0.02f, minResolution = 0.12f; //TODO less precision for peripheral pixels than center?

    public CameraSensor(Term root, P src, NAgent agent, FloatToObjectFunction<Truth> brightnessToTruth) {
        super(src, src.width(), src.height());

        this.nar = agent.nar;

        agent.sensors.addAll(
            encode((x,y)->

                $.inh( $.p(
                    (radix > 1 ?
                        $.p( $.pRecurse($.radixArray(x, radix, width)), $.pRecurse($.radixArray(y, radix, height))) :
                        $.p(x, y)
                    )), root)

            , brightnessToTruth));

    }

    public List<SensorConcept> encode(Int2Function<Compound> cellTerm, FloatToObjectFunction<Truth> brightnessToTruth) {
        FasterList<SensorConcept> l = $.newArrayList();
        for (int x = 0; x < width; x++) {
            int xx = x;
            for (int y = 0; y < height; y++) {
                //TODO support multiple coordinate termizations
                Compound cell = cellTerm.get(x, y);

                int yy = y;
                SensorConcept sss;

                //monochrome only for now
                FloatSupplier brightness = () -> src.brightness(xx, yy);

//                float dx = Math.abs(x - width/2f);
//                float dy = Math.abs(y - height/2f);
//                float cdist = (float) (Math.sqrt( dx*dx + dy*dy )-1) / (Math.max(width,height)/2f);

                l.add(sss = new SensorConcept(cell, nar,
                        brightness,
                        brightnessToTruth
                ).resolution(
                        //distToResolution(cdist)
                        maxResolution
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

    public void update() {
        src.update();
    }

    interface Int2Function<T> {
        T get(int x, int y);
    }
}
