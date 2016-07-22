package spacegraph.layout;

import spacegraph.SpaceGraph;
import spacegraph.SpaceTransform;
import spacegraph.Spatial;

import java.util.List;
import java.util.function.Consumer;

/**
 * TODO generalize to arbitrary plane sizes and orientations
 */
public class Flatten<O> implements SpaceTransform<O>, Consumer<Spatial<O>> {

    @Override
    public void update(SpaceGraph<O> g, List<Spatial<O>> verts, float dt) {
        verts.forEach(this);
    }

    @Override
    public void accept(Spatial<O> s) {
        s.move(s.x(), s.y(), 0, 0.9f);
    }
}
