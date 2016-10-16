package spacegraph;

import org.codehaus.commons.nullanalysis.NotNull;

/**
 * Drawn edge, lightweight
 */
public final class EDraw {
    public SimpleSpatial target;
    public float width, r, g, b, a;

    /** additional attraction force multiplier */
    public float attraction = 1f;

    public EDraw clear() {
        target = null;
        return this;
    }
}
