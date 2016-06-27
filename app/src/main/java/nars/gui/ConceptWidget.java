package nars.gui;

import com.jogamp.opengl.GL2;
import nars.term.Termed;
import spacegraph.Spatial;
import spacegraph.phys.dynamics.RigidBody;

/**
 * Created by me on 6/26/16.
 */
class ConceptWidget extends Spatial<Termed> {

    /**
     * measure of inactivity, in time units
     */
    public float lag;

    public ConceptWidget(Termed x, int maxEdges) {
        super(x, maxEdges);
    }

    @Override
    protected void renderRelative(GL2 gl, RigidBody body) {
        super.renderRelative(gl, body);

        renderLabel(gl);
    }



}
