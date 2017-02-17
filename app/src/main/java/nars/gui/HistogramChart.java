package nars.gui;

import com.jogamp.opengl.GL2;
import jcog.Util;
import nars.NAR;
import nars.concept.Concept;
import nars.budget.BLink;
import nars.nar.Default;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import spacegraph.Surface;
import spacegraph.math.Color3f;
import spacegraph.render.Draw;

import java.util.function.Supplier;

/**
 * Created by me on 9/2/16.
 */
public class HistogramChart extends Surface {


    private final Supplier<double[]> data;
    private final Color3f dark, light;

    public HistogramChart(Supplier<double[]> source, Color3f dark, Color3f light) {

        this.data = source;
        this.dark = dark;
        this.light = light;

    }

    public HistogramChart(NAR nar, FloatFunction<BLink<Concept>> meter, int bins, Color3f dark, Color3f light) {

        double[] data = new double[bins];
        this.data = () -> data;
        this.dark = dark;
        this.light = light;

        nar.onCycle(nn -> {
            nn.conceptsActive().forEach(c -> {
                float p = meter.floatValueOf(c);
                int b = Util.bin(p, bins - 1);
                data[b]++;
            });

            double total = 0;
            for (double e : data) {
                total += e;
            }
            if (total > 0) {
                for (int i = 0; i < bins; i++)
                    data[i] /= total;
            }

                    //priHistogram(data);
        });

    }

    @Override
    protected void paint(GL2 gl) {

        double[] data = this.data.get();

        int N = data.length;
        float dx = 1f / N;
        double max = data[Util.argmax(data)];

        float x = 0;

        float ra = dark.x;
        float ga = dark.y;
        float ba = dark.z;
        float rb = light.x;
        float gb = light.y;
        float bb = light.z;

        for (int i = 0; i < N; i++) {

            float v = (float) (data[i] / max);

            gl.glColor3f(Util.lerp(v, rb, ra), Util.lerp(v, gb, ga), Util.lerp(v, bb, ba));

            Draw.rect(gl, x, 0, dx, v);

            x += dx;
        }

    }
}

