package nars.rover.system;

import com.artemis.Aspect;
import com.artemis.Entity;
import com.artemis.systems.EntityProcessingSystem;
import nars.rover.Sim;
import nars.rover.obj.Physical;
import org.jbox2d.dynamics.World2D;

/**
 * Created by me on 3/29/16.
 */
public class EntityPhysics extends EntityProcessingSystem {

    private final Sim sim;

    public EntityPhysics(Sim sim) {
        super(Aspect.all(Physical.class));
        this.sim = sim;
    }

    @Override
    protected void process(Entity e) {


        Physical t = e.getComponent(Physical.class);
        boolean defined = t.bodyDef != null;
        boolean created = t.body != null;
        World2D w2 = sim.world;

        if (!defined) {
            if (created) {
                w2.destroyBody(t.body);
                t.body = null;
            }
        } else {
            if (!created) {
                t.body = w2.createBody(t.bodyDef);
                t.fixture = t.body.createFixture(t.shape, t.density);
                t.body.setUserData(e);
                t.fixture.setUserData(e);
            }
        }
    }

}
