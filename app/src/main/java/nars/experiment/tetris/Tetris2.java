package nars.experiment.tetris;

import nars.$;
import nars.NAR;
import nars.Param;
import nars.experiment.NAREnvironment;
import nars.experiment.tetris.visualizer.TetrisVisualizer;
import nars.gui.BagChart;
import nars.gui.BeliefTableChart;
import nars.gui.NARSpace;
import nars.index.CaffeineIndex;
import nars.nar.Default;
import nars.nar.util.DefaultConceptBuilder;
import nars.op.ArithmeticInduction;
import nars.op.time.MySTMClustered;
import nars.term.Compound;
import nars.term.Termed;
import nars.term.obj.Termject;
import nars.time.FrameClock;
import nars.util.data.random.XorShift128PlusRandom;
import nars.util.signal.MotorConcept;
import nars.util.signal.SensorConcept;

import java.util.*;

import static nars.$.t;
import static nars.experiment.pong.Pong.numericSensor;
import static nars.experiment.tetris.TetrisState.*;
import static spacegraph.obj.ControlSurface.newControlWindow;

/**
 * Created by me on 7/28/16.
 */
public class Tetris2 extends NAREnvironment {

    public static final int runCycles = 512;
    public static final int cyclesPerFrame = 128;
    static int frameDelay = 10;

    private final TetrisState state;
    private final TetrisVisualizer vis;
    private double currentScore;

    private double previousScore;


    private MotorConcept motorRotate;
    private MotorConcept motorDown;
    private MotorConcept motorLeftRight;
    private boolean rotate;

    /**
     * @param width
     * @param height
     * @param timePerFall larger is slower gravity
     */
    public Tetris2(NAR nar, int width, int height, int timePerFall) {
        super(nar);
        state = new TetrisState(width, height, timePerFall) {
            @Override
            protected int nextBlock() {
                //return super.nextBlock(); //all blocks

                //EASY MODE
                rotate = false;
                return 1; //square blocks

                //return 0; //long blocks
            }
        };
        vis = new TetrisVisualizer(state, 64);

        //restart();
    }


    @Override
    public void init(NAR nar) {

        state.seen = new float[state.width * state.height];
        for (int x = 0; x < state.width; x++) {
            int xx = x;
            for (int y = 0; y < state.height; y++) {
                Compound squareTerm = $.p(new Termject.IntTerm(x), new Termject.IntTerm(y));
                int yy = y;
                sensors.add(new SensorConcept(squareTerm, nar,
                        () -> state.seen[yy * state.width + xx] > 0 ? 1f : 0f,
                        (v) -> t(v, alpha)
                ));
            }
        }


        if (rotate) {
            actions.add(motorRotate = new MotorConcept("(rotate)", nar));
        }
        //actions.add(motorDown = new MotorConcept("(down)", nar));
        actions.add(motorLeftRight = new MotorConcept("(leftright)", nar));

        reset();
    }

    @Override
    public float act() {

        //decide simultaneous motor actions: 0 or more
        long now = nar.time();
        previousScore = currentScore;


        float rotateMotivation = (motorRotate!=null && motorRotate.hasGoals()) ? motorRotate.goals().expectation(now) : 0.5f;
        //float downMotivation = motorDown.hasGoals() ? motorDown.goals().expectation(now) : 0.5f;
        float leftRightMotivation = motorLeftRight.hasGoals() ? motorLeftRight.goals().expectation(now) : 0.5f;

        float actionMargin = 0.25f;
        float actionThresholdHigh = 1f - actionMargin;
        float actionThresholdLow = actionMargin;

        if (rotateMotivation > actionThresholdHigh) {
            state.take_action(CW);
        } else if (rotateMotivation < actionThresholdLow) {
            state.take_action(CCW);
        }
//        if (downMotivation > actionThresholdHigh) {
//            state.take_action(FALL);
//        }

        if (leftRightMotivation > actionThresholdHigh) {
            state.take_action(RIGHT);
        } else if (leftRightMotivation < actionThresholdLow) {
            state.take_action(LEFT);
        }

        if (state.running) {
            state.take_action(-1); //actions already taken above
            state.update();
        } else {
            state.spawn_block();
        }

        state.toVector(false, state.seen);


        if (!state.gameOver()) {
            currentScore = state.get_score();
        } else {
            //System.out.println("restart");
            reset();
        }

        return (float) (currentScore - previousScore);
    }

    public void reset() {
        state.reset();
        state.spawn_block();
        state.running = true;
        currentScore = previousScore = state.get_score();
    }


    public static void main(String[] args) {
        Random rng = new XorShift128PlusRandom(1);

        Param.CONCURRENCY_DEFAULT = 2;
        //Multi nar = new Multi(3,512,
        Default nar = new Default(1024,
                4, 2, 2, rng,
                new CaffeineIndex(new DefaultConceptBuilder(rng),5 * 10000000, false)

                , new FrameClock());
        nar.inputActivation.setValue(0.08f);
        nar.derivedActivation.setValue(0.05f);


        nar.beliefConfidence(0.9f);
        nar.goalConfidence(0.65f);
        nar.DEFAULT_BELIEF_PRIORITY = 0.35f;
        nar.DEFAULT_GOAL_PRIORITY = 0.5f;
        nar.DEFAULT_QUESTION_PRIORITY = 0.3f;
        nar.DEFAULT_QUEST_PRIORITY = 0.4f;
        nar.cyclesPerFrame.set(cyclesPerFrame);
        nar.confMin.setValue(0.02f);

//        nar.on(new TransformConcept("seq", (c) -> {
//            if (c.size() != 3)
//                return null;
//            Term X = c.term(0);
//            Term Y = c.term(1);
//
//            Integer x = intOrNull(X);
//            Integer y = intOrNull(Y);
//            Term Z = (x!=null && y!=null)? ((Math.abs(x-y) <= 1) ? $.the("TRUE") : $.the("FALSE")) : c.term(2);
//
//
//            return $.inh($.p(X, Y, Z), $.oper("seq"));
//        }));
//        nar.believe("seq(#1,#2,TRUE)");
//        nar.believe("seq(#1,#2,FALSE)");

        //nar.log();
        //nar.logSummaryGT(System.out, 0.1f);

//		nar.log(System.err, v -> {
//			if (v instanceof Task) {
//				Task t = (Task)v;
//				if (t instanceof DerivedTask && t.punc() == '!')
//					return true;
//			}
//			return false;
//		});

        //Global.DEBUG = true;

        //new Abbreviation2(nar, "_");

        MySTMClustered stm = new MySTMClustered(nar, 256, '.', 3);
        MySTMClustered stmGoal = new MySTMClustered(nar, 256, '!', 2);

        new ArithmeticInduction(nar);


        Tetris2 t = new Tetris2(nar, 6, 12, 2) {
            @Override
            public void init(NAR nar) {
                super.init(nar);

                List<Termed> charted = new ArrayList(actions);
                charted.add(happy);
                new BeliefTableChart(nar, charted).show(600, 200);


                //BagChart.show((Default) nar, 512);

                newControlWindow(this);

                //STMView.show(stm, 800, 600);


                //NARSpace.newConceptWindow((Default) nar, 128, 8);
            }
        };


//        Iterable<Termed> cheats = Iterables.concat(
//                numericSensor(() -> t.currentX, nar, 0.3f,
//                        "(cursor_x)")
//                        //"(active,a)","(active,b)","(active,c)","(active,d)","(active,e)","(active,f)","(active,g)","(active,h)")
//                        //"I(a)","I(b)","I(c)","I(d)","I(e)","I(f)","I(g)","I(h)")
//                        //"(active,x)")
//                        .resolution(0.5f / t.width),
//                numericSensor(() -> t.currentY, nar, 0.3f,
//                        "(cursor_y)")
//                        //"active:(y,t)", "active:(y,b)")
//                        //"(active,y)")
//                        .resolution(0.5f / t.height)
//        );

//        NAgent n = new NAgent(nar) {
//            @Override
//            public void start(int inputs, int actions) {
//                super.start(inputs, actions);
//
//                List<Termed> charted = new ArrayList(super.actions);
//
//                charted.add(sad);
//                charted.add(happy);
//                Iterables.addAll(charted, cheats);
//
//                if (nar instanceof Default) {
//
//                    new BeliefTableChart(nar, charted).show(600, 900);
//
//                    //BagChart.show((Default) nar, 128);
//
//                    //STMView.show(stm, 800, 600);
//
//
//                    NARSpace.newConceptWindow((Default) nar, 128, 8);
//                }
//
//
//            }
//
//            @Override
//            protected Collection<Task> perceive(Set<Task> inputs) {
//                return super.perceive(inputs);
//            }
//        };


        //addCamera(t, nar, 8, 8);


        t.run(runCycles, frameDelay);


        nar.index.print(System.out);
        NAR.printTasks(nar, true);
        NAR.printTasks(nar, false);
        //nar.forEachActiveConcept(System.out::println);
    }

//    static void addCamera(Tetris t, NAR n, int w, int h) {
//        //n.framesBeforeDecision = GAME_DIVISOR;
//        SwingCamera s = new SwingCamera(t.vis);
//
//        NARCamera nc = new NARCamera("t", n, s, (x, y) -> $.p($.the(x), $.the(y)));
//
//        NARCamera.newWindow(s);
//
//        s.input(0, 0, t.vis.getWidth(),t.vis.getHeight());
//        s.output(w, h);
//
//        n.onFrame(nn -> {
//            s.update();
//        });
//    }

}
