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

import com.gs.collections.api.tuple.Twin;
import com.gs.collections.impl.tuple.Tuples;
import nars.NAR;
import nars.agent.NAgent;
import nars.experiment.Environment;
import nars.experiment.tetris.visualizer.TetrisVisualizer;
import nars.index.CaffeineIndex;
import nars.learn.Agent;
import nars.nar.Default;
import nars.nar.util.DefaultConceptBuilder;
import nars.time.FrameClock;
import nars.util.data.random.XorShift128PlusRandom;

import javax.swing.*;
import java.util.Random;


public class Tetris implements Environment {

    private final TetrisVisualizer vis;
    private double currentScore;
    public TetrisState game;
    
    private int nextAction;

    private double previousScore;
    public float[] seenState;

    public Tetris(int width, int height) {
        game = new TetrisState(width, height);
        vis = new TetrisVisualizer(this, 32);
        JFrame win = new JFrame();
        win.setContentPane(vis);
        win.setSize(vis.size());
        win.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        win.setVisible(true);


        restart();
    }



    @Override
    public float pre(int t, float[] ins) {

        this.seenState = ins;

        return (float)getReward();
    }

    @Override
    public void post(int t, int action, float[] ins, Agent a) {
        step(action);
    }

    public int numActions() {
        return 6;
    }

    public double getReward() {
        return Math.max(-30, Math.min(30, currentScore - previousScore))/30.0;
    }


    public boolean takeAction(int action) {
        nextAction = action;
        return true;
    }



    public void restart() {
        game.reset();
        game.spawn_block();
        game.running = true;
        previousScore = currentScore = 0;
    }

    public double step(int nextAction) {

        if (nextAction > 5 || nextAction < 0) {
            throw new RuntimeException("Invalid action selected in Tetrlais: " + nextAction);            
        }

        if (game.running) {
            game.take_action(nextAction);
            game.update();
        } else {
            game.spawn_block();
        }

        game.toVector(false, seenState);
        vis.repaint();


        if (!game.gameOver()) {
            previousScore = currentScore;
            currentScore = game.get_score();
            return currentScore - previousScore;
        } else {
            restart();
            return 0;
        }

    }


    public int getWidth() {
        return game.width;
    }

    public int getHeight() {
        return game.height;
    }

    @Override
    public Twin<Integer> start() {
        return Tuples.twin(getWidth()*getHeight(),numActions());
    }

    public static void main(String[] args) {
        Random rng = new XorShift128PlusRandom(1);

        //Multi nar = new Multi(4,512,
        Default nar = new Default(1024,
                4, 2, 2, rng,
                new CaffeineIndex(new DefaultConceptBuilder(rng), 1000000, false)

                ,new FrameClock());
        nar.conceptActivation.setValue(0.3f);


        nar.beliefConfidence(0.8f);
        nar.goalConfidence(0.8f); //must be slightly higher than epsilon's eternal otherwise it overrides
        nar.DEFAULT_BELIEF_PRIORITY = 0.3f;
        nar.DEFAULT_GOAL_PRIORITY = 0.8f;
        nar.DEFAULT_QUESTION_PRIORITY = 0.5f;
        nar.DEFAULT_QUEST_PRIORITY = 0.5f;
        nar.cyclesPerFrame.set(16);
        nar.confMin.setValue(0.02f);


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
        //new MySTMClustered(nar, 8, '.', 2);
        //new MySTMClustered(nar, 8, '!');


        NAgent n = new NAgent(nar);

        new Tetris(8, 16).run(n, 5000);

        //nar.index.print(System.out);
        NAR.printTasks(nar, true);
        NAR.printTasks(nar, false);
        n.printActions();
        nar.forEachActiveConcept(System.out::println);
    }

}
