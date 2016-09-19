package nars.experiment.asteroids;

import nars.$;
import nars.NAR;
import nars.remote.SwingAgent;
import nars.video.CameraSensor;
import nars.video.Scale;
import nars.video.SwingCamera;

import static java.lang.Math.*;
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
        int W = space.getWidth();
        int H = space.getHeight();

        SwingCamera swingCam = new SwingCamera(space);
        swingCam.input(W/4, H/4, W/2, H/2); //50%

        Scale cam = new Scale(swingCam, 48, 48);
        pixels = addCamera("ast",
                //space, 32, 32,
                cam,
                (v) -> $.t(v , alpha));
                    //t( Util.clamp(v * 1f /* contrast */)


        int camXYSpeed = 30;
        float camZoomRate = 0.1f;
        int minZoomX = 64;
        int minZoomY = 64;
        addIncrementalRangeAction("ast:(cam,L)", (f)->
            swingCam.inputTranslate(round(-camXYSpeed * f), 0 ) );
        addIncrementalRangeAction("ast:(cam,R)", (f)->
            swingCam.inputTranslate(round(+camXYSpeed * f), 0 ) );
        addIncrementalRangeAction("ast:(cam,U)", (f)->
            swingCam.inputTranslate(0, round(-camXYSpeed * f)) );
        addIncrementalRangeAction("ast:(cam,D)", (f)->
            swingCam.inputTranslate(0, round(+camXYSpeed * f)) );
        addIncrementalRangeAction("ast:(cam,I)", (f)->
            swingCam.inputZoom( (1 - camZoomRate * f), minZoomX, minZoomY) );
        addIncrementalRangeAction("ast:(cam,O)", (f)->
            swingCam.inputZoom( (1 + camZoomRate * f), minZoomX, minZoomY) );

        addToggleAction("ast:fire", (b) -> space.spaceKey = b);
        addToggleAction("ast:forward", (b) -> space.upKey = b);
        addToggleAction("ast:left", (b) -> space.leftKey = b);
        addToggleAction("ast:right", (b) -> space.rightKey = b);

    }



    float prevScore = 0;
    @Override protected float reward() {
        float nextScore = space.frame();
        float ds = nextScore - prevScore;
        this.prevScore = nextScore;
        return ds;
    }

}
