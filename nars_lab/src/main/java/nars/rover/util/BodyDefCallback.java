package nars.rover.util;

import org.jbox2d.dynamics.BodyDef;

/**
 * Created by me on 7/19/15.
 */
@FunctionalInterface
public interface BodyDefCallback {
    BodyDef bodyDefCallback(BodyDef body);
}
