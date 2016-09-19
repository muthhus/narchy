package nars.experiment.asteroids;

import nars.$;
import nars.NAR;
import nars.concept.ActionConcept;
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
    private final CameraSensor<SwingCamera> pixels;

    public static void main(String[] args) {
        playSwing(NARsteroids::new);
    }

    public NARsteroids(NAR nar) {
        super(nar, 4);

        this.space = new Asteroids(false);

        pixels = addCamera("ast", space, 64, 64);
        pixels.cam.input(0f, 0f, 0.3f, 0.3f);

        addToggleAction("ast:fire", () -> space.spaceKey = true, () -> space.spaceKey= false);
        addToggleAction("ast:forward", () -> space.upKey = true, () -> space.upKey= false);
        addToggleAction("ast:left", () -> space.leftKey = true, () -> space.leftKey= false);
        addToggleAction("ast:right", () -> space.rightKey = true, () -> space.rightKey= false);

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
