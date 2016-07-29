package nars.experiment.arkanoid;


import nars.$;
import nars.NAR;
import nars.Param;
import nars.experiment.NAREnvironment;
import nars.index.CaffeineIndex;
import nars.nar.Default;
import nars.nar.util.DefaultConceptBuilder;
import nars.op.ArithmeticInduction;
import nars.op.time.MySTMClustered;
import nars.term.Compound;
import nars.term.obj.Termject;
import nars.time.FrameClock;
import nars.util.Util;
import nars.util.data.random.XorShift128PlusRandom;
import nars.util.signal.MotorConcept;
import nars.util.signal.SensorConcept;
import nars.vision.SwingCamera;
import spacegraph.Surface;
import spacegraph.obj.ControlSurface;
import spacegraph.obj.MatrixView;

import java.util.Random;

import static nars.$.t;
import static nars.vision.PixelCamera.decodeRed;

public class Arkancide extends NAREnvironment {

    private static final int cyclesPerFrame = 32;
    final Arkanoid noid;
    private final SwingCamera cam;


    private MotorConcept motorLeftRight;

    final int visW = 32;
    final int visH = 32;
    final SensorConcept[][] ss;

    float noiseLevel = 0.05f;

    float speed = 20f;
    private long now;

    public class View {
        public Surface camView;
    }
    private final View view;

    public Arkancide(NAR nar) {
        super(nar);
        noid = new Arkanoid();

        ss = new SensorConcept[visW][visH];

        cam = new SwingCamera(noid, visW, visH);
        cam.update();

        view = new View();
        view.camView = new MatrixView(visW, visH, (x, y, g) -> {
//            int rgb = cam.out.getRGB(x,y);
//            float r = decodeRed(rgb);
//            if (r > 0)
//                System.out.println(x + " "+ y + " " + r);
//            g.glColor3f(r,0,0);

            SensorConcept s = ss[x][y];
            float b = s.hasBeliefs() ? s.beliefs().expectation(now) : 0;
            float d = s.hasGoals() ? s.goals().expectation(now) : 0;
            float dr, dg;
            if (d > 0.5f) {
                dr = 0; dg = d-0.5f * 2f;
            } else {
                dg = 0; dr = (1f-d)-0.5f * 2f;
            }

            float p = nar.conceptPriority(s);
            g.glColor4f(dr, dg, b, 0.8f + 0.2f * p);

        });

        ControlSurface.newControlWindow(view);


    }

    @Override
    protected void init(NAR n) {
        for (int x = 0; x < visW; x++) {
            int xx = x;
            for (int y = 0; y < visH; y++) {
                Compound squareTerm = $.p(new Termject.IntTerm(x), new Termject.IntTerm(y));
                int yy = y;
                SensorConcept sss;
                sensors.add(sss = new SensorConcept(squareTerm, nar,
                        () -> noise(decodeRed(cam.out.getRGB(xx, yy))) ,// > 0.5f ? 1 : 0,
                        (v) -> t(v, alpha)
                ));
                ss[x][y] = sss;
            }
        }

        actions.add(motorLeftRight = new MotorConcept("(leftright)", nar));

    }

    private float noise(float v) {
        if (noiseLevel > 0) {
            return Util.clamp(v + (nar.random.nextFloat() * noiseLevel));
        }
        return v;
    }

    @Override
    protected float act() {
        now = nar.time();
        cam.update();
        noid.paddle.move((motorLeftRight.goals().expectation(nar.time()) - 0.5f) * speed);
        return noid.next();
    }

    public static void main(String[] args) {
        Random rng = new XorShift128PlusRandom(1);

        Param.CONCURRENCY_DEFAULT = 2;
        //Multi nar = new Multi(3,512,
        Default nar = new Default(1024,
                4, 2, 2, rng,
                new CaffeineIndex(new DefaultConceptBuilder(rng), 5 * 10000000, false)

                , new FrameClock());
        nar.inputActivation.setValue(0.03f);
        nar.derivedActivation.setValue(0.05f);


        nar.beliefConfidence(0.9f);
        nar.goalConfidence(0.8f);
        nar.DEFAULT_BELIEF_PRIORITY = 0.35f;
        nar.DEFAULT_GOAL_PRIORITY = 0.8f;
        nar.DEFAULT_QUESTION_PRIORITY = 0.3f;
        nar.DEFAULT_QUEST_PRIORITY = 0.4f;
        nar.cyclesPerFrame.set(cyclesPerFrame);
        nar.confMin.setValue(0.06f);

//        nar.on(new TransformConcept("seq", (c) -> {
//            if (c.size() != 3)
//                return null;
//            Term X = c.term(0);
//            Term Y = c.term(1);
//
//            Integer x = intOrNull(X);
//            Integer y = intOrNull(Y);
//            Term Z = (x!=null && y!=null)? ((Math.abs(x-y) <= 1) ? $.the("TRUE") : $.the("FALSE")) : c.term(2);
//
//
//            return $.inh($.p(X, Y, Z), $.oper("seq"));
//        }));
//        nar.believe("seq(#1,#2,TRUE)");
//        nar.believe("seq(#1,#2,FALSE)");

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

        MySTMClustered stm = new MySTMClustered(nar, 256, '.', 2);
        MySTMClustered stmGoal = new MySTMClustered(nar, 256, '!', 2);

        new ArithmeticInduction(nar);



        Arkancide t = new Arkancide(nar);

        t.run(5000, 1);
    }


}