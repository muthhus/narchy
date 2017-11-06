package spacegraph.widget;

import com.jogamp.newt.event.MouseEvent;
import com.jogamp.opengl.GL2;
import spacegraph.Scale;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.ZoomOrtho;
import spacegraph.input.Finger;
import spacegraph.layout.Stacking;
import spacegraph.math.v2;
import spacegraph.render.Draw;
import spacegraph.widget.button.PushButton;

import java.util.Arrays;

/**
 * draggable panel
 */
public class Windo extends Stacking {

    public enum WindowDragMode {
        MOVE,
        RESIZE_NW,
        RESIZE_SW,
        RESIZE_NE,
        RESIZE_SE
    }

    public WindowDragMode dragMode = null;
    public WindowDragMode potentialDragMode = null;

    public final float resizeBorder = 0.05f;

    private boolean hover;
    float pmx, pmy;

    public Windo() {
        super();

//        Surface menubar = //row(new PushButton("x"),
//                new Label(title)
//        ;
//
//        PushButton upButton = new PushButton("^");
//        upButton.scale(0.25f,0.25f).move(0.5f,0.75f);
//
//        PushButton leftButton = new PushButton("<", (p) -> Windo.this.move(-1f, 0f));
//        leftButton.scale(0.25f,0.25f).move(0f, 0.5f);
//
//        PushButton rightButton = new PushButton(">", (p) -> Windo.this.move(1f, 0));
//        rightButton.scale(0.25f,0.25f).move(0.75f, 0.5f);
//
//        set(
//                upButton, leftButton, rightButton,
//                new VSplit(menubar, content, 0.1f)
//                );
    }


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

    /**
     * anchor region for Windo's to populate
     */
    public static class Desktop extends Stacking {

        public Desktop() {

            clipTouchBounds = false;

        }

        public Windo newWindo() {
            Windo w = new Windo();
            children.add(w);
            return w;
        }


    }

    @Override
    public Surface onTouch(Finger finger, v2 hitPoint, short[] buttons) {
        Surface s = super.onTouch(finger, hitPoint, buttons);
        if (s == this) {
            if (hitPoint != null) {
                System.out.println(hitPoint + " " + Arrays.toString(buttons));
                pmx = hitPoint.x;
                pmy = hitPoint.y;
                hover = true;
                return this;
            }
        }
        hover = false;
        return s;
    }

    public boolean opaque() {
        return true;
    }

//    @Override
//    protected void paint(GL2 gl) {
//        super.paint(gl);
//        if (hover && opaque()) {
//            gl.glColor3f(0.25f, 0.25f, 0.25f);
//            Draw.rect(gl, x(), y(), w(), h());
//        }
//    }

    protected void prepaint(GL2 gl) {

    }

    protected void postpaint(GL2 gl) {

    }

    @Override
    protected void paint(GL2 gl) {
//            {
//                //world coordinates alignment and scaling indicator
//                gl.glLineWidth(2);
//                gl.glColor3f(0.5f, 0.5f, 0.5f);
//                float cx = wmx;
//                float cy = wmy;
//                Draw.rectStroke(gl, cx + -100, cy + -100, 200, 200);
//                Draw.rectStroke(gl, cx + -200, cy + -200, 400, 400);
//                Draw.rectStroke(gl, cx + -300, cy + -300, 600, 600);
//            }

        super.paint(gl);

        prepaint(gl);

        float W = w(); //window.getWidth();
        float H = h(); //window.getHeight();

        gl.glColor4f(0.8f, 0.6f, 0f, 0.25f);

        int borderThick = 8;
        gl.glLineWidth(borderThick);
        Draw.line(gl, 0, 0, W, 0);
        Draw.line(gl, 0, 0, 0, H);
        Draw.line(gl, W, 0, W, H);
        Draw.line(gl, 0, H, W, H);

        float resizeBorder = Math.max(W,H)*this.resizeBorder;

        WindowDragMode p;
        if ((p = potentialDragMode) != null) {
            switch (p) {
                case RESIZE_SE:
                    gl.glColor4f(1f, 0.8f, 0f, 0.5f);
                    Draw.quad2d(gl, pmx, pmy, W, resizeBorder, W, 0, W - resizeBorder, 0);
                    break;
                case RESIZE_SW:
                    gl.glColor4f(1f, 0.8f, 0f, 0.5f);
                    Draw.quad2d(gl, pmx, pmy, 0, resizeBorder, 0, 0, resizeBorder, 0);
                    break;
            }
        }





//            gl.glLineWidth(2);
//            gl.glColor3f(0.8f, 0.5f, 0);
//            Draw.text(gl, str(notifications.top().get()), 32, smx + cw, smy + ch, 0);
//            gl.glColor3f(0.4f, 0f, 0.8f);
//            Draw.text(gl, wmx + "," + wmy, 32, smx - cw, smy - ch, 0);

        postpaint(gl);


    }



    @Override
    public boolean tangible() {
        return true;
    }

    //    @Override
//    protected void paintComponent(GL2 gl) {
//        gl.glColor3f(0.25f,0.25f,0.25f);
//        Draw.rect(gl, x(), y(), w(), h());
//    }

    public static void main(String[] args) {
        Desktop d;
        SpaceGraph.window(
                //new Layout(
                //new Windo("x", Widget.widgetDemo()).scale(0.75f, 0.5f).move(-0.5f, -0.5f),
                d = new Desktop(
//                    new Windo("xy", Widget.widgetDemo()),//.move(0.5f,0.5f),
//                    (Windo)(new Windo("xy", Widget.widgetDemo()).move(0.5f,0.5f).scale(0.25f,0.25f))
                )
                , 800, 800
        );
        Windo w = d.newWindo();
        w.set(new Scale(new PushButton("x"), 0.9f));
    }

}
