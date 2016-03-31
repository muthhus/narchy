package nars.rover.run;

import com.artemis.Entity;
import nars.rover.Sim;
import nars.rover.obj.*;
import nars.rover.world.FoodSpawnWorld1;
import org.jbox2d.dynamics.World2D;

/**
 * Created by me on 3/30/16.
 */
public class DemoCellSpace {

    public static void main(String[] args) {

        final Sim sim = new Sim(new World2D());



        //new GridSpaceWorld(GridSpaceWorld.newMazePlanet());
        Grid2D g = //new Grid2D(10, 10, 5);
                Grid2D.newMazePlanet(50,30,3);
        sim.game.createEntity().edit()
            .add(g)
            .add(new DrawAbove(g)) //HACK
        ;

        Entity rover = sim.game.createEntity().edit()
                .add(new Physical(
                        AbstractPolygonBot.newDynamic(0, 0),
                        AbstractPolygonBot.newTriangle()))
                .add(new Motorized())
                .getEntity();

        //RoverWorld world = new ReactorWorld(32, 48, 32);
        new FoodSpawnWorld1(sim, 128, 48, 48, 0.5f);

        sim.run(25);

    }
}
