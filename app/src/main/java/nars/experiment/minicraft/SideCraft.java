package nars.experiment.minicraft;

import nars.$;
import nars.NAR;
import nars.experiment.minicraft.side.SideScrollMinicraft;
import nars.experiment.minicraft.side.awtgraphics.AwtGraphicsHandler;
import nars.remote.SwingAgent;
import nars.util.Util;
import nars.video.CameraSensor;
import nars.video.PixelCast;

import java.awt.image.BufferedImage;

/**
 * Created by me on 9/19/16.
 */
public class SideCraft extends SwingAgent {

    private final SideScrollMinicraft craft;
    private final CameraSensor<?> pixels;

    public static void main(String[] args) {
        playSwing(SideCraft::new, 15000);
    }

    public SideCraft(NAR nar) {
        super(nar, 0);

        this.craft = new SideScrollMinicraft();


        //swingCam.input(W/4, H/4, W/2, H/2); //50%

//        Scale cam = new Scale(swingCam, 48, 48);
//        SwingCamera swing = new SwingCamera(((AwtGraphicsHandler) craft.gfx).buffer);
//        nar.onFrame(nn -> {
//            swing.update();
//        });

        BufferedImage camBuffer = ((AwtGraphicsHandler) craft.gfx).buffer;
        PixelCast cam = new PixelCast(camBuffer, 72, 64);

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


//        new NObj("cra", craft, nar)
//                .read(
//                    "player.health",
//                    "player.dir",
//                    "player.getTile().connectsToGrass",
//                    "player.getTile().connectsToWater"
//                ).into(this);

//        InputHandler input = craft.input;
        addToggleAction("cra:left", (b) -> {
            if (b)
                craft.player.startLeft(false /* slow */);
            else
                craft.player.stopLeft();
        } );
        addToggleAction("cra:right", (b) -> {
            if (b)
                craft.player.startRight(false /* slow */);
            else
                craft.player.stopRight();
        } );
        addToggleAction("cra:up", (b) -> {
            if (b)
                craft.player.startClimb();
            else
                craft.player.stopClimb();
        } );
        addToggleAction("cra:(mouse,L)", (b) -> {
            craft.leftClick = b;
        } );
        addToggleAction("cra:(mouse,R)", (b) -> {
            craft.rightClick = b;
        } );
        float mSpeed = 4f;
        addIncrementalRangeAction("cra:(mouse,X)", (v) -> {
            int x = craft.screenMousePos.x;
            int xx = Util.clamp(x + v * mSpeed, 0, camBuffer.getWidth()-1);
            if (xx!=x) {
                craft.screenMousePos.x = xx;
                return true;
            }
            return false;
        });
        addIncrementalRangeAction("cra:(mouse,Y)", (v) -> {
            int y = craft.screenMousePos.y;
            int yy = Util.clamp(y + v * mSpeed, 0, camBuffer.getHeight()-1);
            if (yy!=y) {
                craft.screenMousePos.y = yy;
                return true;
            }
            return false;
        });


//        addToggleAction("cra:up", (b) -> input.up.toggle(b) );
//        addToggleAction("cra:down", (b) -> input.down.toggle(b) );
//        addToggleAction("cra:left", (b) -> input.left.toggle(b) );
//        addToggleAction("cra:right", (b) -> input.right.toggle(b) );

        craft.startGame(false, 512);
    }



    float prevScore = 0;
    @Override protected float reward() {
        float nextScore = craft.frame();
        float ds = nextScore - prevScore;
        this.prevScore = nextScore;
        return ds;
    }

}
