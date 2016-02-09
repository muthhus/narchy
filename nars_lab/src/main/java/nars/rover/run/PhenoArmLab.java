package nars.rover.run;

import nars.Global;
import nars.rover.RoverWorld;
import nars.rover.Sim;
import nars.rover.robot.NARover;
import nars.rover.world.FoodSpawnWorld1;

import static nars.rover.run.SomeRovers.clock;
import static nars.rover.run.SomeRovers.q;

/**
 * Laboratory for experimenting with brainless bodies
 */
public class PhenoArmLab {

    public static void main(String[] args) {

        Global.DEBUG = Global.EXIT_ON_EXCEPTION = true;


        RoverWorld world = new FoodSpawnWorld1(0, 48, 48, 0.5f);

        //RoverWorld world = new GridSpaceWorld(GridSpaceWorld.newMazePlanet());
        final Sim game = new Sim(clock, world);

        QRover r = new QRover("r2") {
            @Override
            public void init(Sim p) {
                super.init(p);
                new NARover.Arm(sim, torso, -1.75f, 1.5f, 0.8f);
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
