package spacegraph;

/**
 * Created by me on 6/21/16.
 */
public interface SpaceTransform<X> {

    void update(SpaceGraph<X> g, SpaceInput<X,?> src, float dt);

}
