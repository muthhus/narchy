/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package nars.rover.world;

import com.artemis.EntityEdit;
import nars.rover.Sim;
import nars.rover.obj.Edible;
import nars.rover.obj.Material;
import nars.rover.obj.MaterialColor;

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

            EntityEdit ff = newFood(sim, foodSpawnR, foodSpawnR, minSize, maxSize, mass);
            if (Math.random() < foodToPoisonRatio) {
                ff.add(new Edible(0.7f, 0f)).add(new MaterialColor(0.1f, 0.9f, 0.1f));
            } else {
                ff.add(new Edible(0f, 0.7f)).add(new MaterialColor(0.9f, 0.1f, 0.1f));
            }

        }

        float wt = 1f;
        addWall(sim, 0, h, w, wt, 0);
        addWall(sim, -w, 0, wt, h, 0);
        addWall(sim, w, 0, wt, h, 0);
        addWall(sim, 0, -h, w, wt, 0);

    }
}
