package nars.rover.physics;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.World;

@FunctionalInterface
public interface WorldCreator {
  World createWorld(Vec2 gravity);
}
