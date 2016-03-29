package nars.rover.physics;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.World2D;

public class DefaultWorldCreator implements WorldCreator {

    @Override
    public World2D createWorld(Vec2 gravity) {
        return new World2D(gravity);
    }
}
