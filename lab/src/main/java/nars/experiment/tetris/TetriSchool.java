package nars.experiment.tetris;

import nars.NAR;
import nars.NSchool;
import nars.experiment.tetris.impl.TetrisState;
import nars.nar.Alann;
import spacegraph.Facial;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.obj.ConsoleSurface;
import spacegraph.obj.CrosshairSurface;
import spacegraph.obj.Slider;

import static nars.gui.Vis.label;
import static nars.gui.Vis.stacking;
import static spacegraph.obj.GridSurface.col;
import static spacegraph.obj.GridSurface.grid;
import static spacegraph.obj.GridSurface.row;

/**
 * Created by me on 11/11/16.
 */
public class TetriSchool extends NSchool {

    private final TetrisState state;

    public TetriSchool(NAR nar, int width, int height) {
        super(nar);

        state = new TetrisState(width, height, 1) {

        };
    }

    public static void main(String[] args) {
        TetriSchool t = new TetriSchool(new Alann(), 8, 16);

        SpaceGraph s = SpaceGraph.window(TrainingPanel.newTrainingPanel(t), 1000, 800);
        s.add(new Facial(new CrosshairSurface(s)));
    }

    /** -- clock controls
     *  -- contextual commenting feedback input */
    public static class TrainingPanel {


        public static Surface newSchoolControl(NSchool school) {
            Surface runLabel = label("Run");

            return grid(

                stacking(
                    new Slider(5, 0 /* pause */, 10),
                    runLabel
                ),

                stacking(
                    new Slider(0.5f, 0 /* pause */, 1f),
                    label("xyz")
                )

            );
        }

        public static Surface newTrainingPanel(NSchool school) {

            return col(

                newSchoolControl(school),

                new ConsoleSurface(new ConsoleSurface.Demo(80, 25))
                //new CrosshairSurface(s)

            );
        }

    }


}
