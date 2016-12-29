package nars.experiment.arkanoid;


import jcog.math.FloatNormalized;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.concept.ActionConcept;
import nars.remote.NAgents;
import nars.task.MutableTask;

public class Arkancide extends NAgents {

    public static void main(String[] args) {
        Param.DEBUG = false;

        //runRT(Arkancide::new);
        //nRT(Arkancide::new, 25, 5);

        NAR nar = runRT(Arkancide::new, 30, 10, -1);

        //nar.beliefConfidence(0.75f);
        //nar.goalConfidence(0.75f);
    }


    final int visW = 32;
    final int visH = 16;

    //final int afterlife = 60;

    float paddleSpeed = 40f;


    final Arkanoid noid;

    private float prevScore;


    public Arkancide(NAR nar) {
        super("noid", nar);

        noid = new Arkanoid() {
            @Override
            protected void die() {
                //nar.time.tick(afterlife); //wont quite work in realtime mode
                super.die();
            }
        };

        float resX = Math.max(0.01f, 1f/visW); //dont need more resolution than 1/pixel_width
        float resY = Math.max(0.01f, 1f/visH); //dont need more resolution than 1/pixel_width

        senseNumber( "x(paddle)", new FloatNormalized(()->noid.paddle.x)).resolution(resX);
        senseNumber( "x(ball)", new FloatNormalized(()->noid.ball.x)).resolution(resX);
        senseNumber( "y(ball)", new FloatNormalized(()->noid.ball.y)).resolution(resY);
        senseNumber("vx(ball)", new FloatNormalized(()->noid.ball.velocityX));
        senseNumber/*Bi*/("vy(ball)", new FloatNormalized(()->noid.ball.velocityY));

        addCamera("cam", noid, visW, visH);
        //addCameraRetina("zoom(cam(noid))", noid, visW/2, visH/2, (v) -> $.t(v, alpha));


        action(new ActionConcept( "dx(paddle)" , nar, (b, d) -> {


            float pct;
            if (d != null) {
                pct = noid.paddle.moveTo(d.freq(), paddleSpeed); //* d.conf());
            } else {
                pct = noid.paddle.x / noid.SCREEN_WIDTH; //unchanged
            }
            return $.t(pct, nar.confidenceDefault('.'));
            //return null; //$.t(0.5f, alpha);
        }).feedbackResolution(resX));

        //nar.log();
//        predictors.add( (MutableTask)nar.input(
//                //"(((noid) --> #y) && (add(#x,1) <-> #y))?"
//                "((cam --> ($x,$y)) && (camdiff($x,$y) --> similaritree($x,$y))). %1.00;0.99%"
//        ).get(0) );

    }

    @Override
    protected float act() {
        float nextScore = noid.next();
        float reward = nextScore - prevScore;
        this.prevScore = nextScore;
        if (reward == 0)
            return Float.NaN;
        return reward;
    }


}