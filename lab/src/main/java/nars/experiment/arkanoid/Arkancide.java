package nars.experiment.arkanoid;


import jcog.data.FloatParam;
import nars.*;
import nars.term.atom.Atom;
import nars.video.BufferedImageBitmap2D;
import nars.video.CameraSensor;
import nars.video.Scale;
import nars.video.SwingBitmap2D;

public class Arkancide extends NAgentX {

    static boolean numeric = true;
    static boolean cam = true;

    public final FloatParam ballSpeed = new FloatParam(1.5f, 0.04f, 6f);
    //public final FloatParam paddleSpeed = new FloatParam(2f, 0.1f, 3f);


    final int visW = 24;
    final int visH = 12;

    //final int afterlife = 60;

    final float paddleSpeed;


    final Arkanoid noid;
    //private final ActionConcept left, right;

    private float prevScore;

    public static void main(String[] args) {
        Param.DEBUG = false;

        //runRT(Arkancide::new);
        //nRT(Arkancide::new, 25, 5);

        NAR nar = runRT((NAR n) -> {

            Arkancide a = null;

            try {
                //n.truthResolution.setValue(.06f);

                a = new Arkancide(n, cam, numeric);
                //a.curiosity.setValue(0.05f);

                //a.trace = true;
                //((RealTime)a.nar.time).durSeconds(0.05f);
                //a.nar.log();

            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }

            //new RLBooster(a, new HaiQAgent());


            return a;

        }, 20);


//        nar.forEachActiveConcept(c -> {
//            c.forEachTask(t -> {
//                System.out.println(t);
//            });
//        });

        //IO.saveTasksToTemporaryTextFile(nar);

        //System.out.println(ts.db.getInfo());

        //ts.db.close();

        //nar.beliefConfidence(0.75f);
        //nar.goalConfidence(0.75f);
    }


    public Arkancide(NAR nar, boolean cam, boolean numeric) throws Narsese.NarseseException {
        super("noid", nar);
        //super(nar, HaiQAgent::new);


        noid = new Arkanoid(true) {
            @Override
            protected void die() {
                //nar.time.tick(afterlife); //wont quite work in realtime mode
                super.die();
            }
        };


        paddleSpeed = 20 * noid.BALL_VELOCITY;

        float resX = 0.01f; //Math.max(0.01f, 0.5f / visW); //dont need more resolution than 1/pixel_width
        float resY = 0.01f; //Math.max(0.01f, 0.5f / visH); //dont need more resolution than 1/pixel_width

        if (cam) {

            BufferedImageBitmap2D sw = new Scale(new SwingBitmap2D(noid), visW, visH).blur();
            CameraSensor cc = senseCamera("noid", sw, visW, visH)
                    .resolution(0.05f);
//            CameraSensor ccAe = senseCameraReduced($.the("noidAE"), sw, 16)
//                    .resolution(0.25f);

            //senseCameraRetina("noid", noid, visW/2, visH/2, (v) -> $.t(v, alpha));
            //new CameraGasNet($.the("camF"),new Scale(new SwingCamera(noid), 80, 80), this, 64);
        }


        if (numeric) {
            senseNumber($.func((Atom)id,$.the("px")), (() -> noid.paddle.x / noid.getWidth())).resolution(resX);
            senseNumber($.func((Atom)id,$.the("dx")), (() -> /*Math.sqrt*/ /* sharpen */(Math.abs(noid.ball.x - noid.paddle.x) / noid.getWidth()))).resolution(resX);
            senseNumber($.func((Atom)id,$.the("b"), $.the("x")), (() -> (noid.ball.x / noid.getWidth()))).resolution(resX);
            senseNumber($.func((Atom)id,$.the("b"), $.the("y")), (() -> 1f - (noid.ball.y / noid.getHeight()))).resolution(resY);
            //SensorConcept d = senseNumber("noid:bvx", new FloatPolarNormalized(() -> noid.ball.velocityX)).resolution(0.25f);
            //SensorConcept e = senseNumber("noid:bvy", new FloatPolarNormalized(() -> noid.ball.velocityY)).resolution(0.25f);

            //experimental cheat
//            nar.input("--(paddle-ball):x! :|:",
//                      "--(ball-paddle):x! :|:"
//            );

//            SpaceGraph.window(Vis.beliefCharts(100,
//                    Lists.newArrayList(ab.term(), a.term(), b.term(), c.term()),
//                    nar), 600, 600);
//            nar.onTask(t -> {
//                if (//t instanceof DerivedTask &&
//                        t.isEternal()) {
//                        //t.isGoal()) {
//                    System.err.println(t.proof());
//                }
//            });
        }

        /*action(new ActionConcept( $.func("dx", "paddleNext", "noid"), nar, (b, d) -> {
            if (d!=null) {
                paddleSpeed = Util.round(d.freq(), 0.2f);
            }
            return $.t(paddleSpeed, nar.confidenceDefault('.'));
        }));*/

//        actionUnipolarTransfer(paddleControl, (v) -> {
//            return noid.paddle.moveTo(v, maxPaddleSpeed);
//        });
        /*actionTriState*/
//        actionBipolar($.the("x"), (s) -> {
////           switch (s) {
////               case 0:
////                   break;
////               case -1:
////               case 1:
////                    if (s > 0 && Util.equals(noid.paddle.x, noid.getWidth(), 1f))
////                        return 0f; //edge
////                    if (s < 0 && Util.equals(noid.paddle.x, 0, 1f))
////                        return 0f; //edge
//                   if (!noid.paddle.move( maxPaddleSpeed * s)) {
//                       return 0f; //against wall
//                   }
////                    break;
////           }
//                    return s;
//
//        });///.resolution(0.1f);

        Param.DEBUG = true;
        nar.onTask((t) -> {
            if (!t.isInput() && (t.isGoal() || t.isEternal())) {
                System.err.println(t.proof());
            }
        });

        actionBipolar($.the("X"), (dx) -> {
            if (noid.paddle.move(dx * paddleSpeed))
                return dx;
            else
                return Float.NaN;
        });
//        actionToggle($.p("L"), d -> {
//            if (d)
//                noid.paddle.move(-paddleSpeed);
//        });
//        actionToggle($.p("R"), d -> {
//            if (d)
//                noid.paddle.move(+paddleSpeed);
//        });

        //nar.truthResolution.setValue(0.05f);

//        Param.DEBUG = true;
//        nar.onTask(x -> {
//            if (x.isGoal()
//                    && !x.isInput() && (!(x instanceof ActionConcept.CuriosityTask))
//                    //&& x.term().equals(paddleControl)
//            ) {
//                System.err.println(x.proof());
//            }
//        });


//        actionUnipolar($.inh(Atomic.the("paddle"), Atomic.the("nx") ), (v) -> {
//            noid.paddle.moveTo(v, paddleSpeed.floatValue() * maxPaddleSpeed);
//            return true;
//        });
//        action(new ActionConcept( $.func("nx", "paddle"), nar, (b, d) -> {
//
//
//            float pct;
//            if (d != null) {
//                pct = noid.paddle.moveTo(d.freq(), paddleSpeed.floatValue() * maxPaddleSpeed);// * d.conf());
//            } else {
//                pct = noid.paddle.x / Arkanoid.SCREEN_WIDTH; //unchanged
//            }
//            return $.t(pct, nar.confidenceDefault(BELIEF));
//            //return null; //$.t(0.5f, alpha);
//        })/*.feedbackResolution(resX)*/);

        //nar.log();
//        predictors.add( (MutableTask)nar.input(
//                //"(((noid) --> #y) && (add(#x,1) <-> #y))?"
//                "((cam --> ($x,$y)) && (camdiff($x,$y) --> similaritree($x,$y))). %1.00;0.99%"
//        ).get(0) );

    }

    @Override
    protected float act() {
        noid.BALL_VELOCITY = ballSpeed.floatValue();
        float nextScore = noid.next();
        float reward = Math.max(-1f, Math.min(1f, nextScore - prevScore));
        this.prevScore = nextScore;
        //if (reward == 0) return Float.NaN;
        return reward;
    }


}

//                {
//                    Default m = new Default(256, 48, 1, 2, n.random,
//                            new CaffeineIndex(new DefaultConceptBuilder(), 2048, false, null),
//                            new RealTime.DSHalf().durSeconds(1f));
//                    float metaLearningRate = 0.9f;
//                    m.confMin.setValue(0.02f);
//                    m.goalConfidence(metaLearningRate);
//                    m.termVolumeMax.setValue(16);
//
//                    MetaAgent metaT = new MetaAgent(a, m); //init before loading from file
//                    metaT.trace = true;
//                    metaT.init();
//
//                    String META_PATH = "/tmp/meta.nal";
//                    try {
//                        m.input(new File(META_PATH));
//                    } catch (IOException e) {
//                    }
//                    getRuntime().addShutdownHook(new Thread(() -> {
//                        try {
//                            m.output(new File(META_PATH), (x) -> {
//                                if (x.isBeliefOrGoal() && !x.isDeleted() && (x.op() == IMPL || x.op() == Op.EQUI || x.op() == CONJ)) {
//                                    //Task y = x.eternalized();
//                                    //return y;
//                                    return x;
//                                }
//                                return null;
//                            });
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }));
//                n.onCycle(metaT.nar::cycle);
//                }

