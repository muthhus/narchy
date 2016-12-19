package nars.experiment.arkanoid;


import jcog.math.FloatNormalized;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.concept.ActionConcept;
import nars.remote.NAgents;

public class Arkancide extends NAgents {


//    final int visW = 48;
//    final int visH = 24;
    final int visW = 32;
    final int visH = 16;

    final int afterlife = 60;

    float paddleSpeed = 40f;


    final Arkanoid noid;

    private float prevScore;


    public Arkancide(NAR nar) {
        super("noid", nar);

        noid = new Arkanoid() {
            @Override
            protected void die() {
                nar.time.tick(afterlife);
                super.die();
            }
        };

        float resX = Math.max(0.01f, 1f/visW); //dont need more resolution than 1/pixel_width
        float resY = Math.max(0.01f, 1f/visH); //dont need more resolution than 1/pixel_width

        senseNumberBi( "x(paddle)", new FloatNormalized(()->noid.paddle.x)).resolution(resX);
        senseNumberBi( "x(ball)", new FloatNormalized(()->noid.ball.x)).resolution(resX);
        senseNumberBi( "y(ball)", new FloatNormalized(()->noid.ball.y)).resolution(resY);
        senseNumberBi("vx(ball)", new FloatNormalized(()->noid.ball.velocityX));
        senseNumberBi("vy(ball)", new FloatNormalized(()->noid.ball.velocityY));

        addCamera("cam", noid, visW, visH);
        //addCameraRetina("zoom(cam(noid))", noid, visW/2, visH/2, (v) -> $.t(v, alpha));


        ActionConcept a = new ActionConcept(
                "dx(paddle)"
                , nar, (b, d) -> {

            float pct;
            if (d != null) {
                pct = noid.paddle.moveTo(d.freq(), paddleSpeed); //* d.conf());
            } else {
                pct = noid.paddle.x / noid.SCREEN_WIDTH; //unchanged
            }
            return $.t(pct, nar.confidenceDefault('.'));
            //return null; //$.t(0.5f, alpha);
        });
        a.feedback.resolution(resX);
        action(a);

//        action(new ActionConcept(
//                //"happy:noid(paddle,x)"
//                "(leftright)"
//                , nar, (b,d)->{
//            if (d!=null) {
//                //TODO add limits for feedback, dont just return the value
//                //do this with a re-usable feedback interface because this kind of acton -> limitation detection will be common
//                float pct = noid.paddle.move((d.freq() - 0.5f) * paddleSpeed);
////                if (pct > 0)
////                    return $.t(d.freq(), gamma*pct);
//                    //return $.t(Util.lerp(d.freq(), 0.5f, pct), alpha);
//
//                return $.t(d.freq(), gamma);
//
//            }
//            return null; //$.t(0.5f, alpha);
//        }));


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

    public static void main(String[] args) {
        //Param.DEBUG = true;

        //runRT(Arkancide::new);
        //nRT(Arkancide::new, 25, 5);
        runRT(Arkancide::new, 50, 25);
    }


}