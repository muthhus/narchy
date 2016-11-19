package nars.experiment.pacman;


import nars.$;
import nars.NAR;
import nars.concept.ActionConcept;
import nars.experiment.arkanoid.Arkanoid;
import nars.remote.NAgents;

public class PacmanAgent extends NAgents {

    final int visW = 24, visH = 12;


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
        addCamera("game", game, visW, visH);
        //addCameraRetina("game", game, visW, visH);

        int speed = 10;
        actionBipolar("move(x)", (f) -> {
            return game.pacman.move(game.board, 0, Math.round(f * speed)) > 0;

        });

        actionBipolar("move(y)", (f) -> {
            return game.pacman.move(game.board, Math.round(f * speed), 0) > 0;
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
        runRT(PacmanAgent::new, 15, 5);
        //runRT(PacmanAgent::new);
    }


}