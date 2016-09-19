package nars.remote;

import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.truth.Truth;
import nars.video.ImageCamera;
import nars.video.CameraSensor;
import nars.video.Scale;
import nars.video.SwingCamera;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

import static nars.$.t;

/**
 * Created by me on 9/19/16.
 */
abstract public class SwingAgent extends NAgent {

    public final Map<String,CameraSensor> cam = new LinkedHashMap<>();

    public SwingAgent(NAR nar, int frames) {
        super(nar, frames);

    }

    /** pixelTruth defaults to linear monochrome brightness -> frequency */
    protected CameraSensor<Scale> addCamera(String id, Container w, int pw, int ph) {
        return addCamera(id, w, pw, ph, (v) -> t(v, alpha));
    }

    protected CameraSensor<Scale> addCamera(String id, Container w, int pw, int ph, FloatToObjectFunction<Truth> pixelTruth) {
        return addCamera(id, new Scale(new SwingCamera(w), pw, ph), pixelTruth);
    }

    protected <C extends ImageCamera> CameraSensor<C> addCamera(String id, C bc, FloatToObjectFunction<Truth> pixelTruth) {
        CameraSensor c = new CameraSensor<>($.the(id), bc, this, pixelTruth);
        cam.put(id, c);
        return c;
    }


    @Override
    protected float act() {

        cam.values().forEach(CameraSensor::update);

        //TODO update input

        return reward();
    }

    protected abstract float reward();
}
