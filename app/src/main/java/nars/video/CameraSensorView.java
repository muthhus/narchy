package nars.video;

import com.jogamp.opengl.GL2;
import nars.NAR;
import nars.nar.Default;
import nars.truth.Truth;
import nars.concept.SensorConcept;
import spacegraph.obj.MatrixView;

/**
 * displays a CameraSensor pixel data as perceived through its concepts (belief/goal state)
 * monochrome
 */
public class CameraSensorView extends MatrixView implements MatrixView.ViewFunc {

    private final CameraSensor cam;
    private final NAR nar;
    private float maxConceptPriority;
    private long now;

    public CameraSensorView(CameraSensor cam, NAR nar) {
        super(cam.width, cam.height);
        this.cam = cam;
        this.nar = nar;
        nar.onFrame(nn -> {
            now = nn.time();
            maxConceptPriority = ((Default) nar).core.concepts.priMax(); //TODO cache this
        });
    }

    @Override
    public float update(int x, int y, GL2 g) {

        SensorConcept s = cam.ss[x][y];
        Truth b = s.hasBeliefs() ? s.beliefs().truth(now) : null;
        float bf = b != null ? b.freq() : 0.5f;
        Truth d = s.hasGoals() ? s.goals().truth(now) : null;
//        if (d == null) {
//            dr = dg = 0;
//        } else {
//            float f = d.freq();
//            float c = d.conf();
//            if (f > 0.5f) {
//                dr = 0;
//                dg = (f - 0.5f) * 2f;// * c;
//            } else {
//                dg = 0;
//                dr = (0.5f - f) * 2f;// * c;
//            }
//        }
        float dr, dg;
        if (d!=null) {
            float f = d.freq();
            float c = d.conf();
            if (f > 0.5f) {
                dr = 0;
                dg = (f - 0.5f) * 2f;// * c;
            } else {
                dg = 0;
                dr = (0.5f - f) * 2f;// * c;
            }
        } else {
            dr = dg = 0;
        }

        float p = nar.conceptPriority(s);

        //p /= maxConceptPriority;

        bf *= 0.75f;
        g.glColor4f(bf + dr * 0.25f, bf + dg * 0.25f, bf, 0.75f + 0.25f * p);

        return ((b != null ? b.conf() : 0) + (d != null ? d.conf() : 0)) / 4f;

    }
}
