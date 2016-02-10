package nars.rover.run;

import nars.Global;
import nars.rover.RoverWorld;
import nars.rover.Sim;
import nars.rover.robot.Arm;
import nars.rover.world.FoodSpawnWorld1;

import static nars.rover.run.SomeRovers.clock;

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
            public Arm arm;

            @Override
            public void init(Sim p) {
                super.init(p);
                arm = new Arm("x", sim, torso,
                        //-1.75f, 1.5f, 0.8f
                        0, 0, 0,
                        4, 6f, 1.5f
                );
                //torso.setActive(false);

            }

            float t = 0;

            @Override
            public void step(int time) {
                super.step(time);
                if (arm != null) {
                    arm.set(
                            (float)Math.PI/3f * (float)Math.sin(t) /* rotate back and forth within an arc */,
                            0.5f + 0.25f * (float)Math.sin(time/10f) /** oscillate radius */);
                    arm.step(time);
                    t += 0.002f;
                }
            }
        };
        r.setSpeed(0f, 0f); //still

        game.add(r);

//        {
//            NAR nar = new Default();
//
//            //nar.param.outputVolume.set(0);
//
//            game.add(new CarefulRover("r2", nar));
//        }


        float fps = 180;
        game.run(fps);


    }

}
