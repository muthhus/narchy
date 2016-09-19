package nars.remote;

import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.video.CameraSensor;
import nars.video.SwingCamera;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

import static nars.$.t;

/**
 * Created by me on 9/19/16.
 */
abstract public class SwingAgent extends NAgent {

    public final Map<String,CameraSensor> widgets = new LinkedHashMap<>();

    public SwingAgent(NAR nar, int frames) {
        super(nar, frames);

    }

    protected CameraSensor<SwingCamera> addView(String id, Container w, int px, int pw) {
        CameraSensor c = new CameraSensor<SwingCamera>($.the(id), new SwingCamera(w, px, pw), this, (v) -> t(v, alpha));
        widgets.put(id, c);
        return c;
    }


    @Override
    protected float act() {

        widgets.values().forEach(CameraSensor::update);

        //TODO update input

        return reward();
    }

    protected abstract float reward();
}
