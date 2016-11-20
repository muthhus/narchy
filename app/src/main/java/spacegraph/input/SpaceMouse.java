package spacegraph.input;

import com.jogamp.newt.event.MouseAdapter;
import spacegraph.render.JoglPhysics;

/**
 * Created by me on 11/20/16.
 */
public abstract class SpaceMouse extends MouseAdapter {

    public final JoglPhysics space;

    public SpaceMouse(JoglPhysics g) {
        this.space = g;
    }
}
