package nars.guifx.concept;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import nars.NAR;
import nars.concept.Concept;
import nars.guifx.util.ColorMatrix;
import nars.truth.Truth;
import nars.truth.TruthWave;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Math.PI;
import static javafx.application.Platform.runLater;
import static nars.time.Tense.ETERNAL;

/**
 * Created by me on 3/18/16.
 */
public class TaskTablePane extends HBox implements Runnable {
    final Canvas eternal, temporal;
    final TruthWave beliefs = new TruthWave(0), goals = new TruthWave(0);

    final static ColorMatrix beliefColors = new ColorMatrix(8, 8, (f, c) ->
            new Color(0.6f + 0.38f * c, 0.2f, 1f, 0.39f + 0.6f * c)
    );
    final static ColorMatrix goalColors = new ColorMatrix(8, 8, (f, c) ->
            new Color(0.2f + 0.4f * c, 1f, 0.2f, 0.39f + 0.6f * c)
    );

    //horizontal block
    final static TaskRenderer beliefRenderer = (ge, pri, c, w, h, x, y) -> {
        ge.setFill(beliefColors.get(c, pri));
        ge.fillRect(x - w / 2, y - h / 4, w, h / 2);
    };
    //vertical block
    final static TaskRenderer goalRenderer = (ge, pri, c, w, h, x, y) -> {
        ge.setFill(goalColors.get(c, pri));
        ge.fillRect(x - w / 4, y - h / 2, w / 2, h);
    };

    private final NAR nar;
    private long now;

    public TaskTablePane(NAR nar) {
        super();

        this.nar = nar;

        getChildren().addAll(
                eternal = new Canvas(100, 100),
                temporal = new Canvas(250, 100)
        );

    }

    /**
     * redraw
     */
    @Override
    public void run() {
        if (!redraw.compareAndSet(false, true)) {
            return;
        }

        //redraw
        GraphicsContext ge = eternal.getGraphicsContext2D();
        float gew = (float) ge.getCanvas().getWidth();
        float geh = (float) ge.getCanvas().getHeight();
        ge.clearRect(0, 0, gew, geh);

        GraphicsContext gt = temporal.getGraphicsContext2D();
        float tew = (float) gt.getCanvas().getWidth();
        float teh = (float) gt.getCanvas().getHeight();
        gt.clearRect(0, 0, tew, teh);

        //compute bounds from combined min/max of beliefs and goals so they align correctly
        long minT = Long.MAX_VALUE;
        long maxT = Long.MIN_VALUE;

        if (!beliefs.isEmpty()) {
            minT = beliefs.start();
            maxT = beliefs.end();
        }
        if (!goals.isEmpty()) {

            long min = goals.start();
            if (min != ETERNAL) {
                minT = Math.min(goals.start(), minT);
                maxT = Math.max(goals.end(), maxT);
            }

        }


        try {
            if (!beliefs.isEmpty())
                renderTable(minT, maxT, now, ge, gew, geh, gt, tew, teh, beliefs, beliefRenderer);
            if (!goals.isEmpty())
                renderTable(minT, maxT, now, ge, gew, geh, gt, tew, teh, goals, goalRenderer);
        } catch (Throwable t) {
            //HACK
            t.printStackTrace();
        }

        //render current belief and goal states as crosshairs on eternal grid
        Truth bc = beliefs.current;
        long t = nar.time();

        float angleSpeed = 0.5f;
        if (bc!=null) {
            beliefTheta += bc.motivation() * angleSpeed;
            drawCrossHair(ge, gew, geh, bc, Color.ORANGERED, t, beliefTheta);
        }

        Truth gc = goals.current;
        if (gc!=null) {
            goalTheta += gc.motivation() * angleSpeed;
            drawCrossHair(ge, gew, geh, gc, Color.LIMEGREEN, t, goalTheta);
        }


        //borders
        ge.setStroke(Color.GRAY);
        gt.setStroke(Color.GRAY);
        ge.setLineWidth(1);
        ge.strokeRect(0, 0, gew, geh);
        gt.strokeRect(0, 0, tew, teh);
        ge.setStroke(null);
        gt.setStroke(null);


    }

    double beliefTheta, goalTheta = PI/2;

    public void drawCrossHair(GraphicsContext ge, float gew, float geh, Truth truth, Color c, long t, double theta) {
        float w = 4;
        ge.setLineWidth(w);

        float conf = truth.conf();
        c = new Color(c.getRed(), c.getGreen(), c.getBlue(),
            conf * 0.79f + 0.2f);

        float bcx = eternalX(gew, padding, w, conf);
        float bcy = yPos(geh, padding, w, truth.freq());

        ge.setStroke(c);
        //ge.strokeLine(bcx, border, bcx, geh - border);
        //ge.strokeLine(border, bcy, gew - border, bcy);
        double r = gew * (0.1 + (0.15 * conf));

        double dx0 = Math.cos(theta) * r;
        double dy0 = Math.sin(theta) * r;
        ge.strokeLine(dx0+bcx, dy0+bcy, -dx0+bcx, -dy0+bcy);
        double hpi = PI / 2.0;
        double dx1 = Math.cos(theta +hpi) * r;
        double dy1 = Math.sin(theta +hpi) * r;
        ge.strokeLine(dx1+bcx, dy1+bcy, -dx1+bcx, -dy1+bcy);
    }

    final float padding = 4;

    final AtomicBoolean redraw = new AtomicBoolean(true);

    public void update(Concept concept) {

        if (concept == null) return;

        this.now = nar.time();
        beliefs.set(concept.beliefs(), now);
        goals.set(concept.goals(), now);

        if (redraw.compareAndSet(true, false)) {
            runLater(this);
        }
    }

    private void renderTable(long minT, long maxT, long now, GraphicsContext ge, float gew, float geh, GraphicsContext te, float tew, float teh, TruthWave table, TaskRenderer r) {


        //Present axis line
        if ((now <= maxT) && (now >= minT)) {
            float nowLineWidth = 3;
            float nx = xTime(tew, padding, minT, maxT, now, nowLineWidth);
            te.setFill(Color.WHITE);
            te.fillRect(nx - nowLineWidth / 2f, 0, nowLineWidth, teh);
        }

        /** drawn "pixel" dimensions*/

        table.forEach((freq, conf, o, pri) -> {

            boolean eternal = !Float.isFinite(o);
            float eh, x;
            GraphicsContext g;
            float padding = this.padding;
            float pw = 10;
            if (eternal) {
                eh = geh;

                x = eternalX(gew, padding, pw, conf);
                g = ge;
            } else {
                eh = teh;
                x = xTime(tew, padding, minT, maxT, o, pw);
                g = te;
            }
            float ph = 10;
            float y = yPos(eh, padding, ph, freq
            );
            r.renderTask(g, pri, conf, pw, ph, x, y);

        });

    }

    private static float yPos(float eh, float b, float h, float f) {
        return b + (eh - b - h) * (1 - f);
    }

    private static float eternalX(float width, float b, float w, float cc) {
        return b + (width - b - w) * cc;
    }

    @FunctionalInterface
    interface TaskRenderer {
        void renderTask(GraphicsContext ge, float pri, float c, float w, float h, float x, float y);
    }


    private static float xTime(float tew, float b, float minT, float maxT, float o, float w) {
        float p = minT == maxT ? 0.5f : (o - minT) / (maxT - minT);
        return b + p * (tew - b - w);
    }
}
