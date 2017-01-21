package nars.experiment.arkanoid;


import jcog.math.FloatNormalized;
import jcog.math.FloatPolarNormalized;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.concept.ActionConcept;
import nars.remote.NAgents;

public class Arkancide extends NAgents {

    public static void main(String[] args) {
        Param.DEBUG = false;

        //runRT(Arkancide::new);
        //nRT(Arkancide::new, 25, 5);

        NAR nar = runRT((NAR n)-> new Arkancide(n, false), 30, 7, -1);

        //nar.beliefConfidence(0.75f);
        //nar.goalConfidence(0.75f);
    }


    final int visW = 32;
    final int visH = 16;

    //final int afterlife = 60;

    float paddleSpeed = 40f;


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


        float resX = Math.max(0.01f, 1f/visW); //dont need more resolution than 1/pixel_width
        float resY = Math.max(0.01f, 1f/visH); //dont need more resolution than 1/pixel_width

        senseNumberBi( "x(paddle, noid)", new FloatNormalized(()->noid.paddle.x));//.resolution(resX);
        senseNumberBi( "x(ball, noid)", new FloatNormalized(()->noid.ball.x));//.resolution(resX);
        senseNumberBi( "y(ball, noid)", new FloatNormalized(()->noid.ball.y));//.resolution(resY);
        senseNumberBi("vx(ball, noid)", new FloatPolarNormalized(()->noid.ball.velocityX));
        senseNumberBi("vy(ball, noid)", new FloatPolarNormalized(()->noid.ball.velocityY));

        if (cam)
            addCamera("cam", noid, visW, visH);
        else {
            nar.beliefConfidence(0.75f);
            nar.goalConfidence(0.75f);
            nar.linkFeedbackRate.setValue(0.1f);
            nar.logBudgetMin(System.out, 0.6f);
            nar.termVolumeMax.setValue(18); //should need less complexity in non-camera mode
        }

        //addCameraRetina("zoom(cam(noid))", noid, visW/2, visH/2, (v) -> $.t(v, alpha));


        action(new ActionConcept( $.func("x", "paddleNext", "noid"), nar, (b, d) -> {


            float pct;
            if (d != null) {
                pct = noid.paddle.moveTo(d.freq(), paddleSpeed); //* d.conf());
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
        float reward = nextScore - prevScore;
        this.prevScore = nextScore;
        if (reward == 0)
            return Float.NaN;
        return reward;
    }


}