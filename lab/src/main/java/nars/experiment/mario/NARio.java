package nars.experiment.mario;

import jcog.Util;
import jcog.data.FloatParam;
import nars.*;
import nars.concept.SensorConcept;
import nars.experiment.mario.sprites.Mario;
import nars.video.CameraSensor;
import nars.video.PixelBag;

import javax.swing.*;

import static nars.$.$;

public class NARio extends NAgentX {

    private final MarioComponent mario;


    private final SensorConcept vx;

    public NARio(NAR nar) throws Narsese.NarseseException {
        super( nar);

        //Param.ANSWER_REPORTING = false;
        //Param.DEBUG = true;

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


        PixelBag cc = PixelBag.of(() -> mario.image, 36, 28);
        cc.addActions($.the("nario"), this, false, false, true);
        cc.actions.forEach(a -> a.resolution.setValue(0.25f));
        cc.setClarity(0.8f, 0.95f);
        CameraSensor<PixelBag> sc = senseCamera("nario" /*"(nario,local)"*/, cc);
        sc.resolution(0.03f);

        //new ShapeSensor($.the("shape"), new BufferedImageBitmap2D(()->mario.image),this);



//        try {
//            csvPriority(nar, "/tmp/x.csv");
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }

        onFrame((z)->{
        //nar.onCycle(() -> {

            Scene scene1 = mario.scene;

            if (scene1 instanceof LevelScene) {
                LevelScene scene = (LevelScene) scene1;
                float xCam = scene.xCam;
                float yCam = scene.yCam;
                Mario M = ((LevelScene) this.mario.scene).mario;
                float x = ( M.x - xCam) / 320f;
                float y = ( M.y - yCam) / 240f;
                cc.setXRelative(x);
                cc.setYRelative(y);
                //cc.setZoom(0.4f);
            }
            //cc.setXRelative( mario.)
        });

        //sc.pri(0.1f);

//        CameraSensor ccAe = senseCameraReduced($.the("narioAE"), cc, 16)
//            .resolution(0.1f);
        //ccAe.pri(0.1f);



//        //new CameraGasNet($.the("camF"), cc, this, 64);
//        senseCameraRetina("narioGlobal", ()->mario.image, 16, 16, (v) -> t(v, alpha()));//.setResolution(0.1f);
//        sc.setResolution(0.1f);

//        nar.believe("nario:{narioLocal, narioGlobal}");


        vx = senseNumberDifference($("vx"), () -> mario.scene instanceof LevelScene ? ((LevelScene) mario.scene).mario.x : 0).resolution(0.25f);
        senseNumberDifference($("vy"), () -> mario.scene instanceof LevelScene ? ((LevelScene) mario.scene).mario.y : 0).resolution(0.25f);

        actionTriState($("x"), i -> {
            boolean n, p;
            switch (i) {
                case -1:
                    p = false;
                    n = true;
                    break;
                case +1:
                    p = true;
                    n = false;
                    break;
                case 0:
                    p = false;
                    n = false;
                    break;
                default:
                    throw new RuntimeException();
            }
            mario.scene.key(Mario.KEY_LEFT, n);
            mario.scene.key(Mario.KEY_RIGHT, p);
            return true;
        });
        actionTriState($("y"), i -> {
            boolean n, p;
            switch (i) {
                case -1:
                    p = false;
                    n = true;
                    break;
                case +1:
                    p = true;
                    n = false;
                    break;
                case 0:
                    p = false;
                    n = false;
                    break;
                default:
                    throw new RuntimeException();
            }
            mario.scene.key(Mario.KEY_DOWN, n);
            //mario.scene.key(Mario.KEY_UP, p);
            mario.scene.key(Mario.KEY_JUMP, p);
            return true;
        });


        actionToggle($("speed"), (b) -> mario.scene.key(Mario.KEY_SPEED, b));


//        frame.addKeyListener(mario);
//        frame.addFocusListener(mario);
    }

    int lastCoins;

    public final FloatParam Depress = new FloatParam(0.01f, 0f, 1f);
    public final FloatParam MoveRight = new FloatParam(0.25f, 0f, 1f);
    public final FloatParam EarnCoin = new FloatParam(0.95f, 0f, 1f);

    @Override
    protected float act() {
        int coins = Mario.coins;
        float reward = (coins - lastCoins) * EarnCoin.floatValue();
        lastCoins = coins;

        float vx = this.vx.asFloat();

        if (vx > 0.6f /* in range 0..1.0 */)
            reward += MoveRight.floatValue();

        reward -= Depress.floatValue();

        float r = Util.clamp(reward, -1, +1);
//        if (r == 0)
//            return Float.NaN;
        return r;// + (float)Math.random()*0.1f;
    }

    public static void main(String[] args) {


        Param.DEBUG = false;

        NAR nar = runRT((NAR n) -> {

            n.onTask(t -> {
                if (t.isEternal() && !t.isInput()) {
                    System.err.println(t.proof());
                }
            });

            NAgentX x = null;
            try {
                x = new NARio(n);
                //x.durations.setValue(2f);
                x.trace = true;
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }

            //n.termVolumeMax.setValue(60);

//            try {
//                ImmutableTask r = (ImmutableTask) n.ask($.$("(?x ==> happy(nario))"), ETERNAL, (q, a) -> {
//                    System.err.println(a);
//                });
//                n.onCycle((nn) -> {
//                    r.budgetSafe(1f, 0.9f);
//                    nn.input(r);
//                });

//                n.onTask(tt -> {
//                   if (tt.isBelief() && tt.op() == IMPL)
//                       System.err.println("\t" + tt);
//                });

//            } catch (Narsese.NarseseException e) {
//                e.printStackTrace();
//            }

            return x;

        }, 15);



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