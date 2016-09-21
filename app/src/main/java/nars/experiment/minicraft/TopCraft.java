package nars.experiment.minicraft;

import nars.$;
import nars.NAR;
import nars.experiment.minicraft.top.InputHandler;
import nars.experiment.minicraft.top.TopDownMinicraft;
import nars.remote.SwingAgent;
import nars.util.signal.NObj;
import nars.video.CameraSensor;
import nars.video.PixelCast;

import static java.lang.Math.round;

/**
 * Created by me on 9/19/16.
 */
public class TopCraft extends SwingAgent {

    private final TopDownMinicraft craft;
    private final CameraSensor<?> pixels;

    public static void main(String[] args) {
        playSwing(TopCraft::new, 15000);
    }

    public TopCraft(NAR nar) {
        super(nar, 0);

        this.craft = new TopDownMinicraft();


        //swingCam.input(W/4, H/4, W/2, H/2); //50%

//        Scale cam = new Scale(swingCam, 48, 48);
        PixelCast cam = new PixelCast(craft.image, 72, 64);

        pixels = addCamera("cra", cam, (v) -> $.t( v, alpha));


        addIncrementalRangeAction("cra:(cam,X)", (f)-> {
            cam.setX(f);
            return true;
        });
        addIncrementalRangeAction("cra:(cam,Y)", (f)-> {
            cam.setY(f);
            return true;
        });
        addIncrementalRangeAction("cra:(cam,Z)", (f)-> {
            cam.setZ(f);
            return true;
        });

//relative camera controls:
//        float camXYSpeed = 0.1f;
//        float camZoomRate = 0.1f;
//        int minZoomX = 64;
//        int minZoomY = 64;
//        addIncrementalRangeAction("cra:(cam,L)", (f)-> {
//            cam.minX = Math.max(0, Math.min(cam.minX + f * camXYSpeed, cam.maxX - minWidth));
//            return true;
//        });
//        addIncrementalRangeAction("cra:(cam,R)", (f)-> {
//            cam.maxX = Math.max(cam.minX + minWidth, Math.min(cam.maxX + f * camXYSpeed, 1));
//            return true;
//        });
//        addIncrementalRangeAction("cra:(cam,U)", (f)-> {
//            cam.minY = Math.max(0, Math.min(cam.minY + f * camXYSpeed, cam.maxY - minWidth));
//            return true;
//        });
//        addIncrementalRangeAction("cra:(cam,D)", (f)-> {
//            cam.maxY = Math.max(cam.minY + minWidth, Math.min(cam.maxY + f * camXYSpeed, 1));
//            return true;
//        });

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

        new NObj("cra", craft, nar)
                .read(
                    "player.health",
                    "player.dir",
                    "player.getTile().connectsToGrass",
                    "player.getTile().connectsToWater"
                ).into(this);

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
        float nextScore = craft.frameImmediate();
        float ds = nextScore - prevScore;
        this.prevScore = nextScore;
        return ds;
    }

}
