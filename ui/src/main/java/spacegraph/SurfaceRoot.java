package spacegraph;

public interface SurfaceRoot {

    default SurfaceRoot root() {
        return this;
    }

    Ortho translate(float x, float y);

    Ortho move(float x, float y);

    Ortho scale(float s);

    Ortho scale(float sx, float sy);

    void zoom(float x, float y, float sx, float sy);

}
