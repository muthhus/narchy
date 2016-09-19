package nars.experiment.asteroids;

import nars.$;
import nars.NAR;
import nars.remote.SwingAgent;
import nars.util.Util;
import nars.video.CameraSensor;
import nars.video.PanZoom;
import nars.video.SwingCamera;

import static nars.$.t;
import static nars.experiment.arkanoid.Arkancide.playSwing;

/**
 * Created by me on 9/19/16.
 */
public class NARsteroids extends SwingAgent {

    private final Asteroids space;
    private final CameraSensor/*<SwingCamera>*/ pixels;

    public static void main(String[] args) {
        playSwing(NARsteroids::new);
    }

    public NARsteroids(NAR nar) {
        super(nar, 0);

        this.space = new Asteroids(false);

        pixels = addCamera("ast",
                //space, 32, 32,
                new PanZoom(()->space.offscreen, 64, 64),
                (v) -> $.t(v , alpha));
                    //t( Util.clamp(v * 1f /* contrast */)

        //pixels.cam.input(0f, 0f, 0.3f, 0.3f);

        addToggleAction("ast:fire", (b) -> space.spaceKey = b);
        addToggleAction("ast:forward", (b) -> space.upKey = b);
        addToggleAction("ast:left", (b) -> space.leftKey = b);
        addToggleAction("ast:right", (b) -> space.rightKey = b);

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

    float prevScore = 0;
    @Override protected float reward() {
        float nextScore = space.frame();
        float ds = nextScore - prevScore;
        this.prevScore = nextScore;
        return ds;
    }

}
