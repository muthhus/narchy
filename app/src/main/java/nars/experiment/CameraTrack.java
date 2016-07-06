package nars.experiment;

import com.gs.collections.api.tuple.Twin;
import nars.$;
import nars.NAR;
import nars.learn.Agent;
import nars.nar.Default;
import nars.vision.NARCamera;
import nars.vision.SwingCamera;

import javax.swing.*;
import java.awt.*;

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
                circle(g, sw / 6, sh / 6, sw/6);
                circle(g, getWidth() - sw / 6, sh / 6, sw/6);
                circle(g, sw / 6, getHeight() - sh / 6, sw/6);
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
        swingCam.output(32,32);
        NARCamera.newWindow(cam);


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

        new CameraTrack(400, 400, 32, 32, nar);
        nar.loop(15f);
    }
}
