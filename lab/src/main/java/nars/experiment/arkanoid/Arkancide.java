package nars.experiment.arkanoid;


import jcog.math.FloatPolarNormalized;
import nars.*;
import nars.concept.ActionConcept;
import nars.NAgentX;

import java.io.IOException;

import static nars.Op.BELIEF;

public class Arkancide extends NAgentX {

    public static final int CYCLES_PER_FRAME = 2;
    static boolean cam = true;

    private final float paddleSpeed = 1f;


    final int visW = 40;
    final int visH = 24;

    //final int afterlife = 60;

    float maxPaddleSpeed;


    final Arkanoid noid;

    private float prevScore;

    public static void main(String[] args) throws IOException {
        Param.DEBUG = false;

        //runRT(Arkancide::new);
        //nRT(Arkancide::new, 25, 5);

        NAR nar = runRT((NAR n) -> {

            Arkancide agent = null;
            try {
                agent = new Arkancide(n, cam);
            } catch (Narsese.NarseseException e) {

            }

            return agent;

        }, 50, 5, -1);


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


    public Arkancide(NAR nar, boolean cam) throws Narsese.NarseseException {
        super("noid", nar);

        //nar.derivedEvidenceGain.setValue(1f);


        noid = new Arkanoid(!cam) {
            @Override
            protected void die() {
                //nar.time.tick(afterlife); //wont quite work in realtime mode
                super.die();
            }
        };

        //nar.linkFeedbackRate.setValue(0.02f);

        maxPaddleSpeed = 35 * Arkanoid.BALL_VELOCITY;



        //nar.truthResolution.setValue(0.02f);

        //nar.input(new TaskBuilder(happy, Op.BELIEF, $.t(0.5f, 0.15f)).eternal());

        //float resX = Math.max(0.01f, 1f/visW); //dont need more resolution than 1/pixel_width
        //float resY = Math.max(0.01f, 1f/visH); //dont need more resolution than 1/pixel_width


        if (cam) {
            senseCamera("cam1", noid, visW, visH);
            //senseCameraRetina("cam2", noid, visW/2, visH/2, (v) -> $.t(v, alpha));
        } else {
            //nar.termVolumeMax.set(12);
            senseNumber( "x(paddle(noid))", new FloatPolarNormalized(()->noid.paddle.x, noid.getWidth()/2));//.resolution(resX);
            senseNumber( "x(ball(noid))", new FloatPolarNormalized(()->noid.ball.x, noid.getWidth()/2));//.resolution(resX);
            senseNumber( "y(ball(noid))", new FloatPolarNormalized(()->noid.ball.y, noid.getHeight()/2));//.resolution(resY);
            senseNumber("vx(ball(noid))", new FloatPolarNormalized(()->noid.ball.velocityX));
            senseNumber("vy(ball(noid))", new FloatPolarNormalized(()->noid.ball.velocityY));
        }






        /*action(new ActionConcept( $.func("dx", "paddleNext", "noid"), nar, (b, d) -> {
            if (d!=null) {
                paddleSpeed = Util.round(d.freq(), 0.2f);
            }
            return $.t(paddleSpeed, nar.confidenceDefault('.'));
        }));*/
        action(new ActionConcept( $.func("nx", "paddle(noid)"), nar, (b, d) -> {


            float pct;
            if (d != null) {
                pct = noid.paddle.moveTo(d.freq(), paddleSpeed * maxPaddleSpeed); //* d.conf());
            } else {
                pct = noid.paddle.x / Arkanoid.SCREEN_WIDTH; //unchanged
            }
            return $.t(pct, nar.confidenceDefault(BELIEF));
            //return null; //$.t(0.5f, alpha);
        })/*.feedbackResolution(resX)*/);

        //nar.log();
//        predictors.add( (MutableTask)nar.input(
//                //"(((noid) --> #y) && (add(#x,1) <-> #y))?"
//                "((cam --> ($x,$y)) && (camdiff($x,$y) --> similaritree($x,$y))). %1.00;0.99%"
//        ).get(0) );

    }

    @Override
    protected float act() {
        float nextScore = noid.next();
        float reward = Math.max(-1f, Math.min(1f,nextScore - prevScore));
        this.prevScore = nextScore;
        if (reward == 0)
            return Float.NaN;
        return reward;
    }


}