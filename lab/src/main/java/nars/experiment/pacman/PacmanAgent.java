package nars.experiment.pacman;


import nars.$;
import nars.NAR;
import nars.concept.ActionConcept;
import nars.experiment.arkanoid.Arkanoid;
import nars.remote.NAgents;

public class PacmanAgent extends NAgents {

    final int visW = 32, visH = 16;


    final PacmanGame game;

    private float prevScore;


    public PacmanAgent(NAR nar) {
        super(nar, 1);

        game = new PacmanGame();

//        senseNumber("(paddle,x)", ()-> game.paddle.x);
//        senseNumber("(ball,x)", ()-> game.ball.x);
//        senseNumber("(ball,y)", ()-> game.ball.y);
//        senseNumber("(ball,vx)", ()-> game.ball.velocityX);
//        senseNumber("(ball,vy)", ()-> game.ball.velocityY);
//
        addCameraRetina("game", game, visW, visH);

        float THRESH = 0.25f;
        actionBipolar("(updown)", (f) -> {
            if (!game.LEFT && !game.RIGHT) {
                game.UP = (f > THRESH);
                game.DOWN = (f < -THRESH);
                return true;
            }
            return false;
        });
        actionBipolar("(leftright)", (f) -> {
            if (!game.UP && !game.DOWN) {
                game.LEFT = (f > THRESH);
                game.RIGHT = (f < -THRESH);
                return true;
            }
            return false;
        });

        //addCameraRetina("noid", noid, visW/2, visH/2, (v) -> t(v, alpha));
//
//        action(new ActionConcept(
//                //"happy:noid(paddle,x)"
//                "(leftright)"
//                , nar, (b,d)->{
//            if (d!=null) {
//                //TODO add limits for feedback, dont just return the value
//                //do this with a re-usable feedback interface because this kind of acton -> limitation detection will be common
//                float pct = game.paddle.move((d.freq() - 0.5f) * paddleSpeed);
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
        float nextScore = game.next();
        float reward = nextScore - prevScore;
        this.prevScore = nextScore;
        return reward;
    }

    public static void main(String[] args) {
        runRT(PacmanAgent::new);
        //runRT(PacmanAgent::new);
    }


}