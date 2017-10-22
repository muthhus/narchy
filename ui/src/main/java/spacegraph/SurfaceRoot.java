package spacegraph;

import jcog.event.On;

import java.util.function.Consumer;

public interface SurfaceRoot {

    default SurfaceRoot root() {
        return this;
    }

    Ortho move(float x, float y);

    Ortho scale(float s);

    Ortho scale(float sx, float sy);

    void zoom(float x, float y, float sx, float sy);

    /** receives notifications, logs, etc */
    On onLog(Consumer o);
}
