package nars.gui;

import com.google.common.collect.Lists;
import com.jogamp.opengl.GL2;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.nar.Default;
import nars.table.BeliefTable;
import nars.term.Term;
import nars.term.Terms;
import nars.time.FrameTime;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.obj.layout.Grid;
import spacegraph.render.Draw;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static nars.$.$;
import static nars.$.task;
import static spacegraph.render.Draw.push;
import static spacegraph.render.Draw.pop;

/**
 * Tool for analyzing and tuning temporal dynamics among a set of specified concepts
 */
public class TruthLab extends Grid {

    private final List<ConceptTimeline> views;
    private final NAR nar;

    private final List<Term> concepts;

    /** samples per frame */
    int samplePeriod = 1;

    long start, end;

    public TruthLab(NAR n, Term... x) {
        super(VERTICAL);
        start = n.time();
        this.nar = n;
        this.concepts = Lists.newArrayList(x);
        this.views = concepts.stream().map(ConceptTimeline::new).collect(toList());

        n.onFrame(this::update);
    }

    @Override
    public void layout() {
        super.layout();
    }

    protected void update(NAR n) {

        this.end = Math.max(this.end, n.time());

        List<Surface> cc = $.newArrayList();
        views.forEach(l -> cc.addAll( l.update(n, start, end, samplePeriod) ));
        setChildren(cc);
        layout();
    }

    static class TruthTimeline extends Surface {

        float[] labelColor = new float[4];

        float timeScale = 0.05f;

        public String label = "?";

        //sample: occ, freq, conf
        final float[] data;
        final int samples;

        public TruthTimeline(long start, long end, int samplePeriod, IntToObjectFunction<Truth> eval) {
            this.data = new float[(int)Math.ceil( ((double)(end - start))/samplePeriod) * 3];

            int i = 0;
            int samples = 0;
            for (float occ = start; occ < end; occ += samplePeriod) {

                data[i++] = occ;

                Truth t = eval.apply(Math.round(occ));
                float f;
                if (t != null)
                    f = t.freq();
                else
                    f = 0.5f;

                data[i++] = f;

                float c;
                if (t != null)
                    c = t.conf();
                else
                    c = -1;

                data[i++] = c;

                //float p = b.pri(occ);
                //etc..

                samples++;
            }
            this.samples = samples;
        }


        @Override
        protected void paint(GL2 gl) {
            float sw = 0.9f * timeScale;
            float sh = 0.9f;

            //HACK
            push(gl);
            {
                gl.glScalef(0.1f, 2f, 1f);
                gl.glColor4fv(labelColor, 0);
                Draw.text(gl, label, 0.05f, 0, 0.25f, 0, Draw.TextAlignment.Right);
            }
            pop(gl);

            for (int i = 0; i < data.length; ) {
                float occ = data[i++];
                float f = data[i++];
                float c = data[i++];
                if (c < 0)
                    continue;

                float x = occ * timeScale;
                float y = (1f - sh) / 2f;

                gl.glColor4f(1-f, f, 0, c);
                Draw.rect(gl, x, y, sw, sh);
                gl.glColor4f(0.5f, 0.5f, 0.5f, 0.5f);
                Draw.rectStroke(gl, x, y, sw, sh);
            }
        }

    }

    static class TaskTimeline extends TruthTimeline {

        public TaskTimeline(Task task, long start, long end, int samplePeriod) {
            super(start, end, samplePeriod, (w) -> $.t(task.freq(), task.conf(w)));

            this.label = task.toString();
            Draw.colorHash(Terms.atemporalize( task.term() ), labelColor);
        }
    }
    static class BeliefTableTimeline extends TruthTimeline {

        public BeliefTableTimeline(Term t, BeliefTable b, long start, long end, int samplePeriod) {
            super(start, end, samplePeriod, (w) -> b.truth(w));

            this.label = t.toString();
            Draw.colorHash(t, labelColor);
        }
    }

    public static class ConceptTimeline extends Grid {
        private final Term term;

        public ConceptTimeline(Term x) {
            super(VERTICAL);

            this.term = x;
        }


        public List<Surface> update(NAR n, long start, long end, int samplePeriod) {

            List<Surface> cc = $.newArrayList();

            Concept c = n.concept(term);
            if (c == null) {

            } else {



                cc.add(new BeliefTableTimeline(term, c.beliefs(), start, end, samplePeriod));
                c.beliefs().forEach(b -> {
                    cc.add(new TaskTimeline(b, start, end, samplePeriod));
                });
            }

            return cc;
        }

    }


    public static void main(String[] args) {
        NAR n = new Default(1000, 64, 1, 3);
        SpaceGraph.window(
                new TruthLab(n, $("(x)"), $("(y)"),
                        $("((x) && (y))"),
                        $("((x) && --(y))"),
                        $("(--(x) && (y))"),
                        $("(--(x) && --(y))")
                ),
                1200, 900);

        Param.DEBUG = true;
        ((FrameTime)n.time).setDuration(2);
        n.log()
            .inputAt(10, "(x).   :|: %1.0;0.9%")
            .inputAt(20, "(y).   :|: %1.0;0.9%")
            .inputAt(30, "--(x). :|: %1.0;0.8%")
            .inputAt(40, "--(y). :|: %1.0;0.8%")
            .run(60);

    }

}
