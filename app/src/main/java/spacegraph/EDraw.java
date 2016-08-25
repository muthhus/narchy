package spacegraph;

/**
 * Drawn edge, lightweight
 */
public final class EDraw {
    public SimpleSpatial target;
    public float width, r, g, b, a;

    /** additional attraction force multiplier */
    public float attraction = 1f;

    public void set(SimpleSpatial target, float width, float r, float g, float b, float a) {
        this.target = target;
        this.width = width;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public void clear() {
        target = null;
    }
}
