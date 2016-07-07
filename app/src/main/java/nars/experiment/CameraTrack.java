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

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

/**
 * Created by me on 7/5/16.
 */
public class CameraTrack implements Environment {


    private final JPanel scene;
    private final NARCamera cam;
    Map<Term, SensorConcept> sensors;


    public CameraTrack(int sw, int sh, int width, int height, NAR nar) {
        this.scene = new JPanel() {

            float rotationspeed = 1f/250;
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, sw, sh);
//                g.setColor(Color.DARK_GRAY);
//                circle(g, sw / 2, sh / 2, sw / 4);

                float theta = nar.time() * rotationspeed;
                float r = sw/3f;


                g.setColor(new Color(0, 255+nar.random.nextInt(1),0));    circle(g, r, theta, (int)r);
                g.setColor(new Color(0, 255+nar.random.nextInt(1),0));  circle(g, r, theta + 1 * Math.PI/2f, (int)r);
                g.setColor(new Color(0, 255+nar.random.nextInt(1),0));   circle(g, r, theta + 2 * Math.PI/2f, (int)r);
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
                g.setColor(Color.WHITE);


                if (null!=cam)
                    if (null!=((SwingCamera)cam.cam)) {
                        Rectangle r = ((SwingCamera) cam.cam).input;
                        g.drawRect(r.x, r.y, r.width, r.height);
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



        SwingCamera swingCam = new SwingCamera(scene);

        sensors = Global.newHashMap(width*height*3);

        this.cam = new NARCamera(getClass().getSimpleName(), nar, swingCam,
                (x, y) -> $.p($.the(x), $.the(y))
                //(x, y) -> NARCamera.quadp(0, x, y, width, height)
        );
        cam.input(0,0,sw,sh);
        swingCam.output(width,height);
        NARCamera.newWindow(cam);


        //nar.logSummaryGT(System.out, 0.5f);
        Global.DEBUG = true;
//        nar.onTask(tt -> {
//            //detect eternal derivations
//            if (!tt.isInput() && tt.isEternal() && tt.isBelief())
//                System.err.println(tt.proof());
//            /*if (!tt.isInput() && tt.term().hasAny(Op.VAR_DEP))
//                System.err.println(tt.proof());*/
//        });
        //nar.logSummaryGT(System.out, 0.2f);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                final int xx = x;
                final int yy = y;
                Compound t = (Compound) cam.p(x,y).term();

                addSensor(nar, sensors, t, $.the("red"), () -> cam.red(xx, yy));
                addSensor(nar, sensors, t, $.the("gre"), () -> cam.green(xx, yy));
                addSensor(nar, sensors, t, $.the("blu"), () -> cam.blue(xx, yy));
            }
        }

        MutableFloat rt = new MutableFloat(), gt = new MutableFloat(), bt = new MutableFloat();



//        {
//            BagChart.show((Default) nar);
//        }

        java.util.List<Termed> charted = new ArrayList();

//        charted.add($.$("[red]"));
//        charted.add($.$("[gre]"));
//        charted.add($.$("[blu]"));
//        charted.add($.$("(#x-->[red])"));
//        charted.add($.$("(#x-->[gre])"));

        Iterables.addAll( charted, Iterables.concat(
            PongEnvironment.numericSensor("reddish", "low", "high", nar,
                    () -> (float)/*Math.sqrt*/(rt.floatValue()),
                    0.9f).resolution(0.05f),
            PongEnvironment.numericSensor("greenness", "low", "high", nar,
                    () -> (float)/*Math.sqrt*/(gt.floatValue()),
                    0.9f).resolution(0.05f),
            PongEnvironment.numericSensor("blueness", "low", "high", nar,
                    () -> (float)/*Math.sqrt*/(bt.floatValue()),
                    0.9f).resolution(0.05f)
        ));

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

            cam.controller.act(bt.floatValue() - (rt.floatValue() + gt.floatValue()/2f), (float[])null);
        });


        //charted.add($.$("(#x-->blu)"));
        //charted.add(/*nar.goal*/$.$("(blueness-->high)")/*, Tense.Eternal, 1f, 0.99f)*/.term());
        //charted.add(/*nar.goal*/$.$("(redness-->high)")/*, Tense.Eternal, 0f, 0.99f)*/.term());
        //charted.add(/*nar.goal*/$.$("(greenness-->high)")/*, Tense.Eternal, 0f, 0.99f)*/.term());
        //charted.add(nar.goal($.$("(#x-->[red])"), Tense.Eternal, 0f, 0.95f).term());
        //charted.add(nar.goal($.$("(#x-->[green])"), Tense.Eternal, 0f, 0.95f).term());
//        charted.add(nar.ask($.$("(#x-->blu)")));

        Iterables.addAll(charted, cam.controller.rewardConcepts);
        charted.addAll(cam.controller.actions);

//            charted.add(nar.ask($.$("(?x-->red)").term()));
//            charted.add(nar.ask($.$("(?x<->g)").term()));
//            charted.add(nar.ask($.$("(?x<->b)").term()));

        new BeliefTableChart(nar, charted).show(600, 900);
    }

    public void addSensor(NAR nar, Map<Term, SensorConcept> sensors, Compound t, Term componentTerm, FloatSupplier component) {
        Compound tr = $.instprop(t.term(), componentTerm);
        sensors.put(tr, new SensorConcept( tr, nar,
                component,
                f -> {
                    return $.t((float)Math.sqrt(f), 0.95f);
                }).resolution(0.2f).pri(0.04f));
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
        Default nar = new Default(1024, 8, 2, 2);
        nar.cyclesPerFrame.set(32);
        nar.beliefConfidence(0.8f);
        nar.goalConfidence(0.8f);
        nar.confMin.setValue(0.02f);
        nar.conceptActivation.setValue(0.02f);

        new MySTMClustered(nar, 8, '.', 2);

        new CameraTrack(256, 256, 6,6, nar);

        BagChart.show((Default) nar, 32);

        nar.run(15000);
        //nar.loop(50f);

    }
}
