/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package nars.rover.world;

import nars.rover.Sim;
import nars.rover.obj.Material;

/**
 *
 * @author me
 */
public class FoodSpawnWorld1 extends RoverWorld {
    private final float w;
    private final float h;
    private final float foodToPoisonRatio;
    private final int numFood;


    public FoodSpawnWorld1(Sim sim, int numFood, float w, float h, float foodtoPoisonRatio) {
        super(sim.world);
        this.w = w;
        this.h = h;

        this.numFood = numFood;
        this.foodToPoisonRatio = foodtoPoisonRatio;



        float foodSpawnR = w / 1.5f;
        for (int i = 0; i < numFood; i++) {
            float minSize = 0.2f;
            float maxSize = 2.5f;
            float mass = 0.3f;

            addFood(world, foodSpawnR, foodSpawnR, minSize, maxSize, mass,
                    Math.random() < foodToPoisonRatio ?
                            Material.food : Material.poison
            );
        }
        float wt = 1f;
        addWall(world, 0, h, w, wt, 0);
        addWall(world, -w, 0, wt, h, 0);
        addWall(world, w, 0, wt, h, 0);
        addWall(world, 0, -h, w, wt, 0);

    }
}
