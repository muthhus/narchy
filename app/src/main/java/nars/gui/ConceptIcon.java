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

    private final Term concept;
    private Concept _concept;

    private float[] bgColor = new float[4];

    public ConceptIcon(NAR nar, Termed c) {
        super(c.toString());

        this.concept = c.term();
        update(nar.concept(this.concept), nar.time());
    }

    public void update(Concept c, long time) {
        this._concept = c;

        Concept cc = this._concept;
        if (cc == null)
            return;

        float b = 2f * (_concept.beliefFreq(time, 0.5f) - 0.5f);
        bgColor[0] = b >= 0 ? b : 0;
        bgColor[1] = b < 0 ? -b : 0;
        bgColor[2] = 0;
        bgColor[3] = 0.9f;
    }

    @Override
    public void paint(GL2 gl) {

        gl.glColor4fv(bgColor, 0);

        Draw.rect(gl, 0, 0, 1, 1);
        super.paint(gl);
    }
}
