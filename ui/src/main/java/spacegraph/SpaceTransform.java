package spacegraph;

/**
 * Created by me on 6/21/16.
 */
public interface SpaceTransform<X> {

    void update(Iterable<Spatial<X>> g, float dt);

}
