package nars.guifx.concept;

import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import nars.$;
import nars.guifx.NARfx;
import nars.task.MutableTask;
import nars.task.Task;
import nars.task.TruthPolation;
import nars.truth.Truth;

import java.util.ArrayList;
import java.util.List;

import static javafx.application.Platform.runLater;

/**
 * Created by me on 5/9/16.
 */
public class TruthPolationTest extends StackPane {

    public static final int width = 800;
    public static final int height = 400;
    private final Canvas editor;
    private final Canvas polation;
    private final TruthPolation truth;
    final int range = 64;
    private List<Task> tasks;

    public TruthPolationTest() {
        super();


        this.truth = new TruthPolation(range, 0);

        tasks = new ArrayList(range);
        tasks.add(t(1.0f, 0.8f, 5));
        tasks.add(t(0.3f, 0.5f, 15));
        tasks.add(t(0.3f, 0.5f, 45));
        tasks.add(t(0.6f, 0.3f, 50));


        polation = new Canvas(width, height);
        //polation.autosize();

        editor = new Canvas(width, height);
        editor.setOnMouseClicked(e-> {
            //int i =
            updateLater();
        });

        getChildren().addAll(
            polation, editor
        );

        updateLater();
    }

    public void updateLater() {
        runLater(this::update);
    }


    public static MutableTask t(float freq, float conf, long occ) {
        return new MutableTask("a:b", '.', $.t(freq, conf)).time(0, occ);
    }

    public void update() {
        //polation.resize(getWidth(), getHeight());
        GraphicsContext g = polation.getGraphicsContext2D();
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, g.getCanvas().getWidth(), g.getCanvas().getHeight());

        double w = getWidth() * 0.9;
        double h = getHeight() * 0.9;
        double dx = w / range;

        //GraphicsContext f = editor.getGraphicsContext2D();


        int rad = 20;


        for (int i = 0; i < range; i++) {
            Truth tc = truth.truth(i, tasks);
            double x = dx * i;
            double y = (1f-tc.freq()) * h;
            double r = 5 + tc.conf() * rad;

            x-= r/2; y-= r/2;

            g.setFill(Color.hsb(tc.conf() * 100, 0.6f, 0.9).interpolate(Color.TRANSPARENT, 0.25f));
            //g.strokeLine(x, y, x + dx, y);

            g.fillOval(x, y, r, r);
        }

        for (Task tc : tasks) {
            double x = dx *  tc.occurrence();
            double y = (1f-tc.freq()) * h;
            double r = 5 + tc.conf() * rad;
            x-= r/2; y-= r/2;
            g.setFill(Color.hsb(tc.conf() * 100 + 200, 0.8f, 0.9));
            g.fillOval(x, y, r, r);
        }

    }

    public static void main(String[] args) {
        NARfx.run((a,b)->{
            Stage st = new Stage();
            st.setScene(new Scene(new TruthPolationTest()));
            st.show();
            //a.start(st);
        });
    }
}
