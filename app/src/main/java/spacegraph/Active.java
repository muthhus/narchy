package spacegraph;

/**
 * somethign which can be activated and de-activated
 */
public interface Active {

    void reactivate(boolean b);

    boolean active();
    boolean preactive();

    default void stopIfInactive() {
        if (!preactive())
            stop();
    }

    void stop();

}
