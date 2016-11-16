package nars.guifx.concept;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import nars.$;
import nars.Task;
import nars.task.MutableTask;
import nars.task.TruthPolation;
import nars.truth.Truth;
import nars.util.FX;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static javafx.application.Platform.runLater;

/**
 * Created by me on 5/9/16.
 */
public class TruthPolationLab extends VBox implements ChangeListener {

    public static final int width = 1000;
    public static final int height = 400;
    private final Canvas polation;
    private final TruthPolation truth;
    final int range = 32;

    private final List<Task> tasks;
    final ArrayList<Slider> freqSliders;
    final ArrayList<Slider> confSliders;
    private final Slider durationSlider;

    public TruthPolationLab() {
        super();


        freqSliders = new ArrayList<>(range);
        confSliders = new ArrayList<>(range);

        this.truth = new TruthPolation();

        tasks = new ArrayList(range);


        polation = new Canvas(width, height);
        //polation.autosize();


        durationSlider= new Slider(0.1f, 4f, 1f);
        durationSlider.valueProperty().addListener(this);

        HBox controls = new HBox(new Label("Duration", durationSlider));



        VBox v = new VBox(polation, newSliderBank(), controls);
        getChildren().add(v);

        System.out.println(freqSliders.size() + " " + confSliders.size());

        updateLater();
    }

    @NotNull
    public VBox newSliderBank() {
        final HBox confEdit;

        final HBox freqEdit = new HBox();
        confEdit = new HBox();


        for (int i = 0; i < range; i++) {
            Slider f = new Slider(0, 1, 0);

            f.setOrientation(Orientation.VERTICAL);
            freqSliders.add(f);
            freqEdit.getChildren().add(f);

            f.valueProperty().addListener(this);

            Slider c = new Slider(0, 1, 0);

            c.setOrientation(Orientation.VERTICAL);
            confSliders.add(c);
            confEdit.getChildren().add(c);
            c.valueProperty().addListener(this);

            InvalidationListener updateOpac = v -> c.setOpacity(c.getValue() * 0.5 + 0.5);
            c.valueProperty().addListener(updateOpac);

            updateOpac.invalidated(null);

        }

        return new VBox(freqEdit, confEdit);
    }

    public void updateLater() {
        runLater(this::update);
    }


    public static MutableTask t(float freq, float conf, long occ) {
        return new MutableTask("a:b", '.', $.t(freq, conf)).time(0, occ);
    }

    public void update() {

        tasks.clear();
        int active = 0;
        for (int i = 0; i < freqSliders.size(); i++) {

            double c = confSliders.get(i).getValue();
            if (c == 0) continue;

            double f = freqSliders.get(i).getValue();

            tasks.add(t((float)f, (float)c, i));
            active++;
        }

        //polation.resize(getWidth(), getHeight());
        GraphicsContext g = polation.getGraphicsContext2D();
        g.setFill(Color.BLACK);
        double cw = g.getCanvas().getWidth();
        double ch = g.getCanvas().getHeight();
        g.fillRect(0, 0, cw, ch);

        double w = cw * 0.9;

        //GraphicsContext f = editor.getGraphicsContext2D();


        int rad = 20;

        if (active > 0) {
            double my = rad * 2;
            double dx = w / range;
            double h = ch * 0.9;

            float dur = (float) durationSlider.getValue();

            for (int i = 0; i < range; i++) {
                Truth tc = truth.truth(i, tasks  /* TODO add eternal background control widget */ );
                if (tc!=null) {
                    double x = dx * i;
                    double y = (1f - tc.freq()) * h;
                    double r = 5 + tc.confWeight() * rad;

                    x -= r / 2;
                    y -= r / 2;
                    y += my;

                    g.setFill(Color.hsb(tc.conf() * 100, 0.6f, 0.9).interpolate(Color.TRANSPARENT, 0.25f));
                    //g.strokeLine(x, y, x + dx, y);

                    g.fillOval(x, y, r, r);
                }
            }

            for (Task tc : tasks) {
                double x = dx * tc.occurrence();
                double y = (1f - tc.freq()) * h;
                double r = 5 + tc.conf() * rad;
                x -= r / 2;
                y -= r / 2;
                y += my;

                g.setFill(Color.hsb(tc.conf() * 100 + 200, 0.8f, 0.9));
                g.fillOval(x, y, r, r);
            }
        }

    }

    public static void main(String[] args) {
        FX.run((a, b) -> {
            Stage st = new Stage();
            st.setScene(new Scene(new TruthPolationLab()));
            st.show();
            //a.start(st);
        });
    }

    @Override
    public void changed(ObservableValue observableValue, Object o, Object t1) {
        update();
    }
}
