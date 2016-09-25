package nars.experiment.arkanoid;


import nars.NAR;
import nars.remote.SwingAgent;
import nars.concept.ActionConcept;

import static nars.$.t;
import static spacegraph.SpaceGraph.window;
import static spacegraph.obj.GridSurface.grid;

public class Arkancide extends SwingAgent {


    final int visW = 32;
    final int visH = 18;


    float paddleSpeed = 20f;


    final Arkanoid noid;

    private float prevScore;


    public Arkancide(NAR nar) {
        super(nar, 10 /* additional decision frames */);

        noid = new Arkanoid();

//        new NObj("noid", noid, nar)
//                .read("paddle.x", "ball.x", "ball.y", "ball.velocityX", "ball.velocityY")
//                .into(this);

        addCamera("noid", noid, visW, visH);

        addAction(new ActionConcept(
                //"happy:noid(paddle,x)"
                "(leftright)"
                , nar, (b,d)->{
            if (d!=null) {
                //TODO add limits for feedback, dont just return the value
                //do this with a re-usable feedback interface because this kind of acton -> limitation detection will be common
                noid.paddle.move((d.freq() - 0.5f) * paddleSpeed);
                return d.withConf(alpha);
            } else {
                return null;
            }
        }));

//        AutoClassifier ac = new AutoClassifier($.the("row"), nar, sensors,
//                4, 8 /* states */,
//                0.05f);

    }

    @Override
    protected float reward() {
        float nextScore = noid.next();
        float reward = nextScore - prevScore;
        this.prevScore = nextScore;
        return reward;
    }

    public static void main(String[] args) {
        run(Arkancide::new, 15500);
    }


}