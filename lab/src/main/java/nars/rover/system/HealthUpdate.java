package nars.rover.system;

import com.artemis.Aspect;
import com.artemis.Entity;
import com.artemis.systems.EntityProcessingSystem;
import nars.rover.obj.Health;


public class HealthUpdate extends EntityProcessingSystem {

    public HealthUpdate() {
        super(Aspect.one(Health.class));
    }

    @Override
    protected void process(Entity e) {
        Health health = e.getComponent(Health.class);
        health.update();
    }
}
