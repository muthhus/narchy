package nars.experiment.arkanoid;


import nars.$;
import nars.NAR;
import nars.concept.ActionConcept;
import nars.remote.NAgents;

public class Arkancide extends NAgents {


    final int visW = 64;
    final int visH = 32;


    float paddleSpeed = 25f;


    final Arkanoid noid;

    private float prevScore;


    public Arkancide(NAR nar) {
        super(nar, 2 );

        noid = new Arkanoid();

        senseNumber("noid", noid, "paddle.x");
        senseNumber("noid", noid, "ball.x");
        senseNumber("noid", noid, "ball.y");
        senseNumber("noid", noid, "ball.velocityX");
        senseNumber("noid", noid, "ball.velocityY");

        addCamera("noid", noid, visW, visH);

        action(new ActionConcept(
                //"happy:noid(paddle,x)"
                "(leftright)"
                , nar, (b,d)->{
            if (d!=null) {
                //TODO add limits for feedback, dont just return the value
                //do this with a re-usable feedback interface because this kind of acton -> limitation detection will be common
                float pct = noid.paddle.move((d.freq() - 0.5f) * paddleSpeed);
                if (pct > 0)
                    return $.t(d.freq(), alpha*pct);
                    //return $.t(Util.lerp(d.freq(), 0.5f, pct), alpha);

            }
            return $.t(0.5f, alpha);
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
        runRT(Arkancide::new);
    }


}