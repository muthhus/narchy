package nars.gui;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Lists;
import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import com.jogamp.opengl.GL2;
import nars.NAR;
import nars.concept.Concept;
import nars.link.BLink;
import nars.nar.Default;
import nars.term.Termed;
import nars.util.Util;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import spacegraph.Facial;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.math.Color3f;
import spacegraph.obj.GridSurface;
import spacegraph.render.Draw;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;
import static spacegraph.layout.TreeChart.WeightedString.w;
import static spacegraph.obj.GridSurface.VERTICAL;

/**
 * Created by me on 9/2/16.
 */
public class HistogramChart extends Surface {


    private final double[] data;
    private final Color3f dark, light;

    public HistogramChart(NAR nar, FloatFunction<BLink<Concept>> meter, int bins, Color3f dark, Color3f light) {

        this.data = new double[bins];
        this.dark = dark;
        this.light = light;

        nar.onFrame(nn -> {
            ((Default) nn).core.concepts.forEach(c -> {
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

    public static void newChart(NAR nar, int bins) {
        new SpaceGraph().add(new Facial(
                new GridSurface(VERTICAL,
                    new HistogramChart(nar, c -> c.pri(), bins, new Color3f(0.5f, 0.25f, 0f), new Color3f(1f, 0.5f, 0.1f)),
                    new HistogramChart(nar, c -> {
                        return c.dur();
                    }, bins, new Color3f(0f, 0.25f, 0.5f), new Color3f(0.1f, 0.5f, 1f))
                )
            ).maximize()).show(800,600);
    }

    @Override
    protected void paint(GL2 gl) {
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

            gl.glColor3f(Util.lerp(rb, ra, v), Util.lerp(gb, ga, v), Util.lerp(bb, ba, v));

            Draw.rect(gl, x, 0, dx, v);

            x += dx;
        }

    }
}

