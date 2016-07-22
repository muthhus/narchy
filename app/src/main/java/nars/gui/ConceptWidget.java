package nars.gui;

import com.jogamp.opengl.GL2;
import nars.term.Termed;
import spacegraph.Spatial;

/**
 * Created by me on 6/26/16.
 */
public class ConceptWidget extends Spatial<Termed> {

    /**
     * measure of inactivity, in time units
     */
    //public float lag;

    public ConceptWidget(Termed x, int maxEdges) {
        super(x, maxEdges);
    }

    @Override
    protected void renderRelativeAspect(GL2 gl) {
        gl.glColor4f(1f, 1f, 1f, pri);
        renderLabel(gl, 0.0005f);
    }



}
