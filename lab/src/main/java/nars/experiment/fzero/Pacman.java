package nars.experiment.fzero;

import jcog.Util;
import nars.$;
import nars.NAR;
import nars.NAgentX;
import nars.Narsese;
import nars.experiment.pacman.PacMan;
import nars.nar.Default;
import nars.nar.NARBuilder;
import nars.term.atom.Atomic;
import nars.time.RealTime;
import nars.video.BufferedImageBitmap2D;
import nars.video.Scale;
import nars.video.SwingBitmap2D;

/**
 * Created by me on 4/30/17.
 */
public class Pacman extends NAgentX {

    private final PacMan g;

    public Pacman(NAR nar) throws Narsese.NarseseException {
        super("G", nar);

        this.g = new PacMan();

//        senseCamera("G", g.view, 64, 64, (v) -> t(v, alpha()))
//                .setResolution(0.01f);
         Scale camScale = new Scale( new SwingBitmap2D(g.view), 64, 64);
            for (BufferedImageBitmap2D.ColorMode cm : new BufferedImageBitmap2D.ColorMode[] {
                    BufferedImageBitmap2D.ColorMode.R,
                    BufferedImageBitmap2D.ColorMode.G,
                    BufferedImageBitmap2D.ColorMode.B
            }) {
                senseCamera("(G,c" + cm.name() + ")",
                        camScale.filter(cm)
                )
                        .setResolution(0.5f);
            }

        actionTriState($.inh(Atomic.the("x"), id), (dh) -> {
            g.keys[0 /* left */] = false;
            g.keys[1 /* right */] = false;
            switch (dh) {
                case +1:
                    g.keys[1] = true;
                    break;
                case -1:
                    g.keys[0] = true;
                    break;
            }
        });

       actionTriState($.inh(Atomic.the("y"), id), (dh) -> {
            g.keys[2 /* up */] = false;
            g.keys[3 /* down */] = false;
            switch (dh) {
                case +1:
                    g.keys[2] = true;
                    break;
                case -1:
                    g.keys[3] = true;
                    break;
            }
        });


    }


    int lastScore = 0;

    @Override
    protected float act() {


        int nextScore = g.score;

        float r = (nextScore - lastScore);
//        if (r > 0)
//            System.out.println(r);
        lastScore = nextScore;


            return 2f * (Util.sigmoid(r) - 0.5f);
    }

    public static void main(String[] args) throws Narsese.NarseseException {
        Default n = NARBuilder.newMultiThreadNAR(
                2,
                new RealTime.DSHalf(true)
                        .durFPS(5f), true);

        Pacman a = new Pacman(n);
        a.startRT(5f);


        NAgentX.chart(a);

    }

}
