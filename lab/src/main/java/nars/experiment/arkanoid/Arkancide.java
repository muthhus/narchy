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

        senseNumberBi("x(noid,paddle)", new FloatNormalized(()->noid.paddle.x));
        senseNumberBi("x(noid,ball)", new FloatNormalized(()->noid.ball.x));
        senseNumberBi("y(noid,ball)", new FloatNormalized(()->noid.ball.y));
        senseNumberBi("vx(noid,ball)", new FloatNormalized(()->noid.ball.velocityX));
        senseNumberBi("vy(noid,ball)", new FloatNormalized(()->noid.ball.velocityY));

        addCamera("cam(noid)", noid, visW, visH);
        //addCameraRetina("zoom(cam(noid))", noid, visW/2, visH/2, (v) -> $.t(v, alpha));

        action(new ActionConcept(
                "dx(noid,paddle)"
                , nar, (b,d)->{

            float pct;
            if (d!=null) {
                pct = noid.paddle.moveTo(d.freq(), paddleSpeed ); //* d.conf());
            } else {
                pct = noid.paddle.x / noid.SCREEN_WIDTH; //unchanged
            }
            return $.t(pct, nar.confidenceDefault('.'));
            //return null; //$.t(0.5f, alpha);
        }));
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
        Param.DEBUG = true;

        //runRT(Arkancide::new);
        //nRT(Arkancide::new, 25, 5);
        runRT(Arkancide::new, 30, 7);
    }


}