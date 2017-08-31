package nars.experiment.fzero;

import java4k.gradius4k.Gradius4K;
import nars.$;
import nars.NAR;
import nars.NAgentX;
import nars.concept.ScalarConcepts;
import nars.gui.Vis;
import nars.term.atom.Atomic;
import nars.video.BufferedImageBitmap2D;
import nars.video.CameraSensor;
import nars.video.Scale;
import nars.video.ShapeSensor;
import org.jetbrains.annotations.NotNull;

import static nars.term.atom.Atomic.the;
import static spacegraph.SpaceGraph.window;
import static spacegraph.layout.Grid.col;

/**
 * Created by me on 4/30/17.
 */
public class Gradius extends NAgentX {

    private final Gradius4K g;

    public Gradius(NAR nar) {
        super("G", nar);

        this.g = new Gradius4K();

        g.updateMS = 20;

        //BufferedImageBitmap2D cc = new Scale(() -> g.image, 48, 48).blur();
        CameraSensor<?> c = senseCameraRetina(id, () -> g.image, 24, 24).resolution(0.01f);

        new ShapeSensor($.p(id, $.the("shape")), new BufferedImageBitmap2D(()->g.image),this);

        float width = g.getWidth();
        float height = g.getHeight();
        @NotNull ScalarConcepts yPos = senseNumber(the("Y"),
                ()->g.player[Gradius4K.OBJ_Y] / height,
                4, ScalarConcepts.FuzzyNeedle
        ).resolution(0.1f);
        @NotNull ScalarConcepts xPos = senseNumber(the("X"),
                ()->g.player[Gradius4K.OBJ_X] / width,
                4, ScalarConcepts.FuzzyNeedle
        ).resolution(0.1f);
        window(
                col(
                        Vis.conceptBeliefPlots(this, xPos, 4),
                        Vis.conceptBeliefPlots(this, yPos, 4)
                ),
                500, 500);


//        PixelBag cc = PixelBag.of(() -> g.image, 64, 64);
//        cc.setClarity(0.5f, 0.9f);


//        //TODO fix the panning/zooming
//        onFrame((z) -> {
//
//            float x = (g.player[Gradius4K.OBJ_X] - g.cameraX) / g.getWidth();
//            float y = (g.player[OBJ_Y]) / g.getHeight();
//
//            cc.setXRelative(x + 0.5f);
//            cc.setYRelative(y + 0.5f);
//            cc.setZoom(0.5f);
//
//            //cc.setXRelative( mario.)
//        });

        //CameraSensor<PixelBag> camScale = senseCamera("(G,cam)" /*"(nario,local)"*/, cc);
        //camScale.resolution(0.1f);



        //nar.truthResolution.setValue(0.05f);

//        BufferedImageBitmap2D camScaleLow = new Scale(() -> g.image, 16, 16);
//        for (BufferedImageBitmap2D.ColorMode cm : new BufferedImageBitmap2D.ColorMode[]{
//                R,
//                BufferedImageBitmap2D.ColorMode.B,
//                BufferedImageBitmap2D.ColorMode.G
//        }) {
//            senseCamera("Gc" + cm.name(), /*"(G,c" + cm.name() + ")"*/
//                    //(cm == R ? camScale : camScaleLow).filter(cm)
//                    camScaleLow.filter(cm).blur()
//            ).resolution(0.05f);
//        }

        /*nar.believe($.inh(*/$.sete(

        actionToggle($.p(Atomic.the("fire")),
                (b) -> g.keys[Gradius4K.VK_SHOOT] = b).term(),

        actionTriState($.p(Atomic.the("dx")), (dh) -> {
            g.keys[Gradius4K.VK_LEFT] = false;
            g.keys[Gradius4K.VK_RIGHT] = false;
            switch (dh) {
                case +1:
                    g.keys[Gradius4K.VK_RIGHT] = true;
                    break;
                case -1:
                    g.keys[Gradius4K.VK_LEFT] = true;
                    break;
            }
        }).term(),

        actionTriState($.p(Atomic.the("dy")), (dh) -> {
            g.keys[Gradius4K.VK_UP] = false;
            g.keys[Gradius4K.VK_DOWN] = false;
            switch (dh) {
                case +1:
                    g.keys[Gradius4K.VK_DOWN] = true;
                    break;
                case -1:
                    g.keys[Gradius4K.VK_UP] = true;
                    break;
            }
        }).term())/*, id))*/;


    }


    int lastScore;

    @Override
    protected float act() {


        int nextScore = g.score;

        float r = (nextScore - lastScore);
//        if (r > 0)
//            System.out.println(r);
        lastScore = nextScore;

        if (g.playerDead > 1)
            return -1f;
        else
            return r;
    }

    public static void main(String[] args) {

        NAgentX.runRT((n) -> {


            Gradius a = null;
                a = new Gradius(n);

            return a;

        }, 10f);

    }

}
