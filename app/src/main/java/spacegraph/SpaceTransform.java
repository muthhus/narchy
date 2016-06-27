package spacegraph;

import java.util.List;

/**
 * Created by me on 6/21/16.
 */
public interface SpaceTransform<O> {

    void update(SpaceGraph<O> g, List<Spatial<O>> verts, float dt);

}
