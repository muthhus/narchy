package spacegraph;

/**
 * Drawn edge, lightweight
 */
public final class EDraw {
    public Spatial target;
    public float width, r, g, b, a;

    public void set(Spatial x, float width, float r, float g, float b, float a) {
        this.target = x;
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
