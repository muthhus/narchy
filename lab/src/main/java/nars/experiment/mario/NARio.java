package nars.experiment.mario;

import nars.*;
import nars.experiment.mario.sprites.Mario;

import javax.swing.*;

import static nars.$.$;
import static nars.$.t;

public class NARio extends NAgentX {

    private final MarioComponent mario;

    public NARio(NAR nar) {
        super(nar);

        Param.ANSWER_REPORTING = false;

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


        try {
            senseCamera("camF", ()->mario.image, 60, 30, (v) -> t(v, alpha)).setResolution(0.1f);
            senseCameraRetina("camZ", ()->mario.image, 30, 18, (v) -> t(v, alpha)).setResolution(0.04f);


            senseNumberDifference($("nario(x,v)"), ()-> mario.scene instanceof LevelScene ? ((LevelScene)mario.scene).mario.x : 0);
            senseNumberDifference($("nario(y,v)"), ()-> mario.scene instanceof LevelScene ? ((LevelScene)mario.scene).mario.y : 0);

            actionTriState($("nario(x)"), i -> {
               switch(i){
                   case -1:
                       mario.scene.toggleKey(Mario.KEY_LEFT, true);
                       break;
                   case +1:
                       mario.scene.toggleKey(Mario.KEY_RIGHT, true);
                       break;
                   case 0:
                       mario.scene.toggleKey(Mario.KEY_LEFT, false);
                       mario.scene.toggleKey(Mario.KEY_RIGHT, false);
                       break;
               }
            });
//            actionToggle($("nario(x,n)"), (b)->mario.scene.toggleKey(Mario.KEY_LEFT, b));
//            actionToggle($("nario(x,p)"), (b)->mario.scene.toggleKey(Mario.KEY_RIGHT, b));

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

    int lastCoins = 0;

    @Override
    protected float act() {
        int coins = Mario.coins;
        float reward = coins - lastCoins;
        lastCoins = coins;
        return -0.1f + reward/4f;
    }

    public static void main(String[] args) {


        NAR nar = runRT((NAR n) -> {

            return new NARio(n);

        }, 8, 2, -1);
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