package nars.gui.graph;

import java.util.List;

/**
 * Created by me on 6/21/16.
 */
public interface GraphTransform<O> {

    void update(GraphSpace<O> g, List<Atomatter<O>> verts, float dt);

}
