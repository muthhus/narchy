package nars.experiment.asteroids;

import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.remote.SwingAgent;
import nars.video.CameraSensor;
import nars.video.SwingCamera;

import static nars.$.t;
import static nars.experiment.arkanoid.Arkancide.playSwing;

/**
 * Created by me on 9/19/16.
 */
public class NARsteroids extends SwingAgent {

    private final Asteroids space;
    private final CameraSensor pixels;

    public static void main(String[] args) {
        playSwing(NARsteroids::new);
    }

    public NARsteroids(NAR nar) {
        super(nar, 4);

        this.space = new Asteroids(false);

        SwingCamera cam = new SwingCamera(space, 64, 64);
        float brightness = 2f;
        pixels = new CameraSensor($.oper("ast"), cam, this, (v) -> t(Math.min(1f, v*brightness), alpha));
        cam.input(0f, 0f, 0.3f, 0.3f);
    }

//    @Override
//    protected float act() {
//
//        float leftright = (float) Math.random() - 0.5f;
//        float up = (float) Math.random();
//
//
//        //space.act(dt, leftright, up);
//        space.frame();
//        pixels.update();
//
//        return 0;
//    }

    @Override
    protected float reward() {
        space.frame();
        return 0;
    }
}
