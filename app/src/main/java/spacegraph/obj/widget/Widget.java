package spacegraph.obj.widget;

import com.jogamp.opengl.GL2;
import nars.util.Texts;
import nars.util.Util;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.input.Finger;
import spacegraph.obj.Cuboid;
import spacegraph.obj.layout.Stacking;
import spacegraph.obj.widget.console.VirtualConsole;
import spacegraph.render.Draw;
import spacegraph.render.SpaceGraph2D;

import static spacegraph.obj.layout.Grid.*;

/**
 * Base class for GUI widgets, similarly designed to JComponent
 */
public abstract class Widget extends Stacking {

    @Nullable Finger touchedBy;


//MARGIN
//    @Override
//    public void setParent(Surface s) {
//        super.setParent(s);
//
//        float proportion = 0.9f;
//        float margin = 0.0f;
//        //float content = 1f - margin;
//        float x = margin / 2f;
//
//        Surface content = content();
//        content.scaleLocal.set(proportion, proportion);
//        content.translateLocal.set(x, 1f - proportion, 0);
//
//    }


    @Override
    protected void paint(GL2 gl) {

        if (touchedBy != null) {
            gl.glColor3f(1f, 1f, 0f);
            gl.glLineWidth(4);
            Draw.rectStroke(gl, 0, 0, 1, 1);
        }

        paintComponent(gl);

    }

    protected abstract void paintComponent(GL2 gl);


//    @Override
//    protected boolean onTouching(v2 hitPoint, short[] buttons) {
////        int leftTransition = buttons[0] - (touchButtons[0] ? 1 : 0);
////
////        if (leftTransition == 0) {
////            //no state change, just hovering
////        } else {
////            if (leftTransition > 0) {
////                //clicked
////            } else if (leftTransition < 0) {
////                //released
////            }
////        }
//
//
//        return false;
//    }


    public void touch(@Nullable Finger finger) {
        touchedBy = finger;
    }


    public static void main(String[] args) {


        //SpaceGraph.window(widgetDemo(), 800, 600);

        //SpaceGraph dd = SpaceGraph.window(new Cuboid(widgetDemo(), 16, 8f).color(0.5f, 0.5f, 0.5f, 0.25f), 1000, 1000);

        new SpaceGraph2D(new Cuboid(widgetDemo(), 16, 8f, 0.1f).color(0.5f, 0.5f, 0.5f, 0.25f).move(0,0,0)).show(800, 600);

//        SpaceGraph.window(col(
//                new Slider(0.5f, 0, 1).on((s,v)->{
//                    ortho1.scale(0.25f + 2f * v);
//                    //dd.zNear = 0.1f + 2f * v;
//                    //System.out.println("zNear=" + dd.zNear);
//                }),
//                new Slider(0.5f, 0, 1).on((s,v)->{
//                    //dd.zFar = 10f + 200f * v;
//                    //System.out.println("zFar=" + dd.zFar);
//                })
//        ), 100, 200);

    }

    public static class ScaleDebugLabel extends Label {

        public ScaleDebugLabel() {
            super();
        }

        @Override
        public void paint(GL2 gl) {
            set(scaleLocal.toString());
            super.paint(gl);
        }
    }

    public static Surface widgetDemo() {
        return grid(
                    new BaseSlider(.25f  /* pause */),
                    grid(),
                    col(new CheckBox("ABC"),new CheckBox("XYZ")),
                        grid(new ScaleDebugLabel(), new ScaleDebugLabel(),
                                row(new ScaleDebugLabel(), new ScaleDebugLabel()),
                                col(new ScaleDebugLabel(), new ScaleDebugLabel())
                        ),
                        new PushButton("clickMe()", (p) -> {
                            p.setText(Texts.n2(Math.random()));
                    }),

                    new XYSlider(),
                    new DummyConsole()
            );
    }

    private static class DummyConsole extends VirtualConsole implements Runnable {

        public DummyConsole() {
            super(40, 20);
            new Thread(this).start();
        }

        @Override
        public void run() {

            while(true) {

                append((Math.random()) + " ");

                Util.sleep(200);
            }
        }
    }
}
