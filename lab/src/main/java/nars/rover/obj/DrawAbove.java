package nars.rover.obj;

import com.artemis.Component;
import nars.rover.physics.gl.JoglAbstractDraw;
import org.jbox2d.dynamics.World2D;

/**
 * Created by me on 3/29/16.
 */
abstract public class DrawAbove extends Component {

    public abstract void drawSky(JoglAbstractDraw draw, World2D w);

}
