package spacegraph.space;


import jcog.pri.PLink;
import spacegraph.SimpleSpatial;
import spacegraph.Spatial;

/**
 * Drawn edge, lightweight
 */
public class EDraw<Y> extends PLink<SimpleSpatial<Y>> {

    //TODO use pri as 'width' or 'a'
    public float width, r, g, b, a;

    /** additional attraction force multiplier */
    public float attraction = 1f;

    /** proportional to radius */
    public float attractionDist = 1f;

    public EDraw(SimpleSpatial<Y> target) {
        super(target, 0.5f);
    }

}
