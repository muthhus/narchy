package spacegraph.space;


import jcog.pri.Pri;
import jcog.pri.PriReference;
import spacegraph.SimpleSpatial;

/**
 * Drawn edge, lightweight
 */
public class EDraw<X,Y extends SimpleSpatial<X>> extends Pri implements PriReference {

    public final Y target;
    public float width, r, g, b, a;

    /** additional attraction force multiplier */
    public float attraction = 1f;

    /** proportional to radius */
    public float attractionDist = 1f;

    public EDraw(Y target) {
        this.target = target;
    }

    @Override
    public Object get() {
        return this;
    }

    //abstract public void update(BLink ff);

}
