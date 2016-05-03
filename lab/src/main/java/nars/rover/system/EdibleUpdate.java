package nars.rover.system;

import com.artemis.Aspect;
import com.artemis.Entity;
import com.artemis.systems.EntityProcessingSystem;
import nars.rover.obj.Edible;
import nars.rover.obj.MaterialColor;
import org.jbox2d.common.Color3f;

/**
 * Created by me on 4/25/16.
 */
public class EdibleUpdate extends EntityProcessingSystem {


    public EdibleUpdate() {

        super(Aspect.all(Edible.class, MaterialColor.class));
    }

    @Override
    protected void process(Entity e) {

        Edible edible = e.getComponent(Edible.class);

        MaterialColor color = e.getComponent(MaterialColor.class);
        color.set(edible.poison, 0, edible.nutrients);

        edible.energize();
    }
}
