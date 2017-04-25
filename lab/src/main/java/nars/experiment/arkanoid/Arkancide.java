package nars.experiment.arkanoid;


import com.google.common.collect.Lists;
import jcog.data.FloatParam;
import jcog.math.FloatPolarNormalized;
import nars.*;
import nars.concept.SensorConcept;
import nars.gui.Vis;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.video.CameraSensor;
import spacegraph.SpaceGraph;

public class Arkancide extends NAgentX {

    static boolean numeric = true;
    static boolean cam = false;

    public final FloatParam ballSpeed = new FloatParam(2f, 0.1f, 6f);
    public final FloatParam paddleSpeed = new FloatParam(1f, 0.1f, 3f);


    final int visW = 40;
    final int visH = 24;

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


            } catch (Narsese.NarseseException e) {

            }

            return a;

        }, 10);


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
        super(Atomic.the("noid"), nar);

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
//            if (t instanceof DerivedTask && t.isEternal())
//                System.err.println(t.proof());
//        });

        maxPaddleSpeed = 15 * noid.BALL_VELOCITY;

        float resX = Math.max(0.01f, 1f/visW); //dont need more resolution than 1/pixel_width
        float resY = Math.max(0.01f, 1f/visH); //dont need more resolution than 1/pixel_width

        if (cam) {
            CameraSensor cc = senseCamera("noid", noid, visW, visH);

            //senseCameraRetina("noid", noid, visW/2, visH/2, (v) -> $.t(v, alpha));
            //new CameraGasNet($.the("camF"),new Scale(new SwingCamera(noid), 80, 80), this, 64);
        }
        if (numeric) {
            //nar.termVolumeMax.set(12);
            senseNumber( "x(paddle)", new FloatPolarNormalized(()->noid.paddle.x, noid.getWidth()/2));//.resolution(resX);
            SensorConcept sensorConcept1 = senseNumber("x(ball)", new FloatPolarNormalized(() -> (noid.ball.x/noid.getWidth() - 0.5f)*2f, 1));
            SensorConcept xb = sensorConcept1;
            SensorConcept sensorConcept = senseNumber( "y(ball)", new FloatPolarNormalized(()-> (noid.ball.y/noid.getHeight() - 0.5f)*2f, 1));
            SensorConcept yb = sensorConcept;
            senseNumber("vx(ball)", new FloatPolarNormalized(()->noid.ball.velocityX));
            senseNumber("vy(ball)", new FloatPolarNormalized(()->noid.ball.velocityY));

            SpaceGraph.window(Vis.beliefCharts(200,
                    Lists.newArrayList(new Term[] { xb.term(), yb.term() }),
                    nar), 600, 600);
        }

        /*action(new ActionConcept( $.func("dx", "paddleNext", "noid"), nar, (b, d) -> {
            if (d!=null) {
                paddleSpeed = Util.round(d.freq(), 0.2f);
            }
            return $.t(paddleSpeed, nar.confidenceDefault('.'));
        }));*/
        actionUnipolar($.inh(Atomic.the("paddle"), Atomic.the("nx") ), (v) -> {
            noid.paddle.moveTo(v, paddleSpeed.floatValue() * maxPaddleSpeed);
            return true;
        });
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
        float reward = Math.max(-1f, Math.min(1f,nextScore - prevScore));
        this.prevScore = nextScore;
        if (reward == 0)
            return Float.NaN;
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

