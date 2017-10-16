package spacegraph.space;


import jcog.pri.PLink;
import spacegraph.SimpleSpatial;

/**
 * Drawn edge, lightweight
 */
public class EDraw<Y extends SimpleSpatial<?>> extends PLink<Y> {

    //TODO use pri as 'width' or 'a'
    public float width, r, g, b, a;

    /** additional attraction force multiplier */
    public float attraction = 1f;

    /** proportional to radius */
    public float attractionDist = 1f;

    public EDraw(Y target) {
        super(target, 0);
    }

}
