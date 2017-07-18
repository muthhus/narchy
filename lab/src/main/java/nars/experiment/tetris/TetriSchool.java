//package nars.experiment.tetris;
//
//import com.jogamp.opengl.GL2;
//import jcog.Util;
//import jcog.data.FloatParam;
//import nars.$;
//import nars.NAR;
//import nars.Narsese;
//import nars.concept.SensorConcept;
//import nars.experiment.tetris.impl.TetrisState;
//import nars.gui.Vis;
//import nars.term.Termed;
//import spacegraph.Ortho;
//import spacegraph.SpaceGraph;
//import spacegraph.Surface;
//import spacegraph.render.Draw;
//import spacegraph.space.CrosshairSurface;
//import spacegraph.widget.button.CheckBox;
//import spacegraph.widget.button.MatrixPad;
//import spacegraph.widget.button.PushButton;
//import spacegraph.widget.console.ConsoleSurface;
//import spacegraph.widget.slider.BaseSlider;
//import spacegraph.widget.slider.XYSlider;
//
//import java.awt.*;
//import java.util.List;
//
//import static nars.experiment.tetris.TetriSchool.TrainingPanel.newTrainingPanel;
//import static nars.gui.Vis.label;
//import static nars.gui.Vis.stack;
//import static spacegraph.SpaceGraph.window;
//import static spacegraph.layout.Grid.*;
//
//public class TetriSchool implements Runnable {
//
//    private final TetrisState game;
//
//
//    final Thread sim;
//    private final List<SensorConcept> cells;
//    private final NAR nar;
//    int updatePeriodMS = 200;
//
//    public TetriSchool(NAR nar, int width, int height) {
//        this.nar = nar;
//
//        cells = $.newArrayList(width*height);
//
//        game = new TetrisState(width, height, 2) {
//
//            @Override
//            public int spawn_block() {
//                int b = super.spawn_block();
//                try {
//                    nar.input("tetris(block," + b + "). :|:");
//                } catch (Narsese.NarseseException e) {
//                    e.printStackTrace();
//                }
//                return b;
//            }
//
//            @Override
//            public void reset() {
//                super.reset();
//                try {
//                    nar.input("tetris(reset). :|:");
//                } catch (Narsese.NarseseException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            @Override
//            public void next() {
//                super.next();
//
//                try {
//                    nar.input("tetris(time," + game.time + " ). :|:");
//                } catch (Narsese.NarseseException e) {
//                    e.printStackTrace();
//                }
//                cells.forEach(d -> nar.input(d.apply(nar)));
//            }
//        };
//
//        //sensors(nar, game, cells);
//
//        sim = new Thread(this);
//        sim.start();
//
//        //nar.loop(50);
//
//    }
//
//    @Override
//    public void run() {
//        while (true) {
//            game.next();
//            Util.sleep(updatePeriodMS);
//        }
//    }
//
//    public static void main(String[] args) {
//        int H = 12;
//        int W = 6;
//
//
//        NAR n =
//                //NAgents.newAlann();
//                ((MultiNAR) null).startPeriodMS(40).nar;
//
//
//        TetriSchool t = new TetriSchool(n, W, H);
//
//        SpaceGraph s = window(row(
//            newTrainingPanel(t),
//            new MatrixPad(W, H, (x, y) ->
//                new PushButton(/*x + "," + y*/) {
//                    @Override
//                    public void paintBack(GL2 gl) {
//                        float bc = (t.game.seen[t.game.i(x, H-1-y)]);
//
//                        Color c;
//                        if ((bc < 1.0) && (bc > 0)) {
//                            c = Color.WHITE; // falling block, ~0.5
//                        } else if (bc > 0) {
//
//                            switch ((int) bc) {
//                                case 1:
//                                    c = (Color.PINK);
//                                    break;
//                                case 2:
//                                    c = (Color.RED);
//                                    break;
//                                case 3:
//                                    c = (Color.GREEN);
//                                    break;
//                                case 4:
//                                    c = (Color.YELLOW);
//                                    break;
//                                case 5:
//                                    c = new Color(0.3f, 0.3f, 1.0f); // blue
//                                    break;
//                                case 6:
//                                    c = (Color.ORANGE);
//                                    break;
//                                case 7:
//                                    c = (Color.MAGENTA);
//                                    break;
//                                default:
//                                    c = Color.BLACK;
//                                    break;
//                            }
//                        } else {
//                            c = Color.DARK_GRAY;
//                        }
//
//                        float r = c.getRed()/256f,
//                              g = c.getGreen()/256f,
//                              b = c.getBlue()/256f;
//
//                        float pri = 1f; //n.pri( t.cell(x, y));
//                        gl.glColor3f(r * pri, g * pri, b * pri);
//
//                        float m = 0.05f;
//                        Draw.rect(gl, m, m, 1f-2*m, 1f-2*m);
//                    }
//                } )
//        ), 1000, 800);
//
//        s.add(new Ortho(new CrosshairSurface(s)));
//    }
//
//    private Termed cell(int x, int y) {
//        return cells.get(game.i(x, y));
//    }
//
//    /**
//     * -- clock controls
//     * -- contextual commenting feedback input
//     */
//    public static class TrainingPanel {
//
//
//        public static Surface newSchoolControl(TetriSchool school) {
//            Surface runLabel = label("Slide");
//            return col(
//
//                    stack(
//                            new BaseSlider(.25f  /* pause */),
//                            runLabel
//                    ),
//
////                    new PushButton("clickme", (p) -> {
////                        p.setText(String.valueOf(new Date().hashCode()));
////                    }),
//
//                    grid(
//
//                        new CheckBox("play").on((c, e)->{
//                            school.updatePeriodMS = (e ? 200 : 10000);
//                        }).set(true),
//
//                        col(new CheckBox("fuck"),new CheckBox("shit")),
//
//                        new PushButton("c"), new XYSlider()
//                    )
//
//
//            );
//        }
//
//        public static Surface newTrainingPanel(TetriSchool school) {
//
//            ConsoleSurface term = Vis.logConsole(school.nar, 120, 40, new FloatParam(0.25f));
//
//            return col(
//
//                    newSchoolControl(school),
//
//                    term
//
//            );
//        }
//
//    }
//
//
//}
