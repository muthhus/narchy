package nars.rover.util;

import org.jbox2d.dynamics.FixtureDef;

/**
 * Created by me on 7/19/15.
 */
@FunctionalInterface
public interface FixtureDefCallback {

    FixtureDef fixDefCallback(FixtureDef fixture);
}
