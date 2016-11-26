package spacegraph.obj;

import nars.gui.ConceptWidget;
import nars.link.BLink;
import spacegraph.SimpleSpatial;

/**
 * Drawn edge, lightweight
 */
abstract public class EDraw<X,Y extends SimpleSpatial<X>> {

    public final Y target;
    public float width, r, g, b, a;

    /** additional attraction force multiplier */
    public float attraction = 1f;

    /** proportional to radius */
    public float attractionDist = 1f;

    public EDraw(Y target) {
        this.target = target;
    }

    abstract public void update(BLink<ConceptWidget.TermEdge> ff);

}
