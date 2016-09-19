package nars.video;

import com.jogamp.opengl.GL2;
import nars.NAR;
import nars.nar.Default;
import nars.truth.Truth;
import nars.util.signal.SensorConcept;
import spacegraph.obj.MatrixView;

/**
 * displays a CameraSensor pixel data as perceived through its concepts (belief/goal state)
 */
public class CamView extends MatrixView implements MatrixView.ViewFunc {

    private final CameraSensor cam;
    private final NAR nar;
    private float maxConceptPriority;
    private long now;

    public CamView(CameraSensor cam, NAR nar) {
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
        float dr, dg;
        if (d == null) {
            dr = dg = 0;
        } else {
            float f = d.freq();
            float c = d.conf();
            if (f > 0.5f) {
                dr = 0;
                dg = (f - 0.5f) * 2f;// * c;
            } else {
                dg = 0;
                dr = (0.5f - f) * 2f;// * c;
            }
        }

        float p = nar.conceptPriority(s);
        p /= maxConceptPriority;
        g.glColor4f(dr, dg, bf, 0.75f + 0.25f * p);

        return ((b != null ? b.conf() : 0) + (d != null ? d.conf() : 0)) / 4f;

    }
}
