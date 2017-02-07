package nars.experiment.arkanoid;


import jcog.math.FloatPolarNormalized;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.concept.ActionConcept;
import nars.remote.NAgents;

import java.io.IOException;

public class Arkancide extends NAgents {

    static boolean cam = true;

    private float paddleSpeed = 1f;


    public static void main(String[] args) throws IOException {
        Param.DEBUG = false;

        //runRT(Arkancide::new);
        //nRT(Arkancide::new, 25, 5);

        NAR nar = runRT((NAR n) -> {

            Arkancide agent = new Arkancide(n, cam);

            return agent;

        }, 25, 5, -1);

        //System.out.println(ts.db.getInfo());

        //ts.db.close();

        //nar.beliefConfidence(0.75f);
        //nar.goalConfidence(0.75f);
    }


    final int visW = 32;
    final int visH = 16;

    //final int afterlife = 60;

    float maxPaddleSpeed;


    final Arkanoid noid;

    private float prevScore;



    public Arkancide(NAR nar, boolean cam) {
        super("noid", nar);


        noid = new Arkanoid(!cam) {
            @Override
            protected void die() {
                //nar.time.tick(afterlife); //wont quite work in realtime mode
                super.die();
            }
        };


        maxPaddleSpeed = 46 * noid.BALL_VELOCITY;

        nar.truthResolution.setValue(0.02f);

        //nar.input(new MutableTask(happy, Op.BELIEF, $.t(0.5f, 0.15f)).eternal());

        //float resX = Math.max(0.01f, 1f/visW); //dont need more resolution than 1/pixel_width
        //float resY = Math.max(0.01f, 1f/visH); //dont need more resolution than 1/pixel_width


        if (cam) {
            addCamera("cam1", noid, visW, visH);
            //addCameraRetina("cam2", noid, visW/2, visH/2, (v) -> $.t(v, alpha));
        } else {
            senseNumber( "x(paddle, noid)", new FloatPolarNormalized(()->noid.paddle.x, noid.getWidth()/2));//.resolution(resX);
            senseNumber( "x(ball, noid)", new FloatPolarNormalized(()->noid.ball.x, noid.getWidth()/2));//.resolution(resX);
            senseNumber( "y(ball, noid)", new FloatPolarNormalized(()->noid.ball.y, noid.getHeight()/2));//.resolution(resY);
            senseNumber("vx(ball, noid)", new FloatPolarNormalized(()->noid.ball.velocityX));
            senseNumber("vy(ball, noid)", new FloatPolarNormalized(()->noid.ball.velocityY));
        }

        nar.beliefConfidence(0.9f);
        nar.goalConfidence(0.9f);
        //nar.linkFeedbackRate.setValue(0.02f);
        nar.termVolumeMax.setValue(40);




        /*action(new ActionConcept( $.func("dx", "paddleNext", "noid"), nar, (b, d) -> {
            if (d!=null) {
                paddleSpeed = Util.round(d.freq(), 0.2f);
            }
            return $.t(paddleSpeed, nar.confidenceDefault('.'));
        }));*/
        action(new ActionConcept( $.func("x", "paddleNext", "noid"), nar, (b, d) -> {


            float pct;
            if (d != null) {
                pct = noid.paddle.moveTo(d.freq(), paddleSpeed * maxPaddleSpeed); //* d.conf());
            } else {
                pct = noid.paddle.x / noid.SCREEN_WIDTH; //unchanged
            }
            return $.t(pct, nar.confidenceDefault('.'));
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