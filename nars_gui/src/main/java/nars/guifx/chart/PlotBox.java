package nars.guifx.chart;

import javafx.scene.Node;
import javafx.scene.layout.VBox;

/**
 * Created by me on 10/11/15.
 */
public class PlotBox extends VBox {

    public PlotBox(Plot2D... plots) {
        super(plots);

        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        /*for (int i = 0; i < plots.length; i++) {

        }*/
    }

    public void update() {
        for (Node n : getChildren())
            ((Plot2D) n).update();
    }
}
