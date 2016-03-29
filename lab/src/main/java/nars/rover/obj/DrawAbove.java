package nars.rover.obj;

import com.artemis.Component;
import nars.rover.physics.gl.JoglAbstractDraw;
import nars.rover.physics.j2d.LayerDraw;
import org.jbox2d.dynamics.World2D;

/**
 * Created by me on 3/29/16.
 */
public class DrawAbove extends Component {

    public LayerDraw drawer;

    public DrawAbove(LayerDraw l) {
        this.drawer = l;
    }

}
