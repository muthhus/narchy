package nars.experiment;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.gs.collections.api.tuple.Twin;
import nars.$;
import nars.Global;
import nars.NAR;
import nars.Op;
import nars.experiment.pong.PongEnvironment;
import nars.gui.BagChart;
import nars.gui.BeliefTableChart;
import nars.learn.Agent;
import nars.nal.Tense;
import nars.nar.Default;
import nars.op.time.MySTMClustered;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.util.math.FloatSupplier;
import nars.util.signal.SensorConcept;
import nars.vision.NARCamera;
import nars.vision.SwingCamera;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import static nars.Op.INH;

/**
 * Created by me on 7/5/16.
 */
public class CameraTrack implements Environment {


    private final JPanel scene;
    private final NARCamera cam;
    private final NAR nar
            ;
    Map<Term, SensorConcept> sensors;


    public CameraTrack(int sw, int sh, int width, int height, NAR nar) {
        this.nar = nar;
        this.scene = new JPanel() {

            float rotationspeed = 0; // 1f/150;
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, sw, sh);

                //background pattern
                g.setColor(new Color(20,20,20));
                int gr = 16;
                for (int i = 0; i < sw; i+=gr) {
                    for (int j = 0; j < sh; j+=gr) {
                        g.fillRect(i, j, gr/2, gr/2);
                    }
                }

//                g.setColor(Color.DARK_GRAY);
//                circle(g, sw / 2, sh / 2, sw / 4);

                float theta = nar.time() * rotationspeed;
                float r = sw/3f;


//                g.setColor(new Color(0, 255+nar.random.nextInt(1),0));    circle(g, r, theta, (int)r);
//                g.setColor(new Color(0, 255+nar.random.nextInt(1),0));  circle(g, r, theta + 1 * Math.PI/2f, (int)r);
//                g.setColor(new Color(0, 255+nar.random.nextInt(1),0));   circle(g, r, theta + 2 * Math.PI/2f, (int)r);
                g.setColor(new Color(0, 0, 255+nar.random.nextInt(1))); circle(g, r, theta + 3 * Math.PI/2f, (int)r);
            }

            public void circle(Graphics g, int x, int y, int r) {
                g.fillOval(x -r/2, y -r/2, r, r);
            }

            public void circle(Graphics g, float r, double theta, int rad) {
                double dx = Math.cos(theta) * r;
                double dy = Math.sin(theta) * r;
                g.fillOval((int)(getWidth()/2 + dx - rad/2), (int)(getHeight()/2 + dy - rad/2), rad, rad);

            }

        };
        JPanel overlay = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {


                if (null!=cam)
                    if (null!=((SwingCamera)cam.cam)) {
                        float reward = cam.controller.reward;
                        g.setColor(Color.getHSBColor(cam.controller.happy.beliefs().expectation(nar.time()), 0.5f, 0.5f));
                        Rectangle r = ((SwingCamera) cam.cam).input;
                        g.drawRect(r.x-1, r.y-1, r.width+1, r.height+1);
                    }
            }

        };
        overlay.setOpaque(false);
        scene.setSize(sw, sh);
        JFrame win = new JFrame();
        win.setGlassPane(overlay);
        overlay.setVisible(true);
        win.setContentPane(scene);
        win.setSize(sw, sh);
        win.setVisible(true);
        win.setResizable(false);


        SwingCamera swingCam = new SwingCamera(scene);

        sensors = Global.newHashMap(width*height*3);

        this.cam = new NARCamera(getClass().getSimpleName(), nar, swingCam,
                (x, y) -> $.p($.the(x), $.the(y))
                //(x, y) -> NARCamera.quadp(0, x, y, width, height)
        );
        cam.input(0,0,sw,sh);
        swingCam.output(width,height);
        NARCamera.newWindow(cam);


        //nar.logSummaryGT(System.out, 0.75f);

        //Global.DEBUG = true;
        nar.onTask(tt -> {
            //detect eternal derivations
//            if (!tt.isInput() && tt.isEternal() && tt.isBelief())
//                System.err.println(tt.proof());
//            if (!tt.isInput() && tt.isGoal()) {
//                if (tt.term().op()==INH && tt.term().term(1).op()==Op.OPER)
//                    System.err.println(tt.proof());
//            }
            /*if (!tt.isInput() && tt.term().hasAny(Op.VAR_DEP))
                System.err.println(tt.proof());*/
        });
        //nar.logSummaryGT(System.out, 0.2f);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                final int xx = x;
                final int yy = y;
                Compound t = (Compound) cam.p(x,y).term();

                //addSensor(nar, sensors, t, $.the("red"), () -> thresh(cam.red(xx, yy)));
                //addSensor(nar, sensors, t, $.the("gre"), () -> thresh(cam.green(xx, yy)));
                addSensor(nar, sensors, t, $.the("blu"), () -> thresh(cam.blue(xx, yy)));
            }
        }

        MutableFloat rt = new MutableFloat(), gt = new MutableFloat(), bt = new MutableFloat();



//        {
            BagChart.show((Default) nar);
//        }

        java.util.List<Termed> charted = new ArrayList();

//        charted.add($.$("[red]"));
//        charted.add($.$("[gre]"));
//        charted.add($.$("[blu]"));
//        charted.add($.$("(#x-->[red])"));
//        charted.add($.$("(#x-->[gre])"));

        Iterables.addAll( charted, Iterables.concat(
//            PongEnvironment.numericSensor("reddish", "low", "high", nar,
//                    () -> (float)/*Math.sqrt*/(rt.floatValue()),
//                    0.9f).resolution(0.05f),
//            PongEnvironment.numericSensor("greenness", "low", "high", nar,
//                    () -> (float)/*Math.sqrt*/(gt.floatValue()),
//                    0.9f).resolution(0.05f),
            PongEnvironment.numericSensor("blueness", "low", "high", nar,
                    () -> (float)/*Math.sqrt*/(bt.floatValue()),
                    0.9f).resolution(0.05f)
        ));
        nar.goal("(blueness-->low)", Tense.Eternal, 0f, 1f);
        nar.goal("(blueness-->high)", Tense.Eternal, 1f, 1f);

        nar.onFrame(nn-> {
            scene.repaint();
            overlay.repaint();

            rt.setValue(0);
            gt.setValue(0);
            bt.setValue(0);
            cam.update((x,y,t,r,g,b)->{
                rt.add(r);
                gt.add(g);
                bt.add(b);
            });

            float r = bt.floatValue() - (rt.floatValue() + gt.floatValue() / 2f);
            reward(r);

            //cam.controller.act(bt.floatValue() - (rt.floatValue() + gt.floatValue()/2f), (float[])null);
        });


        //charted.add($.$("(#x-->blu)"));
        //charted.add(/*nar.goal*/$.$("(blueness-->high)")/*, Tense.Eternal, 1f, 0.99f)*/.term());
        //charted.add(/*nar.goal*/$.$("(redness-->high)")/*, Tense.Eternal, 0f, 0.99f)*/.term());
        //charted.add(/*nar.goal*/$.$("(greenness-->high)")/*, Tense.Eternal, 0f, 0.99f)*/.term());
        //charted.add(nar.goal($.$("(#x-->[red])"), Tense.Eternal, 0f, 0.95f).term());
        //charted.add(nar.goal($.$("(#x-->[green])"), Tense.Eternal, 0f, 0.95f).term());
//        charted.add(nar.ask($.$("(#x-->blu)")));

        //Iterables.addAll(charted, cam.controller.rewardConcepts);
        charted.addAll(cam.controller.actions);

        Iterables.addAll(charted, PongEnvironment.numericSensor(()->cam.x, nar,0.5f,
                "(camx --> low)", "(camx --> mid)", "(camx --> high)").resolution(0.2f));
        Iterables.addAll(charted, PongEnvironment.numericSensor(()->cam.y, nar,0.5f,
                "(camy --> low)", "(camy --> mid)", "(camy --> high)").resolution(0.2f));
        Iterables.addAll(charted, PongEnvironment.numericSensor(()->cam.z, nar,0.59f,
                "(camz --> low)", "(camz --> mid)", "(camz --> high)").resolution(0.2f));

//            charted.add(nar.ask($.$("(?x-->red)").term()));
//            charted.add(nar.ask($.$("(?x<->g)").term()));
//            charted.add(nar.ask($.$("(?x<->b)").term()));

        new BeliefTableChart(nar, charted)
                .timeRadius(400)
                .show(600, 900)
        ;
    }

    private float thresh(float v) {
        return v > 0.5f ? 1f : 0f;
    }

    final DescriptiveStatistics rewardStat = new DescriptiveStatistics(128);

    public void reward(float r) {
        cam.controller.reward = r;
        rewardStat.addValue(r);
        if (nar.time() % 100 == 0) {
            System.out.println("reward~= " + rewardStat.getMean());
        }
    }

    public void addSensor(NAR nar, Map<Term, SensorConcept> sensors, Compound t, Term componentTerm, FloatSupplier component) {
        Compound tr = $.inh(t.term(), componentTerm);
        sensors.put(tr, new SensorConcept( tr, nar,
                component,
                f -> {
                    return $.t(f, 0.75f);
                }).resolution(0.1f).pri(0.1f));
    }

    @Override
    public Twin<Integer> start() {
        return null;
    }

    @Override
    public float pre(int t, float[] ins) {
        return 0;
    }

    @Override
    public void post(int t, int action, float[] ins, Agent a) {

    }

    public static void main(String[] args) {
        Default nar = new Default(1024, 4, 2, 2);
        nar.cyclesPerFrame.set(16);
        nar.beliefConfidence(0.7f);
        nar.goalConfidence(0.7f);
        nar.confMin.setValue(0.01f);
        nar.conceptActivation.setValue(0.01f);

        new MySTMClustered(nar, 16, '.', 4);

        new CameraTrack(256, 256, 7, 7, nar);


        nar.run(1500);

        NAR.printTasks(nar,false);
        //nar.loop(50f);

    }
}
