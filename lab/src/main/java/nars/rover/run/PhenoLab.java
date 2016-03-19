package nars.rover.run;

import nars.Global;
import nars.rover.Sim;
import nars.rover.robot.NARover;
import nars.rover.world.FoodSpawnWorld1;
import org.jbox2d.dynamics.World;

import static nars.rover.run.SomeRovers.q;

/**
 * Laboratory for experimenting with brainless bodies
 */
public class PhenoLab {

    public static void main(String[] args) {

        Global.DEBUG = Global.EXIT_ON_EXCEPTION = true;



        //RoverWorld world = new GridSpaceWorld(GridSpaceWorld.newMazePlanet());
        final Sim game = new Sim(new World());

        new FoodSpawnWorld1(game, 0, 48, 48, 0.5f);



//        game.add(new Turret("turret"));
//
//        game.add(new Spider("spider",
//                3, 3, 0.618f, 30, 30));

        NARover r = new NARover("r1", SomeRovers.newNAR()) {
            @Override
            public void init(Sim p) {
                super.init(p);

                q(this);
            }

        };

        game.add(r);

//        {
//            NAR nar = new Default();
//
//            //nar.param.outputVolume.set(0);
//
//            game.add(new CarefulRover("r2", nar));
//        }


        float fps = 30;
        game.run(fps);


    }

}
