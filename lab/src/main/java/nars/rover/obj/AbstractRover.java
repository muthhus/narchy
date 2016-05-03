package nars.rover.obj;

import com.artemis.Entity;
import org.jbox2d.dynamics.Body;

/**
 * Created by me on 5/3/16.
 */
public class AbstractRover extends AbstractPolygonBot {

    public final String id;
    public final Entity entity;
    public final Health health;
    public final Motorized motor;
    public final Body torso;
    public final Turret gun;


    public AbstractRover(String id, Entity e) {
        this.id = id;
        this.entity = e;
        //material = new BeingMaterial(this);
        torso = e.getComponent(Physical.class).body;
        motor = e.getComponent(Motorized.class);

        health = e.getComponent(Health.class);

        gun = new Turret();
    }
}
