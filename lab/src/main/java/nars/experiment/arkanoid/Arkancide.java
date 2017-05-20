package nars.experiment.arkanoid;


import com.google.common.collect.Lists;
import jcog.data.FloatParam;
import jcog.math.FloatPolarNormalized;
import nars.*;
import nars.concept.SensorConcept;
import nars.gui.Vis;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.video.CameraSensor;
import spacegraph.SpaceGraph;

public class Arkancide extends NAgentX {

    static boolean numeric = true;
    static boolean cam = false;

    public final FloatParam ballSpeed = new FloatParam(2f, 0.1f, 6f);
    public final FloatParam paddleSpeed = new FloatParam(2f, 0.1f, 3f);


    final int visW = 32;
    final int visH = 16;

    //final int afterlife = 60;

    float maxPaddleSpeed;


    final Arkanoid noid;

    private float prevScore;

    public static void main(String[] args) {
        Param.DEBUG = false;

        //runRT(Arkancide::new);
        //nRT(Arkancide::new, 25, 5);

        NAR nar = runRT((NAR n) -> {

            Arkancide a = null;

            try {
                a = new Arkancide(n, cam, numeric);
                a.trace = true;
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
        super((Atomic.the("noid")), nar);

        //nar.derivedEvidenceGain.setValue(1f);


        noid = new Arkanoid(!cam) {
            @Override
            protected void die() {
                //nar.time.tick(afterlife); //wont quite work in realtime mode
                super.die();
            }
        };

//        Param.DEBUG = true;
//        nar.onTask(t -> {
//            if (t instanceof DerivedTask && t.isGoal())
//                System.err.println(t.proof());
//        });

        maxPaddleSpeed = 10 * noid.BALL_VELOCITY;

        float resX = Math.max(0.01f, 0.5f / visW); //dont need more resolution than 1/pixel_width
        float resY = Math.max(0.01f, 0.5f / visH); //dont need more resolution than 1/pixel_width

        if (cam) {
            CameraSensor cc = senseCamera("noid", noid, visW, visH);

            //senseCameraRetina("noid", noid, visW/2, visH/2, (v) -> $.t(v, alpha));
            //new CameraGasNet($.the("camF"),new Scale(new SwingCamera(noid), 80, 80), this, 64);
        }
        if (numeric) {
            SensorConcept a = senseNumber("noid:px", (() -> noid.paddle.x / noid.getWidth())).resolution(resX);
            SensorConcept b = senseNumber("noid:bx", (() -> (noid.ball.x / noid.getWidth()))).resolution(resX);
            SensorConcept c = senseNumber("noid:by", (() -> 1f - (noid.ball.y / noid.getHeight()))).resolution(resY);
            SensorConcept d = senseNumber("noid:bvx", new FloatPolarNormalized(() -> noid.ball.velocityX));
            SensorConcept e = senseNumber("noid:bvy", new FloatPolarNormalized(() -> noid.ball.velocityY));

            //experimental cheat
//            nar.input("--(paddle-ball):x! :|:",
//                      "--(ball-paddle):x! :|:"
//            );

            SpaceGraph.window(Vis.beliefCharts(200,
                    Lists.newArrayList(new Term[]{a, b, c, d, e}),
                    nar), 600, 600);

        }

        /*action(new ActionConcept( $.func("dx", "paddleNext", "noid"), nar, (b, d) -> {
            if (d!=null) {
                paddleSpeed = Util.round(d.freq(), 0.2f);
            }
            return $.t(paddleSpeed, nar.confidenceDefault('.'));
        }));*/
        Compound paddleControl = $.inh(Atomic.the("pxMove"), id);
        actionBipolar(paddleControl, (v) -> {

            float dx = paddleSpeed.floatValue() * maxPaddleSpeed *
                    v;
                    //Util.sigmoidSymmetric(v, 6);

            noid.paddle.move(dx);
            //System.out.println(v + " "  + dx + " -> " + noid.paddle.x);
            return true;
        });

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
        if (reward == 0) return Float.NaN;
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

