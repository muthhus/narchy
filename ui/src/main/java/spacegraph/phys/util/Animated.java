package spacegraph.phys.util;


public interface Animated {

    /** returns whether to continue (true), or false if the animation should be removed */
    boolean animate(float dt);

}
