package nars.gui;

import com.jogamp.opengl.GL2;
import nars.NAR;
import nars.concept.Concept;
import nars.concept.table.BeliefTable;
import nars.term.Termed;
import nars.truth.Truth;
import nars.truth.TruthWave;
import spacegraph.Surface;
import spacegraph.render.Draw;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import static java.lang.Math.PI;
import static nars.nal.Tense.ETERNAL;


public class BeliefTableChart extends Surface {

    public static final float baseTaskSize = 0.05f;
    final Termed term;
    final TruthWave beliefs;
    final TruthWave beliefProj;
    final TruthWave goals;
    final TruthWave goalProj;

    final AtomicBoolean redraw;


    private final NAR nar;
    private long now;

    float angleSpeed = 0.5f;

    private BiFunction<Long, long[], long[]> rangeControl = (now, range) -> range; //default: no change
    float beliefTheta, goalTheta;


    /**
     * draw additional projection wave to show truthpolation values for a set of evenly spaced points on the visible range
     */
    private boolean drawProjections = false;
    private boolean showEternal = false;

    long[] range;

    public BeliefTableChart(NAR n, Termed term) {
        this(n, term, null);
    }
    public BeliefTableChart(NAR n, Termed term, long[] range) {
        super();
        this.term = term;
        this.nar = n;

        this.range = range;

        redraw = new AtomicBoolean(false);

        beliefs = new TruthWave(0);
        beliefProj = new TruthWave(0);
        goals = new TruthWave(0);
        goalProj = new TruthWave(0);

        beliefTheta = goalTheta = 0;

        //setAutoSwapBufferMode(true);

        n.onFrame(nn -> {
            update();
        });

        redraw.set(true);

    }

    public void update() {

        this.now = nar.time();
        Concept c = nar.concept(term/* lookup by term, not the termed which could be a dead instance */);

        if (c != null) {
            beliefs.set(c.beliefs(), now);
            goals.set(c.goals(), now);
        } else {
            beliefs.set(BeliefTable.EMPTY, now);
            goals.set(BeliefTable.EMPTY, now);
        }

        ready();

    }


    protected void draw(Termed tt, Concept cc, GL2 gl, long minT, long maxT) {



        float cp = nar.conceptPriority(cc);
        gl.glColor4f(0.5f, 0.5f, 0.5f, 0.2f + 0.25f * cp);
        float size = (cp > 0 ? (0.0003f + 0.00015f * cp) : 0.00015f); //if not active then show in small, otherwise if active show larger and grow in proportion to the activity
        Draw.text(gl, size, size, tt.toString(), 1 / 2f, 1 / 2f, 0);

        TruthWave beliefs = this.beliefs;
        if (!beliefs.isEmpty()) {
            renderTable(cc, minT, maxT, now, gl, beliefs, true);
        }

        TruthWave goals = this.goals;
        if (!goals.isEmpty()) {
            renderTable(cc, minT, maxT, now, gl, goals, false);
        }


        //gl.glLineWidth(1f);
        //gl.glColor4f(1f, 1f, 1f, 0.3f);
        //Draw.strokeRect(gl, 0, 0, gew, geh);
        //Draw.strokeRect(gl, gew, 0, tew, teh);

    }

    @Override
    protected void paint(GL2 gl) {


        /*if (!redraw.compareAndSet(true, false)) {
            return;
        }*/

        //swapBuffers();



        //clear
        //clear(1f /*0.5f*/);


        long minT, maxT;

        if (range!=null) {
            minT = range[0];
            maxT = range[1];
        } else {

            //compute bounds from combined min/max of beliefs and goals so they align correctly
            minT = Long.MAX_VALUE;
            maxT = Long.MIN_VALUE;


            TruthWave b = this.beliefs;
            if (!b.isEmpty()) {
                long start = b.start();
                if (start != ETERNAL) {
                    minT = Math.min(start, minT);
                    maxT = Math.max(b.end(), maxT);
                }
            }
            TruthWave g = this.goals;
            if (!g.isEmpty()) {

                long start = g.start();
                if (start != ETERNAL) {
                    minT = Math.min(start, minT);
                    maxT = Math.max(g.end(), maxT);
                }


            }

            long[] newRange = rangeControl.apply(now, new long[]{minT, maxT});
            minT = newRange[0];
            maxT = newRange[1];
        }


        gl.glColor3f(0,0,0); //background
        Draw.rect(gl, 0,0, 1, 1);

        Concept cc = nar.concept(term);
        if (cc != null) {
            draw(term, cc, gl, minT, maxT);
        }

    }


//    final static ColorMatrix beliefColors = new ColorMatrix(8, 8, (f, c) ->
//            new Color(0.6f + 0.38f * c, 0.2f, 1f, 0.39f + 0.6f * c)
//    );
//    final static ColorMatrix goalColors = new ColorMatrix(8, 8, (f, c) ->
//            new Color(0.2f + 0.4f * c, 1f, 0.2f, 0.39f + 0.6f * c)
//    );

    static final float dz = 0.05f;

    //horizontal block
    final static TaskRenderer beliefRenderer = (ge, q, c, w, h, x, y) -> {
        ge.glColor4f(0.1f + 0.9f * c, 0.1f, 0.1f, 0.5f + 0.25f * q);
        Draw.rect(ge, x - w / 2, y - h / 4, w, h / 2, dz);
    };
    final static TaskRenderer beliefProjRenderer = (ge, q, c, w, h, x, y) -> {
        float a = 0.1f + 0.9f * c;
        ge.glColor4f(a * 0.8f, 0.1f, a * 0.5f, 0.25f + 0.25f * q);
        Draw.rect(ge, x - w / 2, y - h / 4, w / 2, h / 2, dz);
    };
    //vertical block
    final static TaskRenderer goalRenderer = (ge, q, c, w, h, x, y) -> {
        ge.glColor4f(0.1f, 0.1f + 0.9f * c, 0.1f, 0.5f + 0.25f * q);
        Draw.rect(ge, x - w / 4, y - h / 2, w / 2, h, dz);
    };
    final static TaskRenderer goalProjRenderer = (ge, q, c, w, h, x, y) -> {
        float a = 0.1f + 0.9f * c;
        ge.glColor4f(0.1f, a * 0.8f, a * 0.5f, 0.25f + 0.25f * q);
        Draw.rect(ge, x - w / 4, y - h / 2, w / 2, h / 2, dz);
    };


    public void drawCrossHair(GL2 gl, float gew, float geh, Truth truth, double theta) {
        float w = 3;
        gl.glLineWidth(w);

        float conf = truth.conf();


        float bcx = eternalX(gew, 0, w, conf);
        float bcy = yPos(truth.freq(), geh, w);

        //ge.strokeLine(bcx, border, bcx, geh - border);
        //ge.strokeLine(border, bcy, gew - border, bcy);
        double r = gew * (0.15 + (0.2 * conf));

        double dx0 = Math.cos(theta) * r;
        double dy0 = Math.sin(theta) * r;
        Draw.line(gl, dx0 + bcx, dy0 + bcy, -dx0 + bcx, -dy0 + bcy);

        double hpi = PI / 2.0;
        double dx1 = Math.cos(theta + hpi) * r;
        double dy1 = Math.sin(theta + hpi) * r;
        Draw.line(gl, dx1 + bcx, dy1 + bcy, -dx1 + bcx, -dy1 + bcy);
    }



    public void ready() {
        redraw.set(true);
    }

    private void renderTable(Concept c, long minT, long maxT, long now, GL2 gl, TruthWave wave, boolean beliefOrGoal) {

        if (c == null)
            return;

        //Present axis line
        if ((now <= maxT) && (now >= minT)) {
            float nowLineWidth = 0.005f;
            float nx = xTime(minT, maxT, now);

            gl.glColor4f(1f, 1f, 1f, 0.5f);
            Draw.rect(gl, nx - nowLineWidth / 2f, 0, nowLineWidth, 1);
        }

        /** drawn "pixel" dimensions*/

        renderWave(minT, maxT, gl, 1, 1, 1, wave, beliefOrGoal ? beliefRenderer : goalRenderer);

        //draw projections
        if (drawProjections && minT != maxT) {

            BeliefTable table = beliefOrGoal ? c.beliefs() : c.goals();

            int projections = 8;
            TruthWave pwave = beliefProj;
            pwave.setProjected(table, minT, maxT, projections);
            renderWave(minT, maxT, gl, 1, 1, 1, pwave, beliefOrGoal ? beliefProjRenderer : goalProjRenderer);
        }

        Truth bc = wave.current;
        if (bc != null && showEternal) {
            float theta;
            float dTheta = bc.motivation() * angleSpeed;
            if (beliefOrGoal) {
                this.beliefTheta += dTheta;
                theta = beliefTheta;
                gl.glColor4f(1f, 0f, 0, 0.2f + 0.8f * bc.conf());
            } else {
                this.goalTheta += dTheta;
                theta = goalTheta;
                gl.glColor4f(0f, 1f, 0, 0.2f + 0.8f * bc.conf());
            }
            drawCrossHair(gl, 1, 1, bc, theta);
        }

    }

    private void renderWave(long minT, long maxT, GL2 gl, float gew, float geh, float teh, TruthWave wave, TaskRenderer r) {
        wave.forEach((freq, conf, o, qua) -> {

            boolean eternal = (o != o);
            float eh, x;
            float pw = baseTaskSize;// + gew / (1f / conf) / 4f;//10 + 10 * conf;
            float ph = baseTaskSize;// + geh / (1f / conf) / 4f;//10 + 10 * conf;

            if (showEternal && eternal) {
                eh = geh;
                x = eternalX(gew, 0, pw, conf);
            } else if ((o >= minT) && (o <= maxT)) {
                eh = teh;
                x = xTime(minT, maxT, o);
            } else {
                x = eh = Float.NaN; //dont draw
            }

            if(eh==eh) {
                r.renderTask(gl, qua, conf, pw, ph, x, yPos(freq, eh, ph));
            }
        });
    }

    private static float yPos(float f, float eh,  /* margin */ float dh /* drawn object height, padding */) {
        return (eh - dh) * (f);
    }

    private static float eternalX(float width, float b, float w, float cc) {
        return b + (width - b - w) * cc;
    }

    public BeliefTableChart time(BiFunction<Long, long[], long[]> rangeControl) {
        this.rangeControl = rangeControl;
        return this;
    }

    public BeliefTableChart timeRadius(long nowRadius) {
        this.time((now, range) -> {
            long low = range[0];
            long high = range[1];

            if (now - low > nowRadius)
                low = now - nowRadius;
            if (high - now > nowRadius)
                high = now + nowRadius;
            return new long[]{low, high};
        });
        return this;
    }

    @FunctionalInterface
    interface TaskRenderer {
        void renderTask(GL2 gl, float qua, float c, float w, float h, float x, float y);
    }


    private static float xTime(float minT, float maxT, float o) {
        float p = minT == maxT ? 0.5f : (o - minT) / (maxT - minT);
        return p ;
    }


}
