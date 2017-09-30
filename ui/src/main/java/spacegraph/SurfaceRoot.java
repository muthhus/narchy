package spacegraph;

import spacegraph.math.v2;

public interface SurfaceRoot {

    default SurfaceRoot root() {
        return this;
    }

    Ortho translate(float x, float y);

    Ortho move(float x, float y);

    Ortho scale(float s);

    v2 scale();

    Ortho scale(float sx, float sy);
}
