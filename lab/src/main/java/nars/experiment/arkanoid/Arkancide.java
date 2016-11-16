package nars.experiment.arkanoid;


import nars.$;
import nars.NAR;
import nars.concept.ActionConcept;
import nars.remote.NAgents;

import static nars.$.t;

public class Arkancide extends NAgents {


    final int visW = 48;
    final int visH = 24;


    float paddleSpeed = 150f;


    final Arkanoid noid;

    private float prevScore;


    public Arkancide(NAR nar) {
        super(nar, 4 );

        noid = new Arkanoid();

        senseNumber("(paddle,x)", ()->noid.paddle.x);
        senseNumber("(ball,x)", ()->noid.ball.x);
        senseNumber("(ball,y)", ()->noid.ball.y);
        senseNumber("(ball,vx)", ()->noid.ball.velocityX);
        senseNumber("(ball,vy)", ()->noid.ball.velocityY);

        addCamera("noid", noid, visW, visH);
        //addCameraRetina("noid", noid, visW/2, visH/2, (v) -> t(v, alpha));

        action(new ActionConcept(
                //"happy:noid(paddle,x)"
                "(leftright)"
                , nar, (b,d)->{
            if (d!=null) {
                //TODO add limits for feedback, dont just return the value
                //do this with a re-usable feedback interface because this kind of acton -> limitation detection will be common
                float pct = noid.paddle.move((d.freq() - 0.5f) * paddleSpeed);
//                if (pct > 0)
//                    return $.t(d.freq(), gamma*pct);
                    //return $.t(Util.lerp(d.freq(), 0.5f, pct), alpha);

                return $.t(d.freq(), gamma);

            }
            return null; //$.t(0.5f, alpha);
        }));


    }

    @Override
    protected float act() {
        float nextScore = noid.next();
        float reward = nextScore - prevScore;
        this.prevScore = nextScore;
        return reward;
    }

    public static void main(String[] args) {
        //runRT(Arkancide::new);
        runRT(Arkancide::new);
    }


}