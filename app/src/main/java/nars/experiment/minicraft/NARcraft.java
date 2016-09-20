package nars.experiment.minicraft;

import nars.$;
import nars.NAR;
import nars.experiment.asteroids.Asteroids;
import nars.experiment.minicraft.top.InputHandler;
import nars.experiment.minicraft.top.TopDownMinicraft;
import nars.remote.SwingAgent;
import nars.video.CameraSensor;
import nars.video.PixelCast;
import nars.video.Scale;
import nars.video.SwingCamera;

import static java.lang.Math.round;
import static nars.experiment.arkanoid.Arkancide.playSwing;

/**
 * Created by me on 9/19/16.
 */
public class NARcraft extends SwingAgent {

    private final TopDownMinicraft craft;
    private final CameraSensor<?> pixels;

    public static void main(String[] args) {
        playSwing(NARcraft::new);
    }

    public NARcraft(NAR nar) {
        super(nar, 0);

        this.craft = new TopDownMinicraft();
        int W = craft.getWidth();
        int H = craft.getHeight();

        PixelCast swingCam = new PixelCast(craft.image, 64, 64);
        //swingCam.input(W/4, H/4, W/2, H/2); //50%

//        Scale cam = new Scale(swingCam, 48, 48);
        pixels = addCamera("cra",
                swingCam,
                (v) -> $.t(v , alpha));


//        int camXYSpeed = 30;
//        float camZoomRate = 0.1f;
//        int minZoomX = 64;
//        int minZoomY = 64;
//        addIncrementalRangeAction("cra:(cam,L)", (f)->
//            swingCam.inputTranslate(round(-camXYSpeed * f), 0 ) );
//        addIncrementalRangeAction("cra:(cam,R)", (f)->
//            swingCam.inputTranslate(round(+camXYSpeed * f), 0 ) );
//        addIncrementalRangeAction("cra:(cam,U)", (f)->
//            swingCam.inputTranslate(0, round(-camXYSpeed * f)) );
//        addIncrementalRangeAction("cra:(cam,D)", (f)->
//            swingCam.inputTranslate(0, round(+camXYSpeed * f)) );
//        addIncrementalRangeAction("cra:(cam,I)", (f)->
//            swingCam.inputZoom( (1 - camZoomRate * f), minZoomX, minZoomY) );
//        addIncrementalRangeAction("cra:(cam,O)", (f)->
//            swingCam.inputZoom( (1 + camZoomRate * f), minZoomX, minZoomY) );

        InputHandler input = craft.input;
        addToggleAction("cra:fire", (b) -> input.attack.toggle(b) );
        addToggleAction("cra:up", (b) -> input.up.toggle(b) );
        addToggleAction("cra:down", (b) -> input.down.toggle(b) );
        addToggleAction("cra:left", (b) -> input.left.toggle(b) );
        addToggleAction("cra:right", (b) -> input.right.toggle(b) );

        TopDownMinicraft.start(craft, false);
    }



    float prevScore = 0;
    @Override protected float reward() {
        float nextScore = craft.frame();
        float ds = nextScore - prevScore;
        this.prevScore = nextScore;
        return ds;
    }

}
