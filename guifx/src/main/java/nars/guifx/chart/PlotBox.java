package nars.guifx.chart;

import javafx.scene.Node;
import javafx.scene.layout.TilePane;

/**
 * Created by me on 10/11/15.
 */
public class PlotBox extends TilePane {

    public PlotBox(Plot2D... plots) {
        super((Node[])plots);

        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        /*for (int i = 0; i < plots.length; i++) {

        }*/
    }

    public void update() {
        for (Node n : getChildren())
            ((Plot2D) n).update();
    }
}
