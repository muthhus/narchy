package nars.experiment.arkanoid;


import nars.$;
import nars.NAR;
import nars.concept.ActionConcept;
import nars.remote.SwingAgent;
import nars.util.Util;

public class Arkancide extends SwingAgent {


    final int visW = 64;
    final int visH = 32;


    float paddleSpeed = 15f;


    final Arkanoid noid;

    private float prevScore;


    public Arkancide(NAR nar) {
        super(nar, 0 /* additional decision frames */);

        noid = new Arkanoid();

//        new NObj("noid", noid, nar)
//                .read("paddle.x", "ball.x", "ball.y", "ball.velocityX", "ball.velocityY")
//                .into(this);

//        nar.onTask(t -> {
//            if (t.isEternal()) {
//                System.err.println(t);
//                System.err.println(t.proof());
//            }
//        });

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

//        AutoClassifier ac = new AutoClassifier($.the("row"), nar, sensors,
//                4, 8 /* states */,
//                0.05f);

    }

    @Override
    protected float act() {
        float nextScore = noid.next();
        float reward = nextScore - prevScore;
        this.prevScore = nextScore;
        return reward;
    }

    public static void main(String[] args) {
        run(Arkancide::new, 15500);
    }


}