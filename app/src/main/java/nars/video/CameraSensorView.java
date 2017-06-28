package nars.video;

import com.jogamp.opengl.GL2;
import jcog.event.On;
import nars.NAR;
import nars.NAgent;
import nars.concept.Concept;
import nars.truth.Truth;
import spacegraph.Surface;
import spacegraph.widget.meter.MatrixView;

import java.util.function.Consumer;

/**
 * displays a CameraSensor pixel data as perceived through its concepts (belief/goal state)
 * monochrome
 */
public class CameraSensorView extends MatrixView implements MatrixView.ViewFunction2D, Consumer<NAgent> {

    private final Sensor2D cam;
    private final NAR nar;
    private final On<NAgent> on;
    private float maxConceptPriority;
    private long now;
    int dur;

    public CameraSensorView(Sensor2D cam, NAgent a) {
        super(cam.width, cam.height);
        this.cam = cam;
        this.nar = a.nar;
        this.dur = nar.dur();
        on = a.onFrame(this);
    }

    @Override
    public Surface hide() {
        on.off();
        return this;
    }

    @Override
    public void accept(NAgent nn) {
        now = nar.time();
        dur = nar.dur();
        maxConceptPriority = 1;
//            nar instanceof Default ?
//                ((Default) nar).focus.active.priMax() :
//                1; //HACK TODO cache this
    }

    @Override
    public float update(int x, int y, GL2 g) {

        Concept s = cam.matrix[x][y];
        Truth b = s.beliefs().truth(now, nar);
        float bf = b != null ? b.freq() : 0.5f;
        Truth d = s.goals().truth(now, nar);
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
            //float c = d.conf();
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

        float p = 1f;//nar.pri(s);
        if (p!=p) p = 0;

        //p /= maxConceptPriority;

        float dSum = dr + dg;
        g.glColor4f(bf * 0.75f + dr * 0.25f,
                  bf * 0.75f + dg * 0.25f,
                bf - dSum * 0.5f, 0.5f + 0.5f * p);

        return 0; //((b != null ? b.conf() : 0) + (d != null ? d.conf() : 0)) / 4f;

    }
}
