package nars.experiment.mario;

import jcog.Util;
import nars.NAR;
import nars.NAgentX;
import nars.Narsese;
import nars.experiment.mario.sprites.Mario;
import nars.video.PixelBag;

import javax.swing.*;

import static nars.$.$;
import static nars.$.t;

public class NARio extends NAgentX {

    private final MarioComponent mario;

    public NARio(NAR nar) {
        super("nario", nar);

        //Param.ANSWER_REPORTING = false;

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
            PixelBag cc = PixelBag.of(() -> mario.image, 40, 40);
            cc.setClarity(0.5f, 1f);
            nar.onCycle(()->{
                LevelScene scene = (LevelScene) mario.scene;
               if (mario.scene instanceof LevelScene) {
                   float xCam = scene.xCam;
                   float yCam = scene.yCam;
                   Mario M = ((LevelScene) this.mario.scene).mario;
                   float x = (-160 + M.x - xCam) / 320f;
                   float y = (-120 + M.y - yCam) / 240f;
                   cc.setXRelative(x);
                   cc.setYRelative(y);
                   cc.setZoom(0.05f);
               }
               //cc.setXRelative( mario.)
            });
            senseCamera("camF", cc, (v) -> t(v, alpha())).setResolution(0.02f);
            //senseCameraRetina("camZ", ()->mario.image, 30, 18, (v) -> t(v, alpha)).setResolution(0.1f);


            senseNumberDifference($("nario(x,v)"), ()-> mario.scene instanceof LevelScene ? ((LevelScene) mario.scene).mario.x : 0);
            senseNumberDifference($("nario(y,v)"), ()-> mario.scene instanceof LevelScene ? ((LevelScene) mario.scene).mario.y : 0);

            actionTriState($("nario(x)"), i -> {
               boolean n, p;
               switch(i){
                   case -1: p = false; n = true; break;
                   case +1: p = true;  n = false; break;
                   case  0: p = false; n = false; break;
                   default:
                       throw new RuntimeException();
               }
                mario.scene.toggleKey(Mario.KEY_LEFT, n);
                mario.scene.toggleKey(Mario.KEY_RIGHT, p);
            });
            actionTriState($("nario(y)"), i -> {
                boolean n, p;
                switch(i){
                    case -1: p = false; n = true; break;
                    case +1: p = true;  n = false; break;
                    case  0: p = false; n = false; break;
                    default:
                        throw new RuntimeException();
                }
                mario.scene.toggleKey(Mario.KEY_DOWN, n);
                mario.scene.toggleKey(Mario.KEY_UP, p);
                mario.scene.toggleKey(Mario.KEY_JUMP, p);
            });

            //actionToggle($("nario(jump)"), (b)->mario.scene.toggleKey(Mario.KEY_JUMP, b));
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
        float r = 0f + Util.clamp(reward, -1, +1);
//        if (r == 0)
//            return Float.NaN;
        return r;// + (float)Math.random()*0.1f;
    }

    public static void main(String[] args) {


        NAR nar = runRT((NAR n) -> {

            return new NARio(n);

        }, 24, 8, -1);


//        ArrayList<PLink<Concept>> x = Lists.newArrayList(nar.conceptsActive());
//        x.sort((a,b)->{
//            int z = Float.compare(a.pri(), b.pri());
//            if (z == 0)
//                return Integer.compare(a.get().hashCode(), b.get().hashCode());
//            return z;
//        });
//        for (PLink y : x)
//            System.out.println(y);

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