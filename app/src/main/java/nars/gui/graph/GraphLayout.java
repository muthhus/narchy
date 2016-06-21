package nars.gui.graph;

import java.util.List;

/**
 * Created by me on 6/21/16.
 */
public interface GraphLayout {

    void update(GraphWindow g, List<GraphWindow.VDraw> verts, float dt);

}
