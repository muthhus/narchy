package spacegraph;

/**
 * somethign which can be activated and de-activated
 */
public interface Active {

    void reactivate(boolean b);

    boolean active();

    default void stopIfInactive() {
        if (!active())
            stop();
    }

    default void stop() {

    }

}
