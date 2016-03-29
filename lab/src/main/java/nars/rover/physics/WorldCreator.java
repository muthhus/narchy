package nars.rover.physics;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.World2D;

@FunctionalInterface
public interface WorldCreator {
  World2D createWorld(Vec2 gravity);
}
