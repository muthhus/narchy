package spacegraph.widget;

import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.layout.Layout;
import spacegraph.layout.VSplit;
import spacegraph.math.v2;
import spacegraph.widget.button.PushButton;

/**
 * draggable panel
 */
public class Windo extends Widget {

    public Windo(String title, Surface content) {
        super();

        Surface menubar = //row(new PushButton("x"),
                new Label(title)
        ;

        PushButton upButton = new PushButton("^");
        upButton.scale(0.25f,0.25f).move(0.5f,0.75f);

        PushButton leftButton = new PushButton("<", (p) -> Windo.this.move(-1f, 0f));
        leftButton.scale(0.25f,0.25f).move(0f, 0.5f);

        PushButton rightButton = new PushButton(">", (p) -> Windo.this.move(1f, 0));
        rightButton.scale(0.25f,0.25f).move(0.75f, 0.5f);

        setChildren(
                upButton, leftButton, rightButton,
                new VSplit(menubar, content, 0.1f)
                );
    }

    v2 dragStart;

//    @Override
//    protected boolean onTouching(v2 hitPoint, short[] buttons) {
//
//        if (super.onTouching(hitPoint, buttons)) {
//            return true;
//        }
//
//        boolean lb = leftButton(buttons);
//        if (hitPoint!=null && dragStart == null && lb) {
//            //drag start
//            dragStart = new v2(hitPoint);
//        } else if ((hitPoint == null || !lb) && dragStart!=null) {
//            //drag release
//            dragStart = null;
//        }
//
//        if (hitPoint!=null && dragStart!=null) {
//            //float dx =  2 * ( hitPoint.x - dragStart.x );
//            //float dy =  2 * ( hitPoint.y - dragStart.y );
//            //move(dx, dy);
//            //return true;
//        }
//
//
//        return false;
//    }

    /** anchor region for Windo's to populate */
    public static class Desktop extends Layout {

        public Desktop(Windo... initial) {

            clipTouchBounds = false;

            setChildren(initial);
        }

    }

    public static void main(String[] args) {
        SpaceGraph.window(
            //new Layout(
                //new Windo("x", Widget.widgetDemo()).scale(0.75f, 0.5f).move(-0.5f, -0.5f),
                new Desktop(
                    new Windo("xy", Widget.widgetDemo()),//.move(0.5f,0.5f),
                    (Windo)(new Windo("xy", Widget.widgetDemo()).move(0.5f,0.5f).scale(0.25f,0.25f))
                )
           , 800, 800
        );
    }

}
