package nars.rover.obj;

import com.artemis.Component;
import nars.rover.physics.j2d.LayerDraw;

/**
 * Created by me on 3/29/16.
 */
public class DrawAbove extends Component {

    public LayerDraw drawer;

    public DrawAbove(LayerDraw l) {
        this.drawer = l;
    }

}
