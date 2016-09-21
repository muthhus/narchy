package nars.video;

import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.op.math.IntIntTo;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;
import nars.util.data.list.FasterList;
import nars.util.math.FloatSupplier;
import nars.concept.SensorConcept;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.IntToIntFunction;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * manages reading a camera to a pixel grid of SensorConcepts
 * monochrome
 */
public class CameraSensor<P extends PixelCamera> extends MatrixSensor<P, SensorConcept> {


    private final int radix = 3;
    private final NAR nar;
    float freqResolution = 0.04f; //TODO less precision for peripheral pixels than center?

    public CameraSensor(Term root, P src, NAgent agent, FloatToObjectFunction<Truth> brightnessToTruth) {
        super(src, src.width(), src.height());

        this.nar = agent.nar;

        agent.sensors.addAll(
            encode((x,y)->

                $.p((radix > 1 ?
                        $.p($.pRadix(x, 4, width), $.pRadix(y, 4, height)) :
                        $.p(x, y)
                    ), root)

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

                l.add(sss = new SensorConcept(cell, nar,
                        brightness,
                        brightnessToTruth
                ).resolution(freqResolution));
                matrix[x][y] = sss;
            }
        }
        return l;
    }

    public void update() {
        src.update();
    }

    interface Int2Function<T> {
        T get(int x, int y);
    }
}
