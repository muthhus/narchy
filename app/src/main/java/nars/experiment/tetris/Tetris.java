/*
Copyright 2007 Brian Tanner
http://rl-library.googlecode.com/
brian@tannerpages.com
http://brian.tannerpages.com

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package nars.experiment.tetris;

import com.google.common.collect.Iterables;
import com.gs.collections.api.tuple.Twin;
import com.gs.collections.impl.tuple.Tuples;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.agent.NAgent;
import nars.experiment.ArithmeticTest;
import nars.experiment.Environment;
import nars.experiment.tetris.visualizer.TetrisVisualizer;
import nars.gui.BagChart;
import nars.gui.BeliefTableChart;
import nars.index.CaffeineIndex;
import nars.index.TransformConcept;
import nars.learn.Agent;
import nars.nar.Default;
import nars.nar.Multi;
import nars.nar.util.DefaultConceptBuilder;
import nars.op.time.MySTMClustered;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.time.FrameClock;
import nars.util.data.random.XorShift128PlusRandom;
import nars.vision.NARCamera;
import nars.vision.SwingCamera;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static nars.experiment.ArithmeticTest.intOrNull;
import static nars.experiment.pong.Pong.numericSensor;


public class Tetris extends TetrisState implements Environment {

    private final TetrisVisualizer vis;
    private final JFrame window;
    private double currentScore;

    private double previousScore;
    public float[] seenState;
    private final static boolean NO_BACKWARDS_ROTATION = true;

    /**
     *
     * @param width
     * @param height
     * @param timePerFall larger is slower gravity
     */
    public Tetris(int width, int height, int timePerFall) {
        super(width, height, timePerFall);
        vis = new TetrisVisualizer(this, 32);
        window = new JFrame();

        window.setSize(vis.getWidth(), vis.getHeight()+32);

        SwingUtilities.invokeLater(()->{
            window.setContentPane(vis);
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setVisible(true);
        });

        restart();
    }



    @Override
    public float pre(int t, float[] ins) {

        if (this.seenState == null)
            this.seenState = new float[ins.length];

        //post process to monochrome bitmap
        for (int i = 0; i < ins.length; i++) {
            float v = seenState[i];
            if (v <= 0) v = 0;
            if (v > 0) v = 1;
            ins[i] = v;
        }
        //System.out.println(Texts.n4(ins));



        float r = (float)getReward();
        //System.out.println("rew=" + r);
        return r;
    }

    @Override
    public void preStart(Agent a) {
        if (a instanceof NAgent) {
            //provide custom sensor input names for the nars agent


            NAgent ag = (NAgent) a;
            NAR nar = ag.nar;

////            //number relations
//            for (int i = 0; i < Math.max(getWidth(),getHeight()); i++) {
//                if (i > 0) {
//                    nar.believe($.inh($.p($.the(i - 1), $.the(i)), $.the("next")), 1f, 1f);
//                }
////                    nar.believe($.inh($.p($.the(i),$.the(i-1)), $.the("prev")), 1f, 1f);
////                    nar.believe($.inst($.secte($.the(i-1),$.the(i)), $.the("tang")), 1f, 1f);
////                    //nar.believe($.inh($.sete($.the(i-1),$.the(i)), $.the("seq")), 1f, 1f);
////                }
//            }
//            nar.ask("(&&,t:(#a,#b),t:(#c,#d),tang:{(#a & #c)})");
//            nar.ask("(&&,t:(#a,#b),t:(#c,#d),tang:{(#b & #d)})");
            //nar.ask("(&&,t:(#a,#b),t:(#c,#d),(prev|next):(#b,#d))");
            //nar.ask("(&&,t:(#a,#b),t:(#c,#d),seq:{#a,#c})");
            //nar.ask("(&&,t:(#a,#b),t:(#c,#d),seq:{#b,#d})");

            ag.setSensorNamer((i) -> {
                int x = x(i);
                int y = y(i);

                //Compound squareTerm = $.inh($.p($.the(x), $.the(y)), $.the("t"));
                Compound squareTerm = $.p($.the(x), $.the(y));
                return squareTerm;

//                int dx = (visionRadius  ) - ax;
//                int dy = (visionRadius  ) - ay;
//                Atom dirX, dirY;
//                if (dx == 0) dirX = $.the("v"); //vertical
//                else if (dx > 0) dirX = $.the("r"); //right
//                else /*if (dx < 0)*/ dirX = $.the("l"); //left
//                if (dy == 0) dirY = $.the("h"); //horizontal
//                else if (dy > 0) dirY = $.the("u"); //up
//                else /*if (dy < 0)*/ dirY = $.the("d"); //down
//                Term squareTerm = $.p(
//                        //$.p(dirX, $.the(Math.abs(dx))),
//                        $.inh($.the(Math.abs(dx)), dirX),
//                        //$.p(dirY, $.the(Math.abs(dy)))
//                        $.inh($.the(Math.abs(dy)), dirY)
//                );
//                //System.out.println(dx + " " + dy + " " + squareTerm);
//
//                //return $.p(squareTerm, typeTerm);
//                return $.prop(squareTerm, typeTerm);
//                //return (Compound)$.inh($.the(square), typeTerm);
            });
        }
    }



    @Override
    public void post(int t, int action, float[] ins, Agent a) {
        step(action);
        System.out.println(a.summary());
    }

    public int numActions() {
        return 6;
    }

    public double getReward() {
        return Math.max(-30, Math.min(30, currentScore - previousScore))/10.0;
    }




    public void restart() {
        reset();
        spawn_block();
        running = true;
        previousScore = 0;
        currentScore = -50;
    }

    public double step(int nextAction) {


        if (nextAction > 5 || nextAction < 0) {
            throw new RuntimeException("Invalid action selected in Tetrlais: " + nextAction);            
        }

        if (running) {
            take_action(nextAction);
            update();
        } else {
            spawn_block();
        }

        toVector(false, seenState);
        vis.repaint();


        if (!gameOver()) {
            previousScore = currentScore;
            currentScore = get_score();
            return currentScore - previousScore;
        } else {
            //System.out.println("restart");
            restart();
            return 0;
        }

    }


    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public Twin<Integer> start() {
        return Tuples.twin(getWidth()*getHeight(),NO_BACKWARDS_ROTATION ? numActions()-1 : numActions());
    }

    public static void main(String[] args) {
        Random rng = new XorShift128PlusRandom(1);

        //Multi nar = new Multi(3,512,
        Default nar = new Default(1024,
                4, 2, 2, rng,
                new CaffeineIndex(new DefaultConceptBuilder(rng), 100000, false)

                ,new FrameClock());
        nar.conceptActivation.setValue(0.1f);


        nar.beliefConfidence(0.9f);
        nar.goalConfidence(0.8f); //must be slightly higher than epsilon's eternal otherwise it overrides
        nar.DEFAULT_BELIEF_PRIORITY = 0.5f;
        nar.DEFAULT_GOAL_PRIORITY = 0.6f;
        nar.DEFAULT_QUESTION_PRIORITY = 0.4f;
        nar.DEFAULT_QUEST_PRIORITY = 0.4f;
        nar.cyclesPerFrame.set(24);
        nar.confMin.setValue(0.03f);

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
        new MySTMClustered(nar, 16, '.', 2);
        new MySTMClustered(nar, 16, '!', 2);
        new ArithmeticTest.NumericDifferenceRule(nar);
        //new MySTMClustered(nar, 8, '!');


        Tetris t = new Tetris(6, 12, 4) {
            @Override
            protected int nextBlock() {
                //return super.nextBlock(); //all blocks
                return 1; //square blocks
                //return 0; //long blocks
            }
        };

        Iterable<Termed> cheats = Iterables.concat(
                numericSensor(() -> t.currentX, nar, 0.7f,
                        "I(x)")
                        //"(active,a)","(active,b)","(active,c)","(active,d)","(active,e)","(active,f)","(active,g)","(active,h)")
                        //"I(a)","I(b)","I(c)","I(d)","I(e)","I(f)","I(g)","I(h)")
                        //"(active,x)")
                        .resolution(1f/t.width),
                numericSensor(() -> t.currentY, nar, 0.7f,
                        "I(y)")
                        //"active:(y,t)", "active:(y,b)")
                        //"(active,y)")
                        .resolution(1f/t.height)
        );

        NAgent n = new NAgent(nar) {
            @Override
            public void start(int inputs, int actions) {
                super.start(inputs, actions);

                List<Termed> charted = new ArrayList(super.actions);

                charted.add(sad);
                charted.add(happy);
                Iterables.addAll(charted, cheats);

                if (nar instanceof Default) {
                    new BeliefTableChart(nar, charted).show(600, 900);
                    BagChart.show((Default) nar, 128);
                }
            }
        };






        //addCamera(t, nar, 8, 8);

        t.run(n, 2000);

        nar.index.print(System.out);
        NAR.printTasks(nar, true);
        NAR.printTasks(nar, false);
        n.printActions();
        nar.forEachActiveConcept(System.out::println);
    }

    static void addCamera(Tetris t, NAR n, int w, int h) {
        //n.framesBeforeDecision = GAME_DIVISOR;
        SwingCamera s = new SwingCamera(t.vis);

        NARCamera nc = new NARCamera("t", n, s, (x, y) -> $.p($.the(x), $.the(y)));

        NARCamera.newWindow(s);

        s.input(0, 0, t.vis.getWidth(),t.vis.getHeight());
        s.output(w, h);

        n.onFrame(nn -> {
            s.update();
        });
    }

}
