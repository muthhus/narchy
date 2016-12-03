package spacegraph.obj.widget;

import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.math.v2;
import spacegraph.obj.layout.Layout;
import spacegraph.obj.layout.VSplit;

import static spacegraph.obj.layout.Grid.row;

/**
 * draggable panel
 */
public class Windo extends Widget {

    public Windo(String title, Surface content) {
        super();

        Surface menubar = //row(new PushButton("x"),
                new Label(title)
        ;

        setChildren(new VSplit(menubar, content, 0.1f).scale(0.9f,0.9f));
    }

    v2 dragStart = null;

    @Override
    protected boolean onTouching(v2 hitPoint, short[] buttons) {

        boolean lb = leftButton(buttons);
        if (hitPoint!=null && dragStart == null && lb) {
            //drag start
            dragStart = new v2(hitPoint);
        } else if ((hitPoint == null || !lb) && dragStart!=null) {
            //drag release
            dragStart = null;
        }

        if (hitPoint!=null && dragStart!=null) {
            float dx =  2 * ( hitPoint.x - dragStart.x );
            float dy =  2 * ( hitPoint.y - dragStart.y );
            move(dx, dy);
            return true;
        }


        return super.onTouching(hitPoint, buttons);
    }

    public static void main(String[] args) {
        SpaceGraph.window(
            //new Layout(
                //new Windo("x", Widget.widgetDemo()).scale(0.75f, 0.5f).move(-0.5f, -0.5f),
                new Windo("xy", Widget.widgetDemo())//.move(0.5f,0.5f)
           , 800, 800
        );
    }

}
