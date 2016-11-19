package nars.gui;

import com.jogamp.opengl.GL2;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.table.BeliefTable;
import nars.term.Termed;
import nars.truth.Truth;
import nars.truth.TruthWave;
import spacegraph.Ortho;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.obj.layout.Grid;
import spacegraph.obj.widget.Widget;
import spacegraph.render.Draw;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import static java.lang.Math.PI;
import static java.util.stream.Collectors.toList;
import static nars.time.Tense.ETERNAL;
import static spacegraph.obj.layout.Grid.VERTICAL;


public class BeliefTableChart extends Widget {

    public static final float baseTaskSize = 0.05f;
    public static final float CROSSHAIR_THICK = 3;
    final Termed term;
    final TruthWave beliefs;
    final TruthWave beliefProj;
    final TruthWave goals;
    final TruthWave goalProj;

    final AtomicBoolean redraw;


    private final NAR nar;
    private String termString;
    private long now;

    float angleSpeed = 0.5f;

    private BiFunction<Long, long[], long[]> rangeControl = (now, range) -> range; //default: no change
    float beliefTheta, goalTheta;


    /**
     * (if > 0): draw additional projection wave to show truthpolation values for a set of evenly spaced points on the visible range
     */
    int projections = 32;

    private boolean showTaskLinks = true;
    @Deprecated
    private boolean showEternal = true;

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

    public static void newBeliefChart(NAR nar, Collection<? extends Termed> terms, long window) {

        List<Surface> actionTables = beliefTableCharts(nar, terms, window);


        new SpaceGraph().add(new Ortho(new Grid(VERTICAL, actionTables)).maximize()).show(800, 600);
    }

    public static List<Surface> beliefTableCharts(NAR nar, Collection<? extends Termed> terms, long window) {
        long[] btRange = new long[2];
        nar.onFrame(nn -> {
            long now = nn.time();
            btRange[0] = now - window;
            btRange[1] = now + window;
        });
        return terms.stream().map(c -> new BeliefTableChart(nar, c, btRange)).collect(toList());
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




        TruthWave beliefs = this.beliefs;
        if (!beliefs.isEmpty()) {
            renderTable(cc, minT, maxT, now, gl, beliefs, true);
        }

        TruthWave goals = this.goals;
        if (!goals.isEmpty()) {
            renderTable(cc, minT, maxT, now, gl, goals, false);
        }

        if (showTaskLinks) {
            gl.glLineWidth(1f);
            float nowX = xTime(minT, maxT, now);
            cc.tasklinks().forEach(tl -> {
                if (tl != null) {
                    Task x = tl.get();
                    if ((x != null) && (x.isBeliefOrGoal())) {
                        long o = x.occurrence();
                        float tlx = o == ETERNAL ? nowX : xTime(minT, maxT, o);
                        if (tlx > 0 && tlx < 1) {
                            float tly = x.freq();
                            float ii = 0.3f + 0.7f * x.conf();
                            gl.glColor4f(ii / 2f, 0, ii, 0.5f + tl.pri() * 0.5f);
                            float w = 0.05f;
                            float h = 0.05f;
                            Draw.rectStroke(gl, tlx - w / 2, tly - h / 2, w, h);
                        }
                    }
                }
            });
        }

        //gl.glLineWidth(1f);
        //gl.glColor4f(1f, 1f, 1f, 0.3f);
        //Draw.strokeRect(gl, 0, 0, gew, geh);
        //Draw.strokeRect(gl, gew, 0, tew, teh);

    }

    @Override
    protected void paintComponent(GL2 gl) {

        /*if (!redraw.compareAndSet(true, false)) {
            return;
        }*/

        //swapBuffers();


        //clear
        //clear(1f /*0.5f*/);


        long minT, maxT;

        if (range != null) {
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


        gl.glColor3f(0, 0, 0); //background
        Draw.rect(gl, 0, 0, 1, 1);

        gl.glLineWidth(1f);
        gl.glColor3f(0.5f, 0.5f, 0.5f); //border
        Draw.rectStroke(gl, 0, 0, 1, 1);

        Concept cc = nar.concept(term);
        float cp;
        if (cc != null) {
            cp = nar.activation(cc);
            draw(term, cc, gl, minT, maxT);
            termString = cc.toString();
        } else {
            cp = 0;
            termString = term.toString();
        }
        gl.glColor4f(0.75f, 0.75f, 0.75f, 0.8f + 0.2f * cp);
        gl.glLineWidth(1);
        Draw.text(gl, termString, (1f/termString.length()) * (0.5f + 0.25f * cp), 1 / 2f, 1 / 2f, 0);
    }


//    final static ColorMatrix beliefColors = new ColorMatrix(8, 8, (f, c) ->
//            new Color(0.6f + 0.38f * c, 0.2f, 1f, 0.39f + 0.6f * c)
//    );
//    final static ColorMatrix goalColors = new ColorMatrix(8, 8, (f, c) ->
//            new Color(0.2f + 0.4f * c, 1f, 0.2f, 0.39f + 0.6f * c)
//    );

    static final float dz = 0.0f;




    public void drawCrossHair(GL2 gl, float x, float gew, Truth truth, double theta) {
        gl.glLineWidth(CROSSHAIR_THICK);

        float conf = truth.conf();


        float bcy = truth.freq();

        //ge.strokeLine(bcx, border, bcx, geh - border);
        //ge.strokeLine(border, bcy, gew - border, bcy);
        double r = gew * (0.5f + 0.5f * conf);


        double dx0 = Math.cos(theta) * r;
        double dy0 = Math.sin(theta) * r;
        Draw.line(gl, dx0 + x, dy0 + bcy, -dx0 + x, -dy0 + bcy);

        double hpi = PI / 2.0;
        double dx1 = Math.cos(theta + hpi) * r;
        double dy1 = Math.sin(theta + hpi) * r;
        Draw.line(gl, dx1 + x, dy1 + bcy, -dx1 + x, -dy1 + bcy);
    }


    public void ready() {
        redraw.set(true);
    }

    private void renderTable(Concept c, long minT, long maxT, long now, GL2 gl, TruthWave wave, boolean beliefOrGoal) {

        if (c == null)
            return;

        float nowX = xTime(minT, maxT, now);

        //Present axis line
        if ((now <= maxT) && (now >= minT)) {

            gl.glColor4f(1f, 1f, 1f, 0.5f);
            Draw.line(gl, nowX, 0, nowX, 1);

            //float nowLineWidth = 0.005f;
            //Draw.rect(gl, nowX - nowLineWidth / 2f, 0, nowLineWidth, 1);
        }

        /** drawn "pixel" dimensions*/

        renderWave(nowX, minT, maxT, gl, wave, beliefOrGoal);

        //draw projections
        if (projections > 0 && minT != maxT) {

            BeliefTable table = beliefOrGoal ? c.beliefs() : c.goals();

            TruthWave pwave = beliefProj;
            pwave.project(table, minT, maxT, projections);
            renderWaveLine(nowX, minT, maxT, gl, pwave, beliefOrGoal);
        }

        Truth bc = wave.current;
        if (bc != null) {
            float theta;
            float dTheta = (bc.expectation() - 0.5f) * angleSpeed;
            if (beliefOrGoal) {
                this.beliefTheta += dTheta;
                theta = beliefTheta;
                gl.glColor4f(1f, 0f, 0, 0.2f + 0.8f * bc.conf());
            } else {
                this.goalTheta += dTheta;
                theta = goalTheta;
                gl.glColor4f(0f, 1f, 0, 0.2f + 0.8f * bc.conf());
            }
            float chSize = 0.1f;
            drawCrossHair(gl, nowX, chSize, bc, theta);
        }

    }

    private void renderWave(float nowX, long minT, long maxT, GL2 gl, TruthWave wave, boolean beliefOrGoal) {
        wave.forEach((freq, conf, s, e, qua, dur) -> {

            boolean eternal = (s != s);
            float pw = baseTaskSize / 4f;// + gew / (1f / conf) / 4f;//10 + 10 * conf;
            float ph = baseTaskSize + conf * baseTaskSize;// + geh / (1f / conf) / 4f;//10 + 10 * conf;

            float start, end, startD, endD;
            if (showEternal && eternal) {
                start = end = nowX;
                startD = endD = Float.NaN;
            } else if (((e <= maxT) && (e >= minT)) || ((s >= minT) && (s <= maxT))) {
                start = xTime(minT, maxT, s);
                startD = xTime(minT, maxT, s-dur);
                end = xTime(minT, maxT, e);
                endD = xTime(minT, maxT, e+dur);
            } else {
                return;
            }



            //r.renderTask(gl, qua, conf, pw, ph, xStart, xEnd, freq);

            float alpha = 0.2f + conf * 0.5f;
            float r, g, b;
            if (beliefOrGoal) {
                r = 0.75f;
                g = 0.25f;
                b = 0;

            } else {
                r = 0;
                g = 0.75f;
                b = 0.25f;
            }




            //draw shadow to indicate duration
            if (!eternal) {
                float mid = (endD + startD) / 2f;
                float W = Math.max((endD - startD), pw);
                float x = mid - W / 2;
                float phh = ph / 2f;
                float y = freq - phh / 2;
                gl.glColor4f(r, g, b, alpha/2f); //, 0.7f + 0.2f * q);
                Draw.rect(gl,
                        x, y,
                        W, phh);
            }

            {
                float mid = (end + start) / 2f;
                float W = Math.max((end - start), pw);
                float x = mid - W / 2;
                float y = freq - ph / 2;
                gl.glColor4f(r, g, b, alpha); //, 0.7f + 0.2f * q);
                Draw.rect(gl,
                        x, y,
                        W, ph);
            }


        });
    }

    private void renderWaveLine(float nowX, long minT, long maxT, GL2 gl, TruthWave wave, boolean beliefOrGoal) {

        gl.glLineWidth(2.0f);
        gl.glBegin(GL2.GL_LINE_STRIP);

        wave.forEach((freq, conf, start, end, qua, dur) -> {

            boolean eternal = (start != start);
            float x;
            float pw = baseTaskSize;// + gew / (1f / conf) / 4f;//10 + 10 * conf;
            float ph = baseTaskSize;// + geh / (1f / conf) / 4f;//10 + 10 * conf;

            if (eternal) {
                x = nowX; //???
            } else if ((start >= minT) && (start <= maxT)) {
                x = xTime(minT, maxT, start);
            } else {
                return;
            }

            float a = 0.25f + 0.75f * conf;
            if (beliefOrGoal) {
                gl.glColor4f(0.75f, 0.25f, 0f, a);
            } else {
                gl.glColor4f(0f, 0.75f, 0.25f, a);
            }

            //r.renderTask(gl, qua, conf, pw, ph, x, freq);
            gl.glVertex3f(x, freq, dz);


            if (start == end)
                return; //just the one point

            if (eternal) {
                //x = nowX; //??
                return;
            } else if ((end >= minT) && (end <= maxT)) {
                x = xTime(minT, maxT, end);
            }

            gl.glVertex3f(x, freq, dz);
        });

        gl.glEnd();
    }

//    private static float yPos(float f, float eh /* drawn object height, padding */) {
//        return (eh) * (f);
//    }

    /*private static float eternalX(float width, float b, float w, float cc) {
        return b + (width - b - w) * cc;
    }*/

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



    private static float xTime(float minT, float maxT, float o) {
        if (minT == maxT) return 0.5f;
        return (Math.min(maxT, Math.max(minT,o)) - minT) / (maxT - minT);
    }


}
