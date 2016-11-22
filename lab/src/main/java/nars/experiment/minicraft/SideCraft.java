package nars.experiment.minicraft;

import nars.$;
import nars.NAR;
import nars.experiment.minicraft.side.SideScrollMinicraft;
import nars.experiment.minicraft.side.awtgraphics.AwtGraphicsHandler;
import nars.remote.NAgents;
import nars.util.Util;
import nars.video.PixelAutoClassifier;
import nars.video.PixelBag;
import nars.video.Sensor2D;

import java.awt.image.BufferedImage;

import static spacegraph.SpaceGraph.window;

/**
 * Created by me on 9/19/16.
 */
public class SideCraft extends NAgents {

    private final SideScrollMinicraft craft;
    private final Sensor2D pixels;
    private PixelAutoClassifier camAE;

    public static void main(String[] args) {
        runRT(SideCraft::new, 30, 5);
    }

    public SideCraft(NAR nar) {
        super(nar, 1);

        this.craft = new SideScrollMinicraft();


        //swingCam.input(W/4, H/4, W/2, H/2); //50%

//        Scale cam = new Scale(swingCam, 48, 48);
//        SwingCamera swing = new SwingCamera(((AwtGraphicsHandler) craft.gfx).buffer);
//        nar.onFrame(nn -> {
//            swing.update();
//        });

        BufferedImage camBuffer = ((AwtGraphicsHandler) craft.gfx).buffer;

        PixelBag cam = new PixelBag(camBuffer, 48, 32).addActions("cra", this);


        camAE = new PixelAutoClassifier("cra", cam.pixels, 8, 8, 32, this);
        window(camAE.newChart(), 500, 500);


        pixels = addCamera("cra", cam, (v) -> $.t(v, alpha));


//        new NObj("cra", craft, nar)
//                .read(
//                    "player.health",
//                    "player.dir",
//                    "player.getTile().connectsToGrass",
//                    "player.getTile().connectsToWater"
//                ).into(this);

//        InputHandler input = craft.input;
        actionToggle("cra(key,left)", (b) -> {
            if (b) craft.player.startLeft(false /* slow */);
            else craft.player.stopLeft();
        });
        actionToggle("cra(key,right)", (b) -> {
            if (b) craft.player.startRight(false /* slow */);
            else craft.player.stopRight();
        });
        actionToggle("cra(key,up)", (b) -> {
            if (b) craft.player.startClimb();
            else craft.player.stopClimb();
        });
        actionToggle("cra(key,mouseL)", (b) -> craft.leftClick = b);
        actionToggle("cra(key,mouseR)", (b) -> craft.rightClick = b);
        float mSpeed = 45f;
        actionBipolar("cra(mouse,X)", (v) -> {
            int x = craft.screenMousePos.x;
            int xx = Util.clamp(x + v * mSpeed, 0, camBuffer.getWidth() - 1);
            if (xx != x) {
                craft.screenMousePos.x = xx;
                return true;
            }
            return false;
        });
        actionBipolar("cra(mouse,Y)", (v) -> {
            int y = craft.screenMousePos.y;
            int yy = Util.clamp(y + v * mSpeed, 0, camBuffer.getHeight() - 1);
            if (yy != y) {
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
    final int gameFramesPerCycle = 1;

    @Override
    protected float act() {

        float nextScore = 0;
        for (int i = 0; i < gameFramesPerCycle; i++)
            nextScore = craft.frame();
        float ds = nextScore - prevScore;
        this.prevScore = nextScore;
        return ds;
    }

}
