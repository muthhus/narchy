package nars.experiment.minicraft;

import nars.$;
import nars.NAR;
import nars.experiment.minicraft.top.InputHandler;
import nars.experiment.minicraft.top.TopDownMinicraft;
import nars.remote.SwingAgent;
import nars.util.signal.NObj;
import nars.video.MatrixSensor;
import nars.video.PixelBag;
import spacegraph.SpaceGraph;
import spacegraph.obj.MatrixView;

import static java.lang.Math.round;
import static nars.experiment.minicraft.SideCraft.arrayRenderer;
import static spacegraph.obj.GridSurface.col;

/**
 * Created by me on 9/19/16.
 */
public class TopCraft extends SwingAgent {

    private final TopDownMinicraft craft;
    private final MatrixSensor pixels;
    private final SideCraft.PixelAutoClassifier camAE;

    public static void main(String[] args) {
        run(TopCraft::new, 15000);
    }

    public TopCraft(NAR nar) {
        super(nar, 0);

        this.craft = new TopDownMinicraft();


        //swingCam.input(W/4, H/4, W/2, H/2); //50%

//        Scale cam = new Scale(swingCam, 48, 48);
        PixelBag cam = new PixelBag(craft.image, 64, 64);

        pixels = addCamera("cra", cam, (v) -> $.t( v, alpha));

        camAE = new SideCraft.PixelAutoClassifier("cra", cam.pixels, 16, 16, 16, 4, this);
        SpaceGraph.window(
                col(
                    new MatrixView(camAE.W.length, camAE.W[0].length, arrayRenderer(camAE.W)),
                    new MatrixView(camAE.pixRecon.length, camAE.pixRecon[0].length, arrayRenderer(camAE.pixRecon))
                ),
                500, 500
        );


        actionBipolar("cra:(cam,X)", (f)-> {
            cam.setX(f);
            return true;
        });
        actionBipolar("cra:(cam,Y)", (f)-> {
            cam.setY(f);
            return true;
        });
        actionBipolar("cra:(cam,Z)", (f)-> {
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
        actionToggle("cra:fire", (b) -> input.attack.toggle(b) );
        actionToggle("cra:up", (b) -> input.up.toggle(b) );
        actionToggle("cra:down", (b) -> input.down.toggle(b) );
        actionToggle("cra:left", (b) -> input.left.toggle(b) );
        actionToggle("cra:right", (b) -> input.right.toggle(b) );

        TopDownMinicraft.start(craft, false);
    }



    float prevScore = 0;
    @Override protected float reward() {

        //camAE.learn = (nar.time() % 500 < 250);
        camAE.frame();

        float nextScore = craft.frameImmediate();
        float ds = nextScore - prevScore;
        this.prevScore = nextScore;
        return ds;
    }

}
