package nars.experiment.tetris;

import com.jogamp.opengl.GL2;
import nars.NAR;
import nars.NSchool;
import nars.experiment.tetris.impl.TetrisState;
import nars.nar.Alann;
import nars.nar.Default;
import nars.util.Util;
import spacegraph.Facial;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.obj.widget.*;
import spacegraph.obj.CrosshairSurface;
import spacegraph.render.Draw;

import java.awt.*;
import java.util.Date;

import static nars.experiment.tetris.TetriSchool.TrainingPanel.newTrainingPanel;
import static nars.gui.Vis.label;
import static nars.gui.Vis.stacking;
import static spacegraph.SpaceGraph.window;
import static spacegraph.obj.layout.Grid.col;
import static spacegraph.obj.layout.Grid.grid;
import static spacegraph.obj.layout.Grid.row;

public class TetriSchool extends NSchool implements Runnable {

    private final TetrisState game;


    final Thread sim;
    int updatePeriodMS = 50;

    public TetriSchool(NAR nar, int width, int height) {
        super(nar);

        game = new TetrisState(width, height, 2) {

        };


        sim = new Thread(this);
        sim.start();

        nar.input("a:b.", "b:c.");
        nar.loop(5);

    }

    @Override
    public void run() {
        while (true) {
            game.next();
            Util.sleep(updatePeriodMS);
        }
    }

    public static void main(String[] args) {
        int H = 16;
        int W = 8;

        //Alann n = new Alann();
        Default n = new Default();
        TetriSchool t = new TetriSchool(n, W, H);

        SpaceGraph s = window(row(
            newTrainingPanel(t),
            new MatrixPad(W, H, (x, y) ->
                new PushButton(/*x + "," + y*/) {
                    @Override
                    public void paintBack(GL2 gl) {
                        float bc = (t.game.seen[t.game.i(x, H-1-y)]);

                        Color c;
                        if ((bc < 1.0) && (bc > 0)) {
                            c = Color.WHITE; // falling block, ~0.5
                        } else if (bc > 0) {

                            switch ((int) bc) {
                                case 1:
                                    c = (Color.PINK);
                                    break;
                                case 2:
                                    c = (Color.RED);
                                    break;
                                case 3:
                                    c = (Color.GREEN);
                                    break;
                                case 4:
                                    c = (Color.YELLOW);
                                    break;
                                case 5:
                                    c = new Color(0.3f, 0.3f, 1.0f); // blue
                                    break;
                                case 6:
                                    c = (Color.ORANGE);
                                    break;
                                case 7:
                                    c = (Color.MAGENTA);
                                    break;
                                default:
                                    c = Color.BLACK;
                                    break;
                            }
                        } else {
                            c = Color.BLACK;
                        }

                        float r = c.getRed()/256f,
                              g = c.getGreen()/256f,
                              b = c.getBlue()/256f;
                        gl.glColor3f(r, g, b);

                        float m = 0.05f;
                        Draw.rect(gl, m, m, 1f-2*m, 1f-2*m);
                    }
                } )
        ), 1000, 800);

        s.add(new Facial(new CrosshairSurface(s)));
    }

    /**
     * -- clock controls
     * -- contextual commenting feedback input
     */
    public static class TrainingPanel {


        public static Surface newSchoolControl(NSchool school) {
            Surface runLabel = label("Slide");
            return col(

                    stacking(
                            new Slider(.25f, 0 /* pause */, 1),
                            runLabel
                    ),

//                    new PushButton("clickme", (p) -> {
//                        p.setText(String.valueOf(new Date().hashCode()));
//                    }),

                    grid(
                        new PushButton("a"), col(new CheckBox("fuck"),new CheckBox("shit")),
                        new PushButton("c"), new XYSlider()
                    )


            );
        }

        public static Surface newTrainingPanel(NSchool school) {

            ConsoleSurface term = new ConsoleSurface(80, 25);
            school.nar.log(term);
            return col(

                    newSchoolControl(school),

                    term
                    //new CrosshairSurface(s)

            );
        }

    }


}
