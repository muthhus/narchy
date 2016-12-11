package spacegraph;

/**
 * somethign which can be activated and de-activated
 */
public interface Active {

    void reactivate(boolean b);

    default void activate() {
        reactivate(true);
    }
    default void deactivate() {
        reactivate(false);
    }

    boolean active();
    boolean preactive();

    boolean hide();

}
