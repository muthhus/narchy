package nars.rover.system;

import com.artemis.Aspect;
import com.artemis.Entity;
import com.artemis.systems.EntityProcessingSystem;
import nars.rover.obj.VisionRay;
import org.jbox2d.dynamics.World2D;

/**
 * Created by me on 3/29/16.
 */
public class VisionRaySystem extends EntityProcessingSystem {

    private final World2D world2d;

    public VisionRaySystem(World2D world) {
        super(Aspect.all(VisionRay.class));
        this.world2d = world;
    }

    @Override
    protected void process(Entity e) {

        VisionRay v = e.getComponent(VisionRay.class);

        World2D w = this.world2d;

        for (VisionRay.RayDrawer r : v.rayDrawers)
            r.update(w);

    }

}
