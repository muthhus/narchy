package nars.experiment;

import com.gs.collections.api.tuple.Twin;
import nars.$;
import nars.NAR;
import nars.gui.BagChart;
import nars.gui.BeliefTableChart;
import nars.learn.Agent;
import nars.nal.Tense;
import nars.nar.Default;
import nars.term.Termed;
import nars.vision.NARCamera;
import nars.vision.SwingCamera;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * Created by me on 7/5/16.
 */
public class CameraTrack implements Environment {

    private final int w;
    private final int h;
    private final JPanel scene;
    private final NARCamera cam;

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
        this.cam = new NARCamera(getClass().getSimpleName(), nar, swingCam, (x, y) -> {
			return $.p($.the(x), $.the(y));
		});
        cam.input(0,0,256,256);
        swingCam.output(12,12);
        NARCamera.newWindow(cam);


        //nar.log();
        nar.onFrame(nn->{
            cam.update((x,y,t,r,g,b) -> {
                nar.believe($.inh(t.term(), $.the("r")), Tense.Present, r/256f, 0.9f);
                nar.believe($.inh(t.term(), $.the("g")), Tense.Present, g/256f, 0.9f);
                nar.believe($.inh(t.term(), $.the("b")), Tense.Present, b/256f, 0.9f);
            });
        });

        {
            BagChart.show((Default) nar);
        }

        {
            java.util.List<Termed> charted = new ArrayList();

            charted.add($.$("(#x<->r)"));
            charted.add($.$("(#x<->g)"));
            charted.add($.$("(#x<->b)"));
            charted.add($.$("((2,2)-->g)"));
//            charted.add(nar.ask($.$("(?x<->r)").term()));
//            charted.add(nar.ask($.$("(?x<->g)").term()));
//            charted.add(nar.ask($.$("(?x<->b)").term()));

            new BeliefTableChart(nar, charted).show(600, 900);
        }
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
        nar.cyclesPerFrame.set(32);
        nar.conceptActivation.setValue(0.02f);

        new CameraTrack(400, 400, 32, 32, nar);
        nar.loop(25f);

    }
}
