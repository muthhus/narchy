package spacegraph.widget;

import com.jogamp.opengl.GL2;
import jcog.Texts;
import jcog.Util;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.input.Finger;
import spacegraph.layout.Stacking;
import spacegraph.render.Draw;
import spacegraph.widget.button.CheckBox;
import spacegraph.widget.button.PushButton;
import spacegraph.widget.console.ConsoleTerminal;
import spacegraph.widget.slider.BaseSlider;
import spacegraph.widget.slider.XYSlider;

import static spacegraph.layout.Grid.*;

/**
 * Base class for GUI widgets, similarly designed to JComponent
 */
public class Widget extends Stacking {

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

    public Widget() {

    }

    public Widget(Surface... child) {
        set(child);
    }

    @Override
    protected void paint(GL2 gl) {

        super.paint(gl);

//        /*if (Param.DEBUG)*/ {
//            Draw.colorHash(gl, hashCode(), 0.5f);
//            String s = "g:" + scaleGlobal;
//            Draw.text(gl, s, 0.025f, 0.5f, 0, 0);
//            String s2 = "l:" + scaleLocal;
//            Draw.text(gl, s2, 0.025f, 0.5f, 1f, 0);
//        }

        //rainbow backgrounds
        //Draw.colorHash(gl, this.hashCode(), 0.8f, 0.2f, 0.25f);
        //Draw.rect(gl, 0, 0, 1, 1);


        if (touchedBy != null) {
            Draw.colorHash(gl, getClass().hashCode(), 0.5f);
            //gl.glColor3f(1f, 1f, 0f);
            gl.glLineWidth(4);
            Draw.rectStroke(gl, 0, 0, 1, 1);
        }

        paintComponent(gl);

    }

    protected void paintComponent(GL2 gl) {

    }



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
        if (finger == null) {
            onTouch(null,null);
        }
    }


    public static void main(String[] args) {


        SpaceGraph.window(widgetDemo(), 800, 600);

        //SpaceGraph dd = SpaceGraph.window(new Cuboid(widgetDemo(), 16, 8f).color(0.5f, 0.5f, 0.5f, 0.25f), 1000, 1000);

//        new SpaceGraph2D(
//                new Cuboid(widgetDemo(), 16, 8f, 0.2f).color(0.5f, 0.5f, 0.5f, 0.25f).move(0,0,0)
//        ).show(800, 600);

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
                                row(new PushButton("x"), new PushButton("xyz")),
                                col(new ScaleDebugLabel(), new PushButton("sdjfjsdfk"))
                        ),
                        new PushButton("clickMe()", (p) -> {
                            p.setText(Texts.n2(Math.random()));
                    }),

                    new XYSlider(),
                    new DummyConsole()
            );
    }

    private static class  DummyConsole extends ConsoleTerminal implements Runnable {

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
