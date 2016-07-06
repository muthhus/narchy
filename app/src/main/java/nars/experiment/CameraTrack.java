package nars.experiment;

import com.gs.collections.api.tuple.Twin;
import nars.$;
import nars.Global;
import nars.NAR;
import nars.Op;
import nars.gui.BeliefTableChart;
import nars.learn.Agent;
import nars.nar.Default;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.util.math.FloatSupplier;
import nars.util.signal.SensorConcept;
import nars.vision.NARCamera;
import nars.vision.SwingCamera;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by me on 7/5/16.
 */
public class CameraTrack implements Environment {

    private final int w;
    private final int h;
    private final JPanel scene;
    private final NARCamera cam;
    Map<Term, SensorConcept> sensors;

    public CameraTrack(int sw, int sh, int w, int h, NAR nar) {
        this.scene = new JPanel() {
            @Override public void paint(Graphics g) {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, sw, sh);
                g.setColor(Color.WHITE);
                circle(g, sw / 2, sh / 2, sw/3);
                g.setColor(Color.RED);
                circle(g, sw / 6, sh / 6, sw/6);
                g.setColor(Color.GREEN);
                circle(g, getWidth() - sw / 6, sh / 6, sw/6);
                g.setColor(Color.BLUE);
                circle(g, sw / 6, getHeight() - sh / 6, sw/6);
                g.setColor(Color.YELLOW);
                circle(g, getWidth() - sw / 6, getHeight() - sh / 6, sw/6);
            }

            public void circle(Graphics g, int x, int y, int r) {
                g.fillOval(x -r/2, y -r/2, r, r);
            }
        };
        scene.setSize(sw, sh);
        JFrame win = new JFrame();
        win.setContentPane(scene);
        win.setSize(sw, sh);
        win.setVisible(true);


        this.w = w;
        this.h = h;

        SwingCamera swingCam = new SwingCamera(scene);
        int width = 12;
        int height = 12;
        sensors = Global.newHashMap(width*height*3);

        this.cam = new NARCamera(getClass().getSimpleName(), nar, swingCam,
//                (x, y) -> $.p($.the(x), $.the(y))
                (x, y) -> NARCamera.quadp(0, x, y, width, height)
        );
        cam.input(0,0,256,256);
        swingCam.output(width,height);
        NARCamera.newWindow(cam);


        //nar.logSummaryGT(System.out, 0.5f);
        Global.DEBUG = true;
        nar.onTask(tt -> {
            //detect eternal derivations
            if (!tt.isInput() && tt.isEternal())
                System.err.println(tt.proof());
            /*if (!tt.isInput() && tt.term().hasAny(Op.VAR_DEP))
                System.err.println(tt.proof());*/
        });

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



//        {
//            BagChart.show((Default) nar);
//        }

        java.util.List<Termed> charted = new ArrayList();

        charted.add($.$("red"));
        charted.add($.$("gre"));
        charted.add($.$("blu"));
        charted.add($.$("(#x-->red)"));
        charted.add($.$("(#x-->gre)"));
        charted.add($.$("(#x-->blu)"));
        charted.add(nar.ask($.$("(?x && (#x-->blu))")));

//            charted.add(nar.ask($.$("(?x<->r)").term()));
//            charted.add(nar.ask($.$("(?x<->g)").term()));
//            charted.add(nar.ask($.$("(?x<->b)").term()));

        new BeliefTableChart(nar, charted).show(600, 900);
    }

    public void addSensor(NAR nar, Map<Term, SensorConcept> sensors, Compound t, Term componentTerm, FloatSupplier component) {
        Compound tr = $.inh(t.term(), componentTerm);
        sensors.put(tr, new SensorConcept( tr, nar,
                component,
                f -> $.t(f, 0.9f)).resolution(0.1f).pri(0.1f));
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
        Default nar = new Default();
        nar.cyclesPerFrame.set(128);
        nar.conceptActivation.setValue(0.15f);

        new CameraTrack(400, 400, 32, 32, nar);
        nar.loop(15f);

    }
}
