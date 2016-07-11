package nars.rover.system;

import com.artemis.Aspect;
import com.artemis.Entity;
import com.artemis.systems.EntityProcessingSystem;
import nars.rover.obj.RunNAR;

/**
 * Created by me on 3/29/16.
 */
public class RunNARSystem extends EntityProcessingSystem {

    /**
     * Creates a new EntityProcessingSystem.
     *
     * @param aspect the aspect to match entities
     */
    public RunNARSystem() {
        super(Aspect.all(RunNAR.class));
    }

    @Override
    protected void process(Entity e) {

        e.getComponent(RunNAR.class).nar.next();

    }
}
