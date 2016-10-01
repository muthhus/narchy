package nars.experiment.minicraft;

import nars.$;
import nars.NAR;
import nars.experiment.minicraft.top.InputHandler;
import nars.experiment.minicraft.top.TopDownMinicraft;
import nars.remote.SwingAgent;
import nars.util.signal.NObj;
import nars.video.MatrixSensor;
import nars.video.PixelBag;

import static spacegraph.SpaceGraph.window;

/**
 * Created by me on 9/19/16.
 */
public class TopCraft extends SwingAgent {

    private final TopDownMinicraft craft;
    private final MatrixSensor<PixelBag> pixels;
    private PixelAutoClassifier camAE = null;

    public static void main(String[] args) {
        run(TopCraft::new, 15500);
    }

    public TopCraft(NAR nar) {
        super(nar, 1);

        this.craft = new TopDownMinicraft();

        pixels = addCamera("cra", ()->craft.image, 48,48,(v) -> $.t( v, alpha));

//        camAE = new PixelAutoClassifier("cra", pixels.src.pixels, 8, 8, 48, this);
//        window(camAE.newChart(), 500, 500);

//        new NObj("cra", craft, nar)
//                .read(
//                    //"player.health",
//                    //"player.dir",
//                    "player.getTile().connectsToGrass",
//                    "player.getTile().connectsToWater"
//                ).into(this);

        senseSwitch("cra:dir", ()->craft.player.dir, 0, 4);
        sense("cra:stamina", ()->(craft.player.stamina)/((float)craft.player.maxStamina));

        int tileMax = 13;
        senseSwitch("cra:(tile,0,0)", ()->craft.player.tile().id, 0, tileMax);
        senseSwitch("cra:(tile,0,1)", ()->craft.player.tile(0,1).id, 0, tileMax);
        senseSwitch("cra:(tile,0,-1)", ()->craft.player.tile(0,-1).id, 0, tileMax);
        senseSwitch("cra:(tile,1,0)", ()->craft.player.tile(1,0).id, 0, tileMax);
        senseSwitch("cra:(tile,-1,0)", ()->craft.player.tile(-1,0).id, 0, tileMax);

        InputHandler input = craft.input;
        actionToggle("cra:fire", (b) -> input.attack.toggle(b) );
        actionToggle("cra:(move,0,1)", (b) -> input.up.toggle(b) );
        actionToggle("cra:(move,0,-1)", (b) -> input.down.toggle(b) );
        actionToggle("cra:(move,-1,0)", (b) -> input.left.toggle(b) );
        actionToggle("cra:(move,1,0)", (b) -> input.right.toggle(b) );

        TopDownMinicraft.start(craft, false);
    }



    float prevScore = 0;
    @Override protected float reward() {

        //camAE.learn = (nar.time() % 500 < 250);
        if (camAE!=null)
            camAE.frame();

        float nextScore = craft.frameImmediate();
        float ds = nextScore - prevScore;
        this.prevScore = nextScore;
        float r = 0.5f * ds + 2f * (craft.player.health/((float)craft.player.maxHealth)*2f);// + 0.25f * (craft.player.stamina*((float)craft.player.maxStamina))-0.5f);
        return r;
    }

}
