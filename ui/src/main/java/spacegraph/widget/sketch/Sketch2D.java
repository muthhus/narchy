package spacegraph.widget.sketch;

import spacegraph.SpaceGraph;
import spacegraph.widget.windo.Widget;

import static java.lang.Math.abs;
import static java.lang.Math.min;

/**
 * gesture-aware general-purpose 2d graphical input widgets
 */
abstract public class Sketch2D extends Widget {

    //final RectFloat2D view = new RectFloat2D(0,0,1,1);

    public static void main(String[] args) {
        SpaceGraph.window(new Sketch2DBitmap(256, 256), 800, 800);
    }
}
