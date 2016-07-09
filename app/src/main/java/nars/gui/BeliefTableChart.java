package nars.gui;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import nars.Global;
import nars.NAR;
import nars.concept.Concept;
import nars.concept.table.BeliefTable;
import nars.term.Termed;
import nars.truth.Truth;
import nars.truth.TruthWave;
import spacegraph.render.JoglSpace2D;
import spacegraph.render.ShapeDrawer;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import static java.lang.Math.PI;
import static nars.nal.Tense.ETERNAL;


public class BeliefTableChart extends JoglSpace2D {

    public static final float baseTaskSize = 3f;
    final List<? extends Termed> terms;
    final List<TruthWave> beliefs;
    final List<TruthWave> beliefProj;
    final List<TruthWave> goals;
    final List<TruthWave> goalProj;

    final AtomicBoolean redraw;


    private final NAR nar;
    private long now;

    float angleSpeed = 0.5f;

    private BiFunction<Long, long[], long[]> rangeControl = (now, range) -> range; //default: no change

    /** the last resolved concept for the specified terms being charted */
    private Concept[] concepts;

    /** draw additional projection wave to show truthpolation values for a set of evenly spaced points on the visible range */
    private boolean drawProjections = false;


    public BeliefTableChart(NAR n, Concept terms) {
        this(n, Collections.singletonList(terms));
    }

    public BeliefTableChart(NAR n, List<? extends Termed> terms) {
        super();
        this.terms = terms;
        concepts = new Concept[terms.size()];
        this.nar = n;

        redraw = new AtomicBoolean(false);

        beliefs = Global.newArrayList();
        beliefProj = Global.newArrayList();
        goals = Global.newArrayList();
        goalProj = Global.newArrayList();
        int numConcepts = terms.size();
        for (int i = 0; i < numConcepts; i++) {
            beliefs.add(new TruthWave(0));
            beliefProj.add(new TruthWave(0));
            goals.add(new TruthWave(0));
            goalProj.add(new TruthWave(0));
        }
        beliefTheta = new float[numConcepts];
        goalTheta = new float[numConcepts];

        //setAutoSwapBufferMode(true);

        n.onFrame(nn -> {
            update();
        });

        redraw.set(true);

    }
    public void update() {

        this.now = nar.time();
        for (int i = 0; i < this.terms.size(); i++) {
            Concept c = nar.concept(terms.get(i).term() /* lookup by term, not the termed which could be a dead instance */);
            concepts[i] = c;
            if (c != null) {
                beliefs.get(i).set(c.beliefs(), now);
                goals.get(i).set(c.goals(), now);
            } else {
                beliefs.get(i).set(BeliefTable.EMPTY, now);
                goals.get(i).set(BeliefTable.EMPTY, now);
            }
        }

        ready();

    }


    protected void draw(Termed tt, Concept cc, GL2 gl, int n, float W, float H, long minT, long maxT) {

        float gew = H;
        float geh = H;

        float tew = W-H;
        float teh = H;

        float cp = nar.conceptPriority(cc);
        gl.glColor4f(0.5f,0.5f,0.5f, 0.2f + 0.25f * cp);
        float size = (cp > 0 ? (0.003f + 0.0015f * cp) : 0.0015f) * H; //if not active then show in small, otherwise if active show larger and grow in proportion to the activity
        ShapeDrawer.renderLabel(gl, size, size, tt.toString(), W/2f, H/2f, 0);

        TruthWave beliefs = this.beliefs.get(n);
        if (!beliefs.isEmpty()) {
            renderTable(cc, n, minT, maxT, now, gl, gew, geh, tew, teh, beliefs, true);
        }

        TruthWave goals = this.goals.get(n);
        if (!goals.isEmpty()) {
            renderTable(cc, n, minT, maxT, now, gl, gew, geh, tew, teh, goals, false);
        }



        gl.glLineWidth(1f);
        gl.glColor4f(1f, 1f, 1f, 0.3f);
        ShapeDrawer.strokeRect(gl, 0, 0, gew, geh);
        ShapeDrawer.strokeRect(gl, gew, 0, tew, teh);

    }

    @Override
    public void display(GLAutoDrawable glAutoDrawable) {

        GL2 gl = (GL2)glAutoDrawable.getGL();

        if (!redraw.compareAndSet(true, false)) {
            return;
        }

        //swapBuffers();

        float W = getWidth();
        float H = getHeight();

        //clear
        clear(1f /*0.5f*/);

        int num = terms.size();
        float dy = H / num;
        gl.glPushMatrix();

        //compute bounds from combined min/max of beliefs and goals so they align correctly
        long minT = Long.MAX_VALUE;
        long maxT = Long.MIN_VALUE;


        for (int i = 0; i < num; i++) {
            TruthWave b = this.beliefs.get(i);
            if (!b.isEmpty()) {
                long start = b.start();
                if (start!=ETERNAL) {
                    minT = Math.min(start, minT);
                    maxT = Math.max(b.end(), maxT);
                }
            }
            TruthWave g = this.goals.get(i);
            if (!g.isEmpty()) {

                long start = g.start();
                if (start != ETERNAL) {
                    minT = Math.min(start, minT);
                    maxT = Math.max(g.end(), maxT);
                }

            }
        }

        long[] newRange = rangeControl.apply(now, new long[] { minT, maxT });
        minT = newRange[0];
        maxT = newRange[1];

        for (int i = num-1; i >=0; i--) {
            float my = 0f;//dy * 0.15f;
            gl.glTranslatef(0,my/2,0);
            draw(terms.get(i), concepts[i], gl, i, W, dy-my, minT, maxT);
            gl.glTranslatef(0,dy-my/2,0);
        }
        gl.glPopMatrix();

    }




//    final static ColorMatrix beliefColors = new ColorMatrix(8, 8, (f, c) ->
//            new Color(0.6f + 0.38f * c, 0.2f, 1f, 0.39f + 0.6f * c)
//    );
//    final static ColorMatrix goalColors = new ColorMatrix(8, 8, (f, c) ->
//            new Color(0.2f + 0.4f * c, 1f, 0.2f, 0.39f + 0.6f * c)
//    );

    //horizontal block
    final static TaskRenderer beliefRenderer = (ge, q, c, w, h, x, y) -> {
        ge.glColor4f(0.1f + 0.9f * c, 0.1f, 0.1f, 0.5f + 0.25f * q);
        ShapeDrawer.rect(ge, x - w / 2, y - h / 4, w, h / 2);
    };
    final static TaskRenderer beliefProjRenderer = (ge, q, c, w, h, x, y) -> {
        float a = 0.1f + 0.9f * c;
        ge.glColor4f(a *0.8f, 0.1f, a *0.5f, 0.25f + 0.25f * q);
        ShapeDrawer.rect(ge, x - w / 2, y - h / 4, w/2, h / 2);
    };
    //vertical block
    final static TaskRenderer goalRenderer = (ge, q, c, w, h, x, y) -> {
        ge.glColor4f(0.1f, 0.1f + 0.9f * c, 0.1f, 0.5f + 0.25f * q);
        ShapeDrawer.rect(ge, x - w / 4, y - h / 2, w / 2, h);
    };
    final static TaskRenderer goalProjRenderer = (ge, q, c, w, h, x, y) -> {
        float a = 0.1f + 0.9f * c;
        ge.glColor4f(0.1f, a *0.8f, a * 0.5f, 0.25f + 0.25f * q);
        ShapeDrawer.rect(ge, x - w / 4, y - h / 2, w / 2, h/2);
    };

    float[] beliefTheta, goalTheta;

    public void drawCrossHair(GL2 gl, float gew, float geh, Truth truth, double theta) {
        float w = 2;
        gl.glLineWidth(w);

        float conf = truth.conf();


        float bcx = eternalX(gew, padding, w, conf);
        float bcy = yPos(truth.freq(), geh, padding, w);

        //ge.strokeLine(bcx, border, bcx, geh - border);
        //ge.strokeLine(border, bcy, gew - border, bcy);
        double r = gew * (0.25 + (0.25 * conf));

        double dx0 = Math.cos(theta) * r;
        double dy0 = Math.sin(theta) * r;
        ShapeDrawer.line(gl, dx0+bcx, dy0+bcy, -dx0+bcx, -dy0+bcy);

        double hpi = PI / 2.0;
        double dx1 = Math.cos(theta + hpi) * r;
        double dy1 = Math.sin(theta + hpi) * r;
        ShapeDrawer.line(gl, dx1+bcx, dy1+bcy, -dx1+bcx, -dy1+bcy);
    }

    final float padding = 4;


    @Override
    public void reshape(GLAutoDrawable ad, int i, int i1, int i2, int i3) {
        super.reshape(ad, i, i1, i2, i3);
        ready();
    }

    public void ready() {
        redraw.set(true);
    }

    private void renderTable(Concept c, int n, long minT, long maxT, long now, GL2 gl, float gew, float geh, float tew, float teh, TruthWave wave, boolean beliefOrGoal) {

        if (c == null)
            return;

        //Present axis line
        if ((now <= maxT) && (now >= minT)) {
            float nowLineWidth = 3;
            float nx = xTime(tew, padding, minT, maxT, now, nowLineWidth);

            gl.glColor4f(1f,1f,1f, 0.5f);
            ShapeDrawer.rect(gl, gew + nx - nowLineWidth / 2f, 0, nowLineWidth, teh);
        }

        /** drawn "pixel" dimensions*/

        renderWave(minT, maxT, gl, gew, geh, tew, teh, wave, beliefOrGoal ? beliefRenderer : goalRenderer);

        //draw projections
        if (drawProjections && minT!=maxT) {

            BeliefTable table = beliefOrGoal ? c.beliefs() : c.goals();

            int projections = 8;
            TruthWave pwave = beliefProj.get(n);
            pwave.setProjected(table, minT, maxT, projections);
            renderWave(minT, maxT, gl, gew, geh, tew, teh, pwave, beliefOrGoal ? beliefProjRenderer : goalProjRenderer);
        }

        Truth bc = wave.current;
        if (bc!=null) {
            float[] theta;
            if (beliefOrGoal) {
                theta = beliefTheta;
                gl.glColor4f(1f,0f,0,0.2f + 0.8f * bc.conf());
            } else {
                theta = goalTheta;
                gl.glColor4f(0f,1f,0,0.2f + 0.8f * bc.conf());
            }
            theta[n] += bc.motivation() * angleSpeed;
            drawCrossHair(gl, gew, geh, bc, theta[n]);
        }

    }

    private void renderWave(long minT, long maxT, GL2 gl, float gew, float geh, float tew, float teh, TruthWave wave, TaskRenderer r) {
        wave.forEach((freq, conf, o, qua) -> {

            boolean eternal = (o!=o);
            float eh, x;
            float padding = this.padding;
            float pw = baseTaskSize + gew/(1f/conf)/4f;//10 + 10 * conf;
            float ph = baseTaskSize + geh/(1f/conf)/4f;//10 + 10 * conf;

            if (eternal) {
                eh = geh;

                x = eternalX(gew, padding, pw, conf);
                //g = ge;
            } else {
                eh = teh;
                x = xTime(tew, padding, minT, maxT, o, pw);
                //g = te;
            }
            float y = yPos(freq, eh, padding, ph);
            if (!eternal)
                x += gew + padding;

            r.renderTask(gl, qua, conf, pw, ph, x, y);
        });
    }

    private static float yPos(float f, float eh, float b /* margin */, float dh /* drawn object height, padding */) {
        return b + (eh - b - dh) * (f);
    }

    private static float eternalX(float width, float b, float w, float cc) {
        return b + (width - b - w) * cc;
    }

    public BeliefTableChart time(BiFunction<Long,long[],long[]> rangeControl) {
        this.rangeControl = rangeControl;
        return this;
    }

    public BeliefTableChart timeRadius(long nowRadius) {
        this.time((now, range) -> {
            long low = range[0];
            long high = range[1];

            if (now - low > nowRadius)
                low  = now-nowRadius;
            if (high - now > nowRadius)
                high = now + nowRadius;
            return new long[]{low,high};
        });
        return this;
    }

    @FunctionalInterface
    interface TaskRenderer {
        void renderTask(GL2 gl, float qua, float c, float w, float h, float x, float y);
    }


    private static float xTime(float tew, float b, float minT, float maxT, float o, float w) {
        float p = minT == maxT ? 0.5f : (o - minT) / (maxT - minT);
        return b + p * (tew - b - w);
    }



}
