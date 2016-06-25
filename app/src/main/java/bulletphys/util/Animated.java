package bulletphys.util;


public interface Animated {

    /** returns whether to continue (true), or false if the animation should be removed */
    public boolean animate(float dt);

}
