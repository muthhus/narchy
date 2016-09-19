package nars.video;

import nars.$;
import nars.agent.NAgent;
import nars.term.Compound;
import nars.term.atom.Atom;
import nars.truth.Truth;
import nars.util.math.FloatSupplier;
import nars.util.signal.SensorConcept;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.jetbrains.annotations.NotNull;

/**
 * manages reading a camera to a pixel grid of SensorConcepts
 */
public class CameraSensor {

    public final SensorConcept[][] ss;
    public final int width, height;
    private final PixelCamera cam;

    public CameraSensor(PixelCamera cam, NAgent agent, FloatToObjectFunction<Truth> brightnessToTruth) {
        this.cam = cam;
        width = cam.width();
        height = cam.height();

        //TODO extract this section and associated variables to a CameraSensorMatrix class or something
        ss = new SensorConcept[width][height];

        Atom xy = $.the("xy");
        for (int x = 0; x < width; x++) {
            int xx = x;
            for (int y = 0; y < height; y++) {
                //TODO support multiple coordinate termizations
                @NotNull Compound coord =
                        $.p($.pRadix(x, 4, width), $.pRadix(y, 4, height));
                //$.p(x, y);

                Compound cell = $.inh(coord, xy);
                int yy = y;
                SensorConcept sss;

                //monochrome only for now
                FloatSupplier brightness = () -> cam.brightness(xx, yy);


                agent.sensors.add(sss = new SensorConcept(cell, agent.nar,
                        brightness,
                        brightnessToTruth
                ));
                //sss.sensor.dur = 0.1f;
                //sss.timing(0,visionSyncPeriod);
                ss[x][y] = sss;
            }
        }

    }

    public void update() {
        cam.update();
    }
}
