package jcog.pri;

/**
 * Created by me on 2/11/17.
 */
public interface PLink<X> extends Link<X>, Priority {

    /**
     * minimum difference necessary to indicate a significant modification in budget float number components
     */
    float EPSILON_DEFAULT = 0.00001f;
}
