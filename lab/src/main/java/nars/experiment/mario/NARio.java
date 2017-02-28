package nars.experiment.mario;

import nars.NAR;
import nars.NAgent;
import nars.NAgents;
import nars.Narsese;
import nars.experiment.arkanoid.Arkancide;
import nars.experiment.mario.sprites.Mario;
import nars.nar.NARBuilder;
import nars.time.FrameTime;
import nars.time.RealTime;

import javax.swing.*;
import java.awt.*;

import static java.awt.event.KeyEvent.VK_LEFT;
import static nars.$.$;
import static nars.$.t;

public class NARio extends NAgents {

    private final MarioComponent mario;

    public NARio(NAR nar) {
        super(nar);

        //Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        mario = new MarioComponent(
                //screenSize.width, screenSize.height
                640, 480
        );
        JFrame frame = new JFrame("Infinite NARio");
        frame.setIgnoreRepaint(true);

        frame.setContentPane(mario);
        //frame.setUndecorated(true);
        frame.pack();
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocation(0, 0);

        //frame.setLocation((screenSize.width-frame.getWidth())/2, (screenSize.height-frame.getHeight())/2);

        frame.setVisible(true);

        mario.start();

        senseCameraRetina("camAll", ()->mario.image, 40, 30, (v) -> t(v, alpha));

        try {
            actionToggle($("nario(x,n)"), (b)->mario.scene.toggleKey(Mario.KEY_LEFT, b));
            actionToggle($("nario(x,p)"), (b)->mario.scene.toggleKey(Mario.KEY_RIGHT, b));
            actionToggle($("nario(jump)"), (b)->mario.scene.toggleKey(Mario.KEY_JUMP, b));
            actionToggle($("nario(y,n)"), (b)->mario.scene.toggleKey(Mario.KEY_DOWN, b));
            actionToggle($("nario(y,p)"), (b)->mario.scene.toggleKey(Mario.KEY_UP, b));
            actionToggle($("nario(speed)"), (b)->mario.scene.toggleKey(Mario.KEY_SPEED, b));
        } catch (Narsese.NarseseException e) {
            e.printStackTrace();
        }

//        frame.addKeyListener(mario);
//        frame.addFocusListener(mario);
    }

    @Override
    protected float act() {
        float score = Mario.coins;
        return 0;
    }

    public static void main(String[] args) {


        NAR nar = runRT((NAR n) -> {

            return new NARio(n);

        }, 40, 10, -1);
    }
}

/*
public class NARio {
    public static void main(String[] args)
    {
        //Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        MarioComponent mario = new MarioComponent(
                //screenSize.width, screenSize.height
                800, 600
        );
        JFrame frame = new JFrame("Infinite NARio");
        frame.setIgnoreRepaint(true);

        frame.setContentPane(mario);
        //frame.setUndecorated(true);
        frame.pack();
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocation(0, 0);

        //frame.setLocation((screenSize.width-frame.getWidth())/2, (screenSize.height-frame.getHeight())/2);

        frame.setVisible(true);

        mario.start();
//        frame.addKeyListener(mario);
//        frame.addFocusListener(mario);
    }
}
 */