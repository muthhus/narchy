package spacegraph;

import com.jogamp.opengl.GL2;
import jcog.event.On;
import org.jetbrains.annotations.Nullable;

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

    GL2 gl();

    /**
     * singleton table.
     * can provide special handling for lifecycle states of stored entries
     * by providing a callback which will be invoked when the value is replaced.
     *
     * if 'added' == null, it will attempt to remove any set value.
     */
    void the(String key, @Nullable Object added, @Nullable Runnable onRemove);

}
