package nars.rover.run;

import nars.Global;
import nars.nar.Default;
import nars.rover.RoverWorld;
import nars.rover.Sim;
import nars.rover.robot.NARover;
import nars.rover.world.FoodSpawnWorld1;

import static nars.rover.run.SomeRovers.clock;
import static nars.rover.run.SomeRovers.q;

/**
 * Laboratory for experimenting with brainless bodies
 */
public class PhenoLab {

    public static void main(String[] args) {

        Global.DEBUG = Global.EXIT_ON_EXCEPTION = true;


        RoverWorld world = new FoodSpawnWorld1(0, 48, 48, 0.5f);

        //RoverWorld world = new GridSpaceWorld(GridSpaceWorld.newMazePlanet());
        final Sim game = new Sim(clock, world);


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
