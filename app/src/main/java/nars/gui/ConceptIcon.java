package nars.gui;

import com.jogamp.opengl.GL2;
import nars.NAR;
import nars.concept.Concept;
import nars.term.Term;
import nars.term.Termed;
import spacegraph.obj.widget.Label;
import spacegraph.render.Draw;

/**
 * Created by me on 11/29/16.
 */
public class ConceptIcon extends Label {
    private final NAR nar;
    private final Term concept;

    public ConceptIcon(NAR nar, Termed c) {
        super(c.toString());
        this.nar = nar;
        this.concept = c.term();
    }

    @Override
    public void paint(GL2 gl) {

        Concept concept = nar.concept(this.concept);

        float b = 2f * (concept.beliefFreq(nar.time(), 0.5f) - 0.5f);
        gl.glColor3f(b >= 0 ? b : 0, b < 0 ? -b : 0, 0);

        Draw.rect(gl, 0, 0, 1, 1);
        super.paint(gl);
    }
}
