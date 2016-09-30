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
        run(TopCraft::new, 67500);
    }

    public TopCraft(NAR nar) {
        super(nar, 0);

        this.craft = new TopDownMinicraft();

        pixels = addCamera("cra", ()->craft.image, 48,48,(v) -> $.t( v, alpha));

//        camAE = new PixelAutoClassifier("cra", pixels.src.pixels, 8, 8, 48, this);
//        window(camAE.newChart(), 500, 500);

        new NObj("cra", craft, nar)
                .read(
                    //"player.health",
                    "player.dir",
                    "player.getTile().connectsToGrass",
                    "player.getTile().connectsToWater"
                ).into(this);


        InputHandler input = craft.input;
        actionToggle("cra:(fire)", (b) -> input.attack.toggle(b) );
        actionToggle("cra:(up)", (b) -> input.up.toggle(b) );
        actionToggle("cra:(down)", (b) -> input.down.toggle(b) );
        actionToggle("cra:(left)", (b) -> input.left.toggle(b) );
        actionToggle("cra:(right)", (b) -> input.right.toggle(b) );

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
        float r = 0.5f * ds + (craft.player.health/((float)craft.player.maxHealth)*2f - 1f);// + 0.25f * (craft.player.stamina*((float)craft.player.maxStamina))-0.5f);
        return r;
    }

}
