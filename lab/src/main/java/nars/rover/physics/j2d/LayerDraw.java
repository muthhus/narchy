package nars.rover.physics.j2d;

import nars.rover.physics.gl.JoglAbstractDraw;
import org.jbox2d.dynamics.World;

/**
 * Created by me on 2/17/16.
 */
public interface LayerDraw {
    void drawGround(JoglAbstractDraw draw, World w);

    void drawSky(JoglAbstractDraw draw, World w);
}
