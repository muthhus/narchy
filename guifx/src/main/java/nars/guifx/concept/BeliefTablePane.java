package nars.guifx.concept;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import nars.NAR;
import nars.concept.Concept;
import nars.guifx.util.ColorMatrix;
import nars.term.Termed;
import nars.truth.TruthWave;

import java.util.concurrent.atomic.AtomicBoolean;

import static javafx.application.Platform.runLater;
import static nars.nal.Tense.ETERNAL;

/**
 * Created by me on 3/18/16.
 */
public class BeliefTablePane extends HBox implements Runnable {
    final Canvas eternal, temporal;
    final TruthWave beliefs = new TruthWave(0), goals = new TruthWave(0);

    final static ColorMatrix beliefColors = new ColorMatrix(8, 8, (f, c) ->
            new Color(0.6f + 0.38f * c, 0.2f, 1f, 0.15f + 0.8f * c)
    );
    final static ColorMatrix goalColors = new ColorMatrix(8, 8, (f, c) ->
            new Color(0.2f + 0.4f * c, 1f, 0f, 0.15f + 0.8f * c)
    );

    //horizontal block
    final static TaskRenderer beliefRenderer = (ge, f, c, w, h, x, y) -> {
        ge.setFill(beliefColors.get(f, c));
        ge.fillRect(x - w / 2, y - h / 4, w, h / 2);
    };
    //vertical block
    final static TaskRenderer goalRenderer = (ge, f, c, w, h, x, y) -> {
        ge.setFill(goalColors.get(f, c));
        ge.fillRect(x - w / 4, y - h / 2, w / 2, h);
    };

    private final NAR nar;
    private long now;

    public BeliefTablePane(NAR nar) {
        super();

        this.nar = nar;

        getChildren().addAll(
                eternal = new Canvas(75, 75),
                temporal = new Canvas(200, 75)
        );

        //setCenter(temporal);
        //setLeft(eternal);
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

        GraphicsContext te = temporal.getGraphicsContext2D();
        float tew = (float) te.getCanvas().getWidth();
        float teh = (float) te.getCanvas().getHeight();
        te.clearRect(0, 0, tew, teh);

        //compute bounds from combined min/max of beliefs and goals so they align correctly
        long minT = Long.MAX_VALUE;
        long maxT = Long.MIN_VALUE;
        {

            if (!beliefs.isEmpty()) {
                minT = beliefs.start();
                maxT = beliefs.end();
            }
            if (!goals.isEmpty()) {

                long min = goals.start();
                if (min != ETERNAL) {
                    minT = Math.min(min, minT);
                    maxT = Math.max(goals.end(), maxT);
                }

            }

        }


        try {
            if (!beliefs.isEmpty())
                renderTable(minT, maxT, now, ge, gew, geh, te, tew, teh, beliefs, beliefRenderer);
            if (!goals.isEmpty())
                renderTable(minT, maxT, now, ge, gew, geh, te, tew, teh, goals, goalRenderer);
        } catch (Throwable t) {
            //HACK
            t.printStackTrace();
        }


        //borders
        {
            ge.setStroke(Color.WHITE);
            te.setStroke(Color.WHITE);
            ge.strokeRect(0, 0, gew, geh);
            te.strokeRect(0, 0, tew, teh);
            ge.setStroke(null);
            te.setStroke(null);
        }


    }

    final AtomicBoolean redraw = new AtomicBoolean(true);

    public void update(Concept concept) {

        if (concept == null) return;

        this.now = nar.time();
        beliefs.set(concept.beliefs());
        goals.set(concept.goals());


        if (redraw.compareAndSet(true, false)) {
            runLater(this);
        }
    }

    private void renderTable(long minT, long maxT, long now, GraphicsContext ge, float gew, float geh, GraphicsContext te, float tew, float teh, TruthWave table, TaskRenderer r) {
        float b = 4; //border

        //Present axis line
        if ((now <= maxT) && (now >= minT)) {
            float nowLineWidth = 3;
            float nx = xTime(tew, b, minT, maxT, now, nowLineWidth);
            te.setFill(Color.WHITE);
            te.fillRect(nx - nowLineWidth / 2f, 0, nowLineWidth, teh);
        }

        float w = 10;
        float h = 10;
        table.forEach((f, cc, o) -> {

            boolean eternal = !Float.isFinite(o);
            float eh, x;
            GraphicsContext g;
            if (eternal) {
                eh = geh;
                x = b + (gew - b - w) * cc;
                g = ge;
            } else {
                eh = teh;
                x = xTime(tew, b, minT, maxT, o, w);
                g = te;
            }
            float y = b + (eh - b - h) * (1 - f);
            r.renderTask(g, f, cc, w, h, x, y);

        });
    }

    @FunctionalInterface
    interface TaskRenderer {
        void renderTask(GraphicsContext ge, float f, float c, float w, float h, float x, float y);
    }


    private float xTime(float tew, float b, float minT, float maxT, float o, float w) {
        float p = minT == maxT ? 0.5f : (o - minT) / (maxT - minT);
        return b + p * (tew - b - w);
    }
}
