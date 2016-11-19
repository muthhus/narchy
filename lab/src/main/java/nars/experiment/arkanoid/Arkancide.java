package nars.experiment.arkanoid;


import nars.$;
import nars.NAR;
import nars.Param;
import nars.concept.ActionConcept;
import nars.remote.NAgents;

import static nars.$.t;

public class Arkancide extends NAgents {


//    final int visW = 48;
//    final int visH = 24;
    final int visW = 32;
    final int visH = 16;


    float paddleSpeed = 40f;


    final Arkanoid noid;

    private float prevScore;


    public Arkancide(NAR nar) {
        super(nar, 1 );

        noid = new Arkanoid();

        senseNumberBi("noid(paddle,x)", ()->noid.paddle.x);
        senseNumberBi("noid(ball,x)", ()->noid.ball.x);
        senseNumberBi("noid(ball,y)", ()->noid.ball.y);
        senseNumberBi("noid(ball,vx)", ()->noid.ball.velocityX);
        senseNumberBi("noid(ball,vy)", ()->noid.ball.velocityY);

        addCamera("noid", noid, visW, visH);
        //addCameraRetina("noid", noid, visW/2, visH/2, (v) -> t(v, alpha));

        action(new ActionConcept(
                //"happy:noid(paddle,x)"
                "noid(leftright)"
                , nar, (b,d)->{
            if (d!=null) {
                float pct = noid.paddle.moveTo(d.freq(), paddleSpeed ); //* d.conf());
                return $.t(pct, gamma);
            }
            return null; //$.t(0.5f, alpha);
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
        //if (reward == 0)
            //return Float.NaN;
        return reward;
    }

    public static void main(String[] args) {
        Param.DEBUG = true;

        //runRT(Arkancide::new);
        //nRT(Arkancide::new, 25, 5);
        run(Arkancide::new, 500);
    }


}